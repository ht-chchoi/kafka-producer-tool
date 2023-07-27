package com.ht.kafkaproducertool

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KafkaProducerToolApplication

fun main(args: Array<String>) {
    runApplication<KafkaProducerToolApplication>(*args)
}
