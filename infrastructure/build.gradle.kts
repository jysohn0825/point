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
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation(libs.kotlin.reflect)

    runtimeOnly("com.h2database:h2")

    testImplementation(testFixtures(project(":domain")))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
}
