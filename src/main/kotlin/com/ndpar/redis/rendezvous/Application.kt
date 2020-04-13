package com.ndpar.redis.rendezvous

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.ndpar"])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
