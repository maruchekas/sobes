package ru.sobes

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SobesApplication

fun main(args: Array<String>) {
    runApplication<SobesApplication>(*args)
}
