plugins {
    alias(libs.plugins.kotlin.plugin.spring)
    alias(libs.plugins.kotlin.plugin.jpa)
    alias(libs.plugins.kotlin.plugin.allopen)
}

allOpen {
    annotation("jakarta.persistence.Entity")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies.bom))

    implementation(project(":domain"))
    implementation(project(":support"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation(libs.redisson.spring.boot.starter)
    implementation(libs.kotlin.reflect)

    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation(testFixtures(project(":domain")))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:mysql:1.21.4")
    testImplementation("org.testcontainers:testcontainers:1.21.4")
}

// Colima/OrbStack 등 Docker Desktop이 아닌 환경에서는 docker.sock이 기본 경로(/var/run/docker.sock)에 없어
// Testcontainers가 데몬을 못 찾는다. DOCKER_HOST를 사람이 직접 셸에 export하는 대신, 현재 활성화된
// `docker context`를 빌드 시점에 조회해 테스트 JVM에 넘겨줌으로써 어떤 Docker 배포판을 쓰든 동일하게 동작하게 한다.
//
// 그리고 Colima처럼 daemon이 VM 안에서 도는 경우, 호스트에 보이는 소켓 경로(~/.colima/.../docker.sock)와
// daemon 입장에서의 실제 경로(/var/run/docker.sock)가 달라서, Testcontainers가 Ryuk 등 사이드카 컨테이너에
// 소켓을 마운트할 때 호스트 경로를 그대로 넘기면 실패한다. daemon 내부 기준 경로는 어떤 배포판이든 항상
// /var/run/docker.sock이므로 TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE로 고정해 준다.
tasks.named<Test>("test") {
    doFirst {
        fun runCommand(vararg command: String): String? =
            runCatching {
                val process: Process =
                    ProcessBuilder(*command)
                        .redirectErrorStream(false)
                        .start()
                val output: String =
                    process.inputStream
                        .bufferedReader()
                        .readText()
                        .trim()
                if (process.waitFor() == 0 && output.isNotBlank()) output else null
            }.getOrNull()

        if (System.getenv("DOCKER_HOST").isNullOrBlank()) {
            val dockerHost: String? =
                runCommand("docker", "context", "inspect", "--format", "{{.Endpoints.docker.Host}}")
            if (dockerHost != null) {
                environment("DOCKER_HOST", dockerHost)
            }
        }

        if (System.getenv("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE").isNullOrBlank()) {
            environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock")
        }
    }
}
