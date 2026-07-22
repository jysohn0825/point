plugins {
    alias(libs.plugins.kotlin.plugin.spring)
    alias(libs.plugins.kotlin.plugin.jpa)
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies.bom))

    implementation(project(":domain"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation(libs.kotlin.reflect)

    runtimeOnly("com.h2database:h2")

    testImplementation(testFixtures(project(":domain")))
}
