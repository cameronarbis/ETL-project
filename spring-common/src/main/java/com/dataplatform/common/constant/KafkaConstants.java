package com.dataplatform.common.constant;

/**
 * Kafka-related constants shared across services.
 */
public final class KafkaConstants {
    private KafkaConstants() {
    }

    // Topics
    public static final String TOPIC_TRANSACTIONS = "transactions";
    public static final String TOPIC_TRANSACTIONS_DLQ = "transactions-dlq";

    // Consumer group
    public static final String CONSUMER_GROUP_ETL = "etl-pipeline-group";

    // Message keys
    public static final String MESSAGE_KEY_TRANSACTION_ID = "transaction_id";

    // Retry configuration
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long INITIAL_BACKOFF_MS = 1000;
    public static final double BACKOFF_MULTIPLIER = 2.0;
}
