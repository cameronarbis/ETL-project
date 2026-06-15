package com.dataplatform.etl.config;

import com.dataplatform.etl.model.Transaction;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.context.annotation.Bean;
import org.springframework.beans.context.annotation.Configuration;
import org.springframework.beans.context.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/**
 * Kafka consumer configuration
 * configures how the spring kafka listener cotainer:
 * 1. connects to kafka (bootstrap servers, group ID)
 * 2. deserializes messages (JSON -> transaction objects)
 * 3. handles offsets (manual commit after DB insert succeeds)
 * 4. manages concurrency (how many threads process messages)
 */
@Configuration
@EnableKakfa
public class KafkaConsumerConfig {
    //inject from application.yml: spring.kafka.bootsrap-servers
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    //inject from application.yml: spring.kafka.consumer.group-id
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    //inject from application.yml: kafka.topic.transactions
    @Value("${kafka.topic.transactions}")
    private String transactionTopic;
}

/**
 * ConsumerFactory creates Kafka Consumer Instances.
 *
 * Configures:
 * - deserialization: how to convert Kafka bytes to Java objects
 * - consumer settings: bootstrap servers, group ID, auto-offset behavior
 * - security/retry policies
 */
@Bean
public ConsumerFactory<String, Transaction> consumerFactory() {
    // build a map of Kafka consumer properties
    java.util.Map<String, Object> props = new java.util.HashMap<>();

    // CONNECTION SETTINGS - where to find Kafka brokers
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

    // which consumer group this instance belongs to
    // multiple ETL instances with the same group ID share the work
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

    // DESERIALIZATION SETTINGS - key is always a String (transaction ID)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    // value is Transaction (JSON deserialized to Java object)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

    // tell JsonDeserializer which Java class to deserialize into
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.dataplatform.etl.model.Transaction");
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.dataplatform.etl.model");

    // OFFSET MANAGEMENT - if no saved offset exists, start from beginning (earliest message)
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    // DO NOT automatically commit offsets to Kafka
    // we manually commit after database insert succeeds
    // this prevents message loss if ETL crashes during insert
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

    // PERFORMANCE tuning - fetch up to 100 records per poll (batch size)
    // larger = fewer network requests, but more memory
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");

    // wait up to 30 seconds for records before returning empy
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 3000);

    // create and return the ConsumerFactory
    return new DefaultKafkaConsumerFactory<>(props);

    /**
     * ConcurrentKafkaListenerContainerFactory configures the listener container
     *
     * the listener container is the mechanism that:
     * 1. Continuously polls Kafka for messages
     * 2. calls for KafkaListener method for each message
     * 3. manages threading, offsets, and error handling
     */

    @Bean
    public ConcurrentKafkaListenerContainerFactor<String, Transaction>
            kafkaListenerContainerFactor(){
            // create a container factory with our consumer factory
            ConcurrentKafkaListenerContainerFactory<String, Transaction> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
            // set the consumer factory we defined above
            factory.setConsumerFactory(consumerFactory());

            // CONCURRENCY: number of threads processing messages in parallel
            // 3 threads = can process 3 messages simultaneously
            // higher = more throughput, but more resource usage
            factory.setConcurrency(3);

            // OFFSET MANAGEMENT: manual commit after listener succeeds
            // AckMode.MANUAL: only commit after our @KafkaListener method completes successfully
            // this ensures: if method throws an exception, offset is not committed,
            // and Kafka will resend the message on the next poll
            factory.getContainer.Properties().setAckMode(ContainerProperties.AckMode.MANUAL);
            return factory;
    }
}