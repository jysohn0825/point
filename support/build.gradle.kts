plugins {
    alias(libs.plugins.kotlin.plugin.spring)
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies.bom))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation(libs.kotlin.reflect)
}
