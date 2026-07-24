plugins {
    alias(libs.plugins.kotlin.plugin.spring)
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies.bom))

    implementation(project(":domain"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation(libs.kotlin.reflect)

    testImplementation(testFixtures(project(":domain")))
}
