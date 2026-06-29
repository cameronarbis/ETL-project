package com.dataplatform.etl.model.Transaction;

import com.dataplatform.etl.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Dead Letter Head Queue Handler: Send failed transaction to DLQ topic
 *
 * when a transaction fails after all retry attempts:
 * 1. capture the original transaction data
 * 2. capture the error message and exception details
 * 3. send to transaction-dlq topic for manual inspection
 * 4. log the failure with full context
 *
 * the DLQ acts as a persistent queue of unprocessable messages for:
 * - debugging and root cause analysis
 * - manual intervention and data correction
 * - replay after upstream issues are fixed
 */

@Service
public class DLQHandler {
    //logger for this class
    private static final Logger logger = LoggerFactory.getLogger(DLQHandler.class);

    // Spring's Kafka template for sending messages
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    // Jacksons ObjectMapper for JSON serialization
    private final ObjectMapper objectMapper;

    // Micrometer metrics registry
    private final MeterRegistry meterRegistry;

    //topic name for dead letter queue (injected from application.yml
    @Value("${kafka.dlq.topic}")
    private String dlqTopic;

    // metric name for tracking DLQ messages sent
    private static final String METRIC_DLQ_SENT = "etl.dlq.sent";

    /**
     * constructor: Spring injects dependencies
     */
    public DLQHandler(KafkaTemplate<String, Map<String, Object>> kafkaTemplate,
                      ObjectMapper objectMapper,
                      MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }
    /**
     * send a failed transaction to the dead letter queue
     *
     * creates a structured DLQ message containing:
     * - original transaction date
     * - error message and exception details
     * - retry count
     * - timestamp of failure
     * - error type (duplicate, connection, constraint, etc.)
     *
     * @param transaction - the transaction that failed
     * @param exception - the exception that caused the failure
     * @param retryCount - how many times we tried before giving up
     */
    public void sendToDLQ(Transaction transaction, Exception exception, int retryCount) {
        try {
            // validate input
            if (transaction == null) ||transaction.getTransactionId() == null {
                logger.error("transaction id is null or transaction id is empty - cannot send to DLQ.");
                return;
            }
            // build the DLQ message as a structured map
            Map<String, Object> dlqMessage = new HashMap<>();

            // ORIGINAL DATA
            dlqMessage.put("original_transaction", transaction);

            //ERROR INFORMATION
            dlqMessage.put("error_type"exception.getClass().getSimpleName());
            dlqMessage.put("error_message", exception.getMessage());

            // full stack trace for debugging
            String stackTrace = getStackTrace(exception);
            dlqMessage.put("stack_trace", stackTrace);

            // ROOT CAUSE
            // if there's a nested cause (e.g. PSQLException wrapping a constraint violation)
            if (exception.getCause() != null) {
                dlqMessage.put("cause_type", exception.getCause().getClass().getSimpleName());
                dlqMessage.put("cause", exception.getCause().getMessage());
            }

            // RETRY INFORMATION
            dlqMessage.put("retry_count", retryCount);

            // TIMESTAMP
            dlqMessage.put("failed_at", Instant.now().toString());

            // CONTEXT INFORMATION
            dlqMessage.put("transaction_id", transaction.getTransactionId());
            dlqMessage.put("user_id", transaction.getUserId());
            dlqMessage.put("amount", transaction.getAmount());

            // send to DLQ topic
            // key: transaction ID (Kafka partitions by key, so related messages go to the same partition)
            // value: DLQ message (Spring will serialize the JSON via KafkaTemplate)
            kafkaTemplate.send(dlqTopic, transaction.getTransactionId(), dlqMessage);

            // log the DLQ send
            logger.warn("Sent transaction {} to DLQ after {} retries. Error: {}",
                    transaction.getTransactionId(), retryCount, exception.getMessage());

            // track metric
            meterRegistry.counter(METRIC_DLQ_SENT).increment();
        } catch (Exception e) {
            // CRITICAL: if sending to DLQ itself fails, log the failure - log to stderr so operation team can view
            logger.error("CRITICAL: Failed to send transaction {} to DLQ. Original error {}. " +
                    "DLQ send error {}"),
            transaction.getTransactionID(),
            exception.getMessage(),
            e.getMessage(),
            e.getMessage(),
            e);
        }
    }

    /**
     * convert excpetion stack trace to srting for logging.
     *
     * @param exception - the exception to extract stack trace from
     * @return stack trace as a single string
     */
private String getStackTrace(Exception exception) {
    if (exception == null) {
        return null;
    }

    // build stack trace string
    StringBuilder builder = new StringBuilder();
    for (StackTraceElement element : exception.getStackTrace()) {
        sb.append(element.toString()).append("\n");
    }
    return builder.toString();
}
}