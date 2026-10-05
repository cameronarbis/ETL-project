package com.dataplatform.etl.service;

import com.dataplatform.etl.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/*
 transaction consumer service: main Kafka listener

 responsibilities:
 1. listen to transaction topic
 2. consume transaction messages
 3. attempt database insert
 4. manually commit offset only on success

 retry + DLQ strategy now lives at the container level: a failed insert
 propagates out of consume(), and KafkaConsumerConfig's DefaultErrorHandler
 handles backoff/redelivery and DLQ routing (see kafkaErrorHandler bean).
 */

@Service
public class TransactionConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumerService.class);

    private final DatabaseService databaseService;

    public TransactionConsumerService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     main Kafka listener: consume Transaction messages.

     @param transaction -> the transaction message from Kafka
     @param ack -> manual acknowledgement handle (we control when offset commits)
     */
    @KafkaListener(
            topics = "${kafka.topic.transactions}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(Transaction transaction, Acknowledgment ack) throws Exception {
        // input validation
        if (transaction == null || transaction.getTransactionId() == null
                || transaction.getUserId() == null
                || transaction.getAmount() == null) {
            logger.error("transaction or transaction id or user id or amount is null");
            ack.acknowledge();
            return;
        }
        logger.info("Received transaction {}", transaction.getTransactionId());

        // let a failure propagate: the container's DefaultErrorHandler
        // (KafkaConsumerConfig.kafkaErrorHandler) retries with backoff and
        // routes to the DLQ once attempts are exhausted.
        databaseService.insertTransaction(transaction);

        logger.info("Successfully processed transaction {}", transaction.getTransactionId());
        ack.acknowledge();
    }
}
