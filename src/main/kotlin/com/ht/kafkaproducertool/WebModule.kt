package com.ht.kafkaproducertool

import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@Controller
class PageController(val kafkaService: KafkaService) {
    @GetMapping("/")
    fun index(@RequestParam message: String?, model: Model): String = model
        .addAttribute("message", message ?: "")
        .addAttribute("senders", this.kafkaService.getSenderList())
        .let { "index" }
}

@RestController
class KafkaRestController(val kafkaService: KafkaService) {
    @PostMapping("/add")
    fun add(@RequestBody body: Map<String, String>): ResponseEntity<Map<String, String>> {
        return try {
            this.kafkaService.addSender(body["senderName"]!!, body["endpoint"]!!)
            ResponseEntity(mapOf("result" to "success"), HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity(mapOf("result" to "fail", "message" to "fail to Connect, message: ${e.message}"), HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @PostMapping("/send")
    fun sendMessage(@RequestBody body: Map<String, String>): ResponseEntity<Map<String, String>> {
        if (!body.containsKey("name") || !body.containsKey("topic") || !body.containsKey("value")) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        return try {
            this.kafkaService.sendMessage(body["name"]!!, body["topic"]!!, body["key"], body["value"]!!)
            ResponseEntity(mapOf("result" to "success"), HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity(mapOf("result" to "fail", "message" to "${e.message}"), HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}

@Service
class KafkaService(val producerManager: ProducerManager) {
    fun getSenderList() = this.producerManager.getSenders()
        .map {
            mapOf(
                "name" to it.key,
                "endpoint" to it.value.producerProperties["bootstrap.servers"])
        }
        .toList()

    fun addSender(senderName: String, endpoint: String) {
        this.producerManager
            .addSender(senderName, this.producerManager
                .createProducerProperties(mapOf(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to endpoint)))
    }

    fun sendMessage(name: String, topic: String, key: String?, value: String) {
        this.producerManager.sendMessage(name, topic, key, value)
    }
}