package com.dataplatform.etl.config;

import com.dataplatform.etl.error.DLQHandler;
import com.dataplatform.etl.model.Transaction;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${kafka.topic.transactions}")
    private String transactionTopic;

    // retry configuration: drives the container-level DefaultErrorHandler below
    @Value("${kafka.retry.max-attempts}")
    private int maxAttempts;

    @Value("${kafka.retry.initial-backoff-ms}")
    private long initialBackoffMs;

    @Value("${kafka.retry.backoff-multiplier}")
    private double backoffMultiplier;

    @Bean
    public ConsumerFactory<String, Transaction> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.dataplatform.etl.model.Transaction");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.dataplatform.etl.model");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /*
     * Replaces the old hand-rolled Thread.sleep retry loop in
     * TransactionConsumerService. When the listener throws, the container
     * seeks the partition back to the failed record and redelivers it after
     * the configured backoff, up to (maxAttempts - 1) retries. Once retries
     * are exhausted, the recoverer runs instead of another redelivery — it
     * routes the record to the DLQ via the existing DLQHandler, preserving
     * the enriched DLQ payload shape. The container commits the offset for a
     * recovered record itself, so no manual Acknowledgment handling is
     * needed here.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DLQHandler dlqHandler) {
        ConsumerRecordRecoverer recoverer = (record, ex) -> {
            Object value = record.value();
            if (value instanceof Transaction transaction) {
                logger.error("Transaction {} failed after {} attempts. Sending to DLQ.",
                        transaction.getTransactionId(), maxAttempts, ex);
                dlqHandler.sendToDLQ(transaction, ex, maxAttempts);
            } else {
                logger.error("Record with key {} failed after {} attempts and is not a Transaction; cannot send to DLQ.",
                        record.key(), maxAttempts, ex);
            }
        };

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxAttempts - 1);
        backOff.setInitialInterval(initialBackoffMs);
        backOff.setMultiplier(backoffMultiplier);

        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Transaction> kafkaListenerContainerFactory(
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Transaction> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }
}
