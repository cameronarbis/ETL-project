package com.dataplatform.ingress.service;

import com.dataplatform.ingress.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String transactionsTopic;

    public KafkaProducerService(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topic.transactions}") String transactionsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.transactionsTopic = transactionsTopic;
    }

    public CompletableFuture<SendResult<String, Object>> publishTransaction(Transaction transaction) {
        log.debug("Publishing transaction {} to topic {}", 
                  transaction.getTransactionId(), transactionsTopic);

        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(transactionsTopic, transaction.getTransactionId(), transaction);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully published transaction {} to partition {} with offset {}",
                        transaction.getTransactionId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish transaction {}: {}",
                        transaction.getTransactionId(),
                        ex.getMessage(), ex);
            }
        });

        return future;
    }

    public SendResult<String, Object> publishTransactionSync(Transaction transaction) throws Exception {
        log.debug("Publishing transaction {} synchronously to topic {}",
                transaction.getTransactionId(), transactionsTopic);

        SendResult<String, Object> result = 
            kafkaTemplate.send(transactionsTopic, transaction.getTransactionId(), transaction).get();

        log.info("Synchronously published transaction {} to partition {} with offset {}",
                transaction.getTransactionId(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());

        return result;
    }
}
