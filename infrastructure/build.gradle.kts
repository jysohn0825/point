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
    testImplementation("org.testcontainers:mysql:1.21.4")
}
