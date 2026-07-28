import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint) apply false
}

allprojects {
    group = "com.jysohn0825"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

val kotestVersion = "5.9.1"

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "jacoco")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    configure<KotlinJvmProjectExtension> {
        compilerOptions {
            // SpEL(@DistributedLock 등)이 리플렉션으로 파라미터 이름을 안정적으로 읽을 수 있도록 한다.
            freeCompilerArgs.addAll("-Xjsr305=strict", "-java-parameters")
        }
    }

    dependencies {
        "testImplementation"("io.kotest:kotest-runner-junit5:$kotestVersion")
        "testImplementation"("io.kotest:kotest-assertions-core:$kotestVersion")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
        // 인터페이스 파라미터 기본값 때문에 생성되는 Kotlin 합성 클래스(런타임 로직이 없음)는 커버리지 대상에서 제외한다.
        classDirectories.setFrom(
            classDirectories.files.map { dir ->
                fileTree(dir) { exclude("**/*\$DefaultImpls.class") }
            },
        )
    }

    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn(tasks.named("jacocoTestReport"))
        classDirectories.setFrom(
            classDirectories.files.map { dir ->
                fileTree(dir) { exclude("**/*\$DefaultImpls.class") }
            },
        )
        violationRules {
            rule {
                limit {
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
    }

    tasks.named("check") {
        dependsOn(tasks.named("jacocoTestReport"), tasks.named("jacocoTestCoverageVerification"))
    }
}
