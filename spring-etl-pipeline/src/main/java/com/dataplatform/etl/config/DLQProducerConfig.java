package com.dataplatform.etl.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the DLQ producer that publishes failed messages.
 * Uses a separate KafkaTemplate with explicit JSON serialization for Map values.
 */
@Configuration
public class DLQProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Producer factory configured for Map<String, Object> serialization to JSON.
     */
    @Bean
    public ProducerFactory<String, Map<String, Object>> dlqProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * KafkaTemplate for sending DLQ messages with JSON serialization.
     * This replaces the default template which uses StringSerializer.
     */
    @Bean
    public KafkaTemplate<String, Map<String, Object>> dlqKafkaTemplate() {
        return new KafkaTemplate<>(dlqProducerFactory());
    }
}
