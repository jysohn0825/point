package com.jysohn0825.point.presentation.support

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.extensions.spring.SpringAutowireConstructorExtension
import io.kotest.extensions.spring.SpringExtension

/**
 * `@SpringBootTest` 기반 `BehaviorSpec`이 생성자로 Spring 빈을 주입받을 수 있도록
 * kotest-extensions-spring의 확장을 등록한다.
 */
class ProjectConfig : AbstractProjectConfig() {
    override fun extensions(): List<Extension> = listOf(SpringExtension, SpringAutowireConstructorExtension)
}
