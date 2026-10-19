package com.dataplatform.etl.error;

import com.dataplatform.common.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class DLQHandler {

    private static final Logger logger = LoggerFactory.getLogger(DLQHandler.class);

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${kafka.dlq.topic}")
    private String dlqTopic;

    private static final String METRIC_DLQ_SENT = "etl.dlq.sent";

    public DLQHandler(@Qualifier("dlqKafkaTemplate") KafkaTemplate<String, Map<String, Object>> kafkaTemplate,
                      ObjectMapper objectMapper,
                      MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    public void sendToDLQ(Transaction transaction, Exception exception, int retryCount) {
        try {
            if (transaction == null || transaction.getTransactionId() == null) {
                logger.error("Transaction is null or transaction id is empty - cannot send to DLQ.");
                return;
            }

            Map<String, Object> dlqMessage = new HashMap<>();
            dlqMessage.put("original_transaction", transaction);
            dlqMessage.put("error_type", exception.getClass().getSimpleName());
            dlqMessage.put("error_message", exception.getMessage());
            dlqMessage.put("stack_trace", getStackTrace(exception));

            if (exception.getCause() != null) {
                dlqMessage.put("cause_type", exception.getCause().getClass().getSimpleName());
                dlqMessage.put("cause", exception.getCause().getMessage());
            }

            dlqMessage.put("retry_count", retryCount);
            dlqMessage.put("failed_at", Instant.now().toString());
            dlqMessage.put("transaction_id", transaction.getTransactionId());
            dlqMessage.put("user_id", transaction.getUserId());
            dlqMessage.put("amount", transaction.getAmount());

            kafkaTemplate.send(dlqTopic, transaction.getTransactionId(), dlqMessage);

            logger.warn("Sent transaction {} to DLQ after {} retries. Error: {}",
                    transaction.getTransactionId(), retryCount, exception.getMessage());

            meterRegistry.counter(METRIC_DLQ_SENT).increment();
        } catch (Exception e) {
            logger.error("CRITICAL: Failed to send transaction {} to DLQ. Original error: {}. DLQ send error: {}",
                    transaction.getTransactionId(),
                    exception.getMessage(),
                    e.getMessage(),
                    e);
        }
    }

    private String getStackTrace(Exception exception) {
        if (exception == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement element : exception.getStackTrace()) {
            builder.append(element.toString()).append("\n");
        }
        return builder.toString();
    }
}
