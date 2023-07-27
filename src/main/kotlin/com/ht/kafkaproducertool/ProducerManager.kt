package com.ht.kafkaproducertool

import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpClientErrorException.BadRequest
import java.lang.RuntimeException
import java.util.Properties
import javax.annotation.PostConstruct

@Component
class ProducerManager {
    private val defaultProps = Properties()
    private val senders = HashMap<String, SenderHolder>()

    @PostConstruct
    fun init() {
        defaultProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        defaultProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
    }

    fun createProducerProperties(props: Map<String, Any>): Properties {
        val tempProps = defaultProps.clone() as Properties
        props.entries.forEach {
            tempProps[it.key] = it.value
        }
        return tempProps
    }

    fun addSender(name: String, producerProperties: Properties) {
        if (this.senders.containsKey(name)) {
            throw RuntimeException("Duplicated Producer Client name")
        }
        if (this.senders
                .map { it.value.producerProperties.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG) }
                .contains(producerProperties.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))) {
            throw RuntimeException("Duplicated Producer Client endpoint")
        }
        this.senders[name] = SenderHolder(
            producerProperties,
            KafkaProducer(producerProperties))
    }

    fun getSenders(): Map<String, SenderHolder> = this.senders

    fun sendMessage(name: String, topic: String, key: String?, value: String) {
        val producerRecord =
            if (key == null) ProducerRecord(topic, value)
            else ProducerRecord(topic, key, value)

        if (!senders.containsKey(name)) {
            throw RuntimeException("No Producer Found")
        }
        val recordMetadata = this.senders[name]?.kafkaProducer?.send(producerRecord)?.get()
    }
}

data class SenderHolder(
    val producerProperties: Properties,
    val kafkaProducer: KafkaProducer<String, String>
)