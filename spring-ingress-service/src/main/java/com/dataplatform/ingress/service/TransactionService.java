package com.dataplatform.ingress.service;

import com.dataplatform.ingress.model.Transaction;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class TransactionService {

    private final KafkaProducerService kafkaProducerService;
    private final Counter transactionsReceived;
    private final Counter transactionsPublished;
    private final Counter transactionsFailed;
    private final Timer publishTimer;

    public TransactionService(
            KafkaProducerService kafkaProducerService,
            MeterRegistry meterRegistry) {
        this.kafkaProducerService = kafkaProducerService;
        
        this.transactionsReceived = Counter.builder("transactions.received")
                .description("Total number of transactions received")
                .register(meterRegistry);
        
        this.transactionsPublished = Counter.builder("transactions.published")
                .description("Total number of transactions successfully published to Kafka")
                .register(meterRegistry);
        
        this.transactionsFailed = Counter.builder("transactions.failed")
                .description("Total number of transactions that failed to publish")
                .register(meterRegistry);
        
        this.publishTimer = Timer.builder("transactions.publish.time")
                .description("Time taken to publish transactions to Kafka")
                .register(meterRegistry);
    }

    public CompletableFuture<Void> processTransaction(Transaction transaction) {
        transactionsReceived.increment();
        
        log.info("Processing transaction: {} from user: {} for amount: {} {}",
                transaction.getTransactionId(),
                transaction.getUserId(),
                transaction.getCurrency(),
                transaction.getAmount());

        Timer.Sample sample = Timer.start();

        validateTransaction(transaction);
        enrichTransaction(transaction);

        return kafkaProducerService.publishTransaction(transaction)
                .thenRun(() -> {
                    sample.stop(publishTimer);
                    transactionsPublished.increment();
                    log.debug("Transaction {} processing completed", transaction.getTransactionId());
                })
                .exceptionally(ex -> {
                    sample.stop(publishTimer);
                    transactionsFailed.increment();
                    log.error("Failed to process transaction {}: {}",
                            transaction.getTransactionId(), ex.getMessage(), ex);
                    throw new RuntimeException("Failed to publish transaction", ex);
                });
    }

    private void validateTransaction(Transaction transaction) {
        if (transaction.getAmount().signum() == 0) {
            throw new IllegalArgumentException("Transaction amount cannot be zero");
        }

        String type = transaction.getType().toLowerCase();
        if (!type.matches("purchase|refund|transfer|withdrawal|deposit")) {
            log.warn("Unusual transaction type: {}", transaction.getType());
        }

        String status = transaction.getStatus().toLowerCase();
        if (!status.matches("completed|pending|failed")) {
            log.warn("Unusual transaction status: {}", transaction.getStatus());
        }
    }

    private void enrichTransaction(Transaction transaction) {
        log.debug("Transaction enrichment point for {}", transaction.getTransactionId());
    }
}
