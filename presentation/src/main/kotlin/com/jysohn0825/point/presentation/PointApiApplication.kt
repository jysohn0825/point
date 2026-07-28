package com.jysohn0825.point.presentation

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.jysohn0825.point"])
class PointApiApplication

fun main(args: Array<String>) {
    runApplication<PointApiApplication>(*args)
}
