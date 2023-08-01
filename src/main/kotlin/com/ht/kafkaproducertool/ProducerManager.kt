package com.ht.kafkaproducertool

import mu.KotlinLogging
import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.stereotype.Component
import java.lang.RuntimeException
import java.util.Properties
import javax.annotation.PostConstruct

@Component
class ProducerManager {
    private val log = KotlinLogging.logger {  }
    private val defaultProps = Properties()
    private val senders = HashMap<String, SenderHolder>()

    @PostConstruct
    fun init() {
        log.info("init kafka Producer Manager, ver=1")

        defaultProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        defaultProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        defaultProps[ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG] = 31000
    }

    fun createProducerProperties(props: Map<String, Any>): Properties {
        val tempProps = defaultProps.clone() as Properties
        props.entries.forEach {
            tempProps[it.key] = it.value
        }
        return tempProps
    }

    fun addSender(name: String, producerProperties: Properties) {
        log.info("add producer client, name: {}, props: {}", name, producerProperties)
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
            KafkaProducer(producerProperties),
            Admin.create(producerProperties))
        log.info("add Producer Client success, name: {}", name)
    }

    fun getSenders(): Map<String, SenderHolder> = this.senders

    fun sendMessage(name: String, topic: String, key: String?, value: String): RecordMetadata? {
        val producerRecord =
            if (key == null) ProducerRecord(topic, value)
            else ProducerRecord(topic, key, value)

        if (!senders.containsKey(name)) {
            throw RuntimeException("No Producer Found")
        }
        val recordMetaData = this.senders[name]?.kafkaProducer?.send(producerRecord)?.get()
        log.info("send Message >> name: {}, topic: {}, key: {}, value: {}", name, topic, key, value)
        return recordMetaData
    }

    fun getTopics(name: String): MutableSet<String> {
        return this.senders[name]!!.kafkaAdmin.listTopics()!!.names()!!.get()
    }
}

data class SenderHolder(
    val producerProperties: Properties,
    val kafkaProducer: KafkaProducer<String, String>,
    val kafkaAdmin: Admin
)