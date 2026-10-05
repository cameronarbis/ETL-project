package com.dataplatform.etl.service;

import com.dataplatform.etl.model.Transaction;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

@Service
public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;

    private static final String METRIC_TRANSACTIONS_INSERTED = "etl.transactions.inserted";
    private static final String METRIC_TRANSACTIONS_ERRORS = "etl.transactions.errors";
    private static final String METRIC_TRANSACTIONS_DUPLICATE = "etl.transactions.duplicate";

    public DatabaseService(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.meterRegistry = meterRegistry;
    }

    public boolean insertTransaction(Transaction transaction) throws Exception {
        if (transaction == null || transaction.getTransactionId() == null) {
            logger.error("Cannot insert null transaction or transaction with null ID");
            meterRegistry.counter(METRIC_TRANSACTIONS_ERRORS).increment();
            throw new IllegalArgumentException("Cannot insert null transaction or transaction with null ID");
        }
        try {
            // Resolve FK lookup IDs via DB helper functions
            Integer customerId = jdbcTemplate.queryForObject(
                    "SELECT get_or_create_customer(NULL, ?, ?)",
                    Integer.class,
                    transaction.getUserId(), transaction.getCustomerEmail()
            );

            Integer merchantId = jdbcTemplate.queryForObject(
                    "SELECT get_or_create_merchant(?)",
                    Integer.class,
                    transaction.getMerchant()
            );

            Integer categoryId = jdbcTemplate.queryForObject(
                    "SELECT category_id FROM categories WHERE category_name = ?",
                    Integer.class,
                    transaction.getCategory()
            );

            Integer locationId = null;
            Transaction.Location loc = transaction.getLocation();
            if (loc != null) {
                locationId = jdbcTemplate.queryForObject(
                        "SELECT get_or_create_location(?, ?, ?)",
                        Integer.class,
                        loc.getCity(), loc.getCountryCode(), loc.getIpAddress()
                );
            }

            String deviceType = null;
            String sessionUuid = null;
            if (transaction.getMetadata() != null) {
                deviceType = transaction.getMetadata().getDevice();
                sessionUuid = transaction.getMetadata().getSessionId();
            }

            String sql = "INSERT INTO transactions " +
                    "(transaction_uuid, customer_id, merchant_id, category_id, location_id, " +
                    "transaction_type, amount_usd, original_amount, original_currency, " +
                    "status, payment_method, device_type, session_uuid, notes, transaction_timestamp) " +
                    "VALUES (CAST(? AS UUID), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS UUID), ?, ?)";

            int rowsInserted = jdbcTemplate.update(sql,
                    transaction.getTransactionId(),
                    customerId,
                    merchantId,
                    categoryId,
                    locationId,
                    transaction.getType(),
                    transaction.getAmount(),
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    transaction.getStatus(),
                    transaction.getPaymentMethod(),
                    deviceType,
                    sessionUuid,
                    transaction.getNotes(),
                    Timestamp.from(Instant.parse(transaction.getTimestamp()))
            );

            if (rowsInserted == 1) {
                logger.info("Successfully inserted transaction: {}", transaction.getTransactionId());
                meterRegistry.counter(METRIC_TRANSACTIONS_INSERTED).increment();
                return true;
            } else {
                logger.warn("Insert returned {} rows (expected 1): {}", rowsInserted, transaction.getTransactionId());
                meterRegistry.counter(METRIC_TRANSACTIONS_ERRORS).increment();
                throw new RuntimeException("Insert returned " + rowsInserted + " rows");
            }
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && (e.getMessage().contains("duplicate") || e.getMessage().contains("unique"))) {
                logger.info("Transaction already exists (duplicate): {}. Treating as success.", transaction.getTransactionId());
                meterRegistry.counter(METRIC_TRANSACTIONS_DUPLICATE).increment();
                return true;
            } else {
                logger.error("Data integrity violation for transaction {}: {}", transaction.getTransactionId(), e.getMessage(), e);
                meterRegistry.counter(METRIC_TRANSACTIONS_ERRORS).increment();
                throw e;
            }
        }
    }
}
