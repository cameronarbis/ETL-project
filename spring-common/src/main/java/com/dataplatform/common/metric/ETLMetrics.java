package com.dataplatform.common.metric;

/**
 * Metric names for ETL pipeline observability.
 */
public final class ETLMetrics {
    private ETLMetrics() {
    }

    // Transaction processing
    public static final String TRANSACTIONS_RECEIVED = "etl.transactions.received";
    public static final String TRANSACTIONS_PROCESSED = "etl.transactions.processed";
    public static final String TRANSACTIONS_FAILED = "etl.transactions.failed";
    public static final String TRANSACTIONS_DUPLICATE = "etl.transactions.duplicate";
    public static final String TRANSACTIONS_VALIDATION_ERROR = "etl.transactions.validation_error";

    // Processing latency
    public static final String PROCESSING_DURATION = "etl.transaction.processing.duration";
    public static final String DATABASE_INSERT_DURATION = "etl.transaction.database.insert.duration";

    // Error handling
    public static final String DLQ_SENT = "etl.dlq.sent";
    public static final String DLQ_REPLAYED = "etl.dlq.replayed";

    // Kafka consumer
    public static final String KAFKA_CONSUMER_LAG = "etl.kafka.consumer.lag";
    public static final String KAFKA_REBALANCES = "etl.kafka.rebalances";

    // Database
    public static final String DATABASE_CONNECTION_ERRORS = "etl.database.connection.errors";
}
