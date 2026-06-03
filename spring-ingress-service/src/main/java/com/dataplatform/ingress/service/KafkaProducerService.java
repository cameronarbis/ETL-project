package com.dataplatform.ingress.service;
import com.dataplatform.ingress.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import  java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String transactionsTopic;

    public KafkaProducerService(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${transactions.topic.transactions}") String transactionsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.transactionsTopic = transactionsTopic;
    }

    // publish method async
    public CompletableFuture<SendResult<String, Object>> publishTransaction(Transaction transaction) {
        logger.debug("publishing transaction {] to topic {}",
                transaction.getTransactionId(), transactionsTopic);
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(transactionsTopic, transaction.getTransactionId(),  transaction);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Successfully published transaction {} to partition {} with offset {}",
                        transaction.getTransactionId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        result.getRecordMetadata().timestamp()
                        );
            } else {
                logger.error("Failed to publish transaction {}: {}",
                        transaction.getTransactionId(),
                        ex.getMessage(), ex);
            }
        });
        return future;
    }

}
