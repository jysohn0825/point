plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(testFixtures(project(":domain")))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}
