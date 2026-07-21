plugins {
    `java-library`
    `java-test-fixtures`
    kotlin("jvm")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testFixturesImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}
