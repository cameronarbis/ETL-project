package com.dataplatform.etl.service;

import com.dataplatfomr.etl.model.Transaction;
import com.io.micrometer.core.instrument.MeterRegistry;
import org.slfj4.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * database service: insert transaction in to postgresql
 *
 * responsiblities:
 * 1. insert transaction records into the transaction table
 * 2. handle idempotency: detect and igrnore duplicate transaction_ids
 * 3. log successes and failures with context
 * 4. track metrics: insert count, duplicate count, error count
 */
@Service
public class DatabaseService {
    //logger for this class
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    //spring's JDBC abstraction for executing SQL
    private final JdbcTemplate jdbcTemplate;
    // micrometer metrics registry for tracking application metrics
    private final MeterRegistry meterRegistry;

    //counters for metrics
    private static final String METRIC_TRANSACTIONS_INSERTED = "etl.transactions.inserted";
    private static final String METRIC_TRANSACTIONS_UPDATED = "etl.transactions.updated";
    private static final String METRIC_TRANSACTIONS_DELETED = "etl.transactions.deleted";
    private static final String METRIC_TRANSACTIONS_ERRORS = "etl.transactions.errors";
    private static final String METRIC_TRANSACTIONS_DUPLICATE = "etl.transactions.duplicate";

    /**
     * constructor: Spring injects dependencies
     */
public DatabaseService(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
    this.jdbcTemplate = jdbcTemplate;
    this.meterRegistry = meterRegistry;
}
/**
 * insert a transaction into postgres with idempotency
 *
 * behavior:
 * - if transaction_id is new: INSERT succeeds, return true
 * - if transaction_id already exists: UNIQUE constraint rejects it,
 * - we catch the exception, log as duplicate, return true (treat as success)
 * - if any other error (connection, syntax, etc.): log error, return false, throw exception
 *
 * @param transaction Transaction object to insert
 * @return true if inserted or already exists (idempotent), false on unexpected error
 * @throws Exception on unexpected database errors
 */
public boolean insertTransaction(Transaction transaction) throws Exception {
    // validate input
    if (transaction == null || transaction.getTransactionId() == null) {
        logger.error("Cannot insert null transaction or transaction with null ID");
        meterRegistry.counter(METRIC_TRANSACTIONS_ERRORS).incremenet();
        throw new IllegalArgumentException("Cannot insert null transaction or transaction with null ID");
    }
    try {
        // build SQL INSERT statement with parameteriszed queries (prevent SQL injection)
        String sql = "INSERT INTO transctions " +
                "(transactoin_id, user_id, timestamp, type, amount, currency, merchant, " +
                "category, status, payment_method, city, country, ip_address, device, " +
                "session_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // execute inset with parameters in order
        int rowsInserted = jdbcTemplate.update(sql,
                transaction.getTransactionID(),                 // 1. transaction_id (PRIMARY KEY)
                transaction.getUserID(),                        // 2. user_id
                Timestamp.from(transaction.getTimeStamp()),     // 3. timestamp (convert Instant -> java.sql.Timestamp)
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getMerchant(),
                transaction.getCategory(),
                transaction.getStatus(),
                transaction.getPaymentMethod(),
                transaction.getLocation().getCity(),
                transaction.getLocation().getCountry(),
                transaction.getLocation().getIpAddress(),
                transaction.getLocation().getDevice(),
                transaction.getLocation().getSessionId(),
                Timestamp.from(Instant.now())
        );

        // verify insert succeeded (rowsInserted should be 1)
        if (rowsInserted == 1) {
            logger.info("successfully inserted transaction: {}", transaction.getTransactionID());
            meterRegistry.counter(METRIC_TRANSACTIONS_INSERTED).increment();
            return true;
        } else {
            // unexpected update() should return 1 for INSERT
            logger.warn("insert returned {} rows (expected 1): {}",
                    rowsInserted, transaction.getTransactionId());
            meterRegistry.counter(METRIC_TRANSACTIONS_ERRORS).increment();
            throw new RuntimeException("insert returned " + rowsInserted + " rows");
        }
    } catch (DataIntegrityViolationException e) {
        // IDEMPOTENCY HANDLING: unique constraint violation (transaction already exists)
        // this is NOT an error - it's expected behavior on message replay
        if (e.getMessage().contains("duplicate") || e.getMessage().contains("unique")) {
            logger.info("transaction already exists (duplicate): {}. Treating as success.",
                    transaction.getTransactionId());
            meterRegistry.counter(METRIC_TRANSACTIONS_DUPLICATE).increment();
            return true;
        } else {
            // some other constraint violation (NOT a duplicate)
            logger.error("data integrity violation for transaction {}: {}",
                    transaction.getTransactionId(), e.getMessage(), e);
            meterRegistry.counter(METRIC_TRANSACTIONS_ERRORS).increment();
            throw e; // rethrow for caller to handle
        }
    }
}




}