package com.dataplatform.common.validation;

import com.dataplatform.common.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates Transaction objects for data quality and consistency.
 */
@Component
public class TransactionValidator {
    private static final Logger logger = LoggerFactory.getLogger(TransactionValidator.class);

    private static final List<String> VALID_TYPES =
        List.of("purchase", "refund", "transfer", "withdrawal", "deposit");

    private static final List<String> VALID_STATUSES =
        List.of("completed", "pending", "failed");

    private static final List<String> VALID_PAYMENT_METHODS =
        List.of("credit_card", "debit_card", "bank_transfer", "paypal", "crypto");

    /**
     * Validate a transaction for data quality.
     */
    public ValidationResult validate(Transaction tx) {
        List<ValidationError> errors = new ArrayList<>();

        if (tx == null) {
            errors.add(new ValidationError("NULL_TRANSACTION", "Transaction cannot be null"));
            return ValidationResult.failure(errors);
        }

        validateRequiredFields(tx, errors);
        validateFieldValues(tx, errors);
        validateTimestamp(tx, errors);
        validateAmount(tx, errors);

        logger.debug("Validation result for transaction {}: valid={}, errors={}",
            tx.getTransactionId(), errors.isEmpty(), errors.size());

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private void validateRequiredFields(Transaction tx, List<ValidationError> errors) {
        if (tx.getTransactionId() == null || tx.getTransactionId().trim().isEmpty()) {
            errors.add(new ValidationError("MISSING_TRANSACTION_ID", "Transaction ID is required", "transactionId"));
        }

        if (tx.getUserId() == null || tx.getUserId().trim().isEmpty()) {
            errors.add(new ValidationError("MISSING_USER_ID", "User ID is required", "userId"));
        }

        if (tx.getType() == null || tx.getType().trim().isEmpty()) {
            errors.add(new ValidationError("MISSING_TYPE", "Transaction type is required", "type"));
        }

        if (tx.getAmount() == null) {
            errors.add(new ValidationError("MISSING_AMOUNT", "Amount is required", "amount"));
        }

        if (tx.getCurrency() == null || tx.getCurrency().trim().isEmpty()) {
            errors.add(new ValidationError("MISSING_CURRENCY", "Currency is required", "currency"));
        }

        if (tx.getMerchant() == null || tx.getMerchant().trim().isEmpty()) {
            errors.add(new ValidationError("MISSING_MERCHANT", "Merchant is required", "merchant"));
        }

        if (tx.getCategory() == null || tx.getCategory().trim().isEmpty()) {
            errors.add(new ValidationError("MISSING_CATEGORY", "Category is required", "category"));
        }

        if (tx.getStatus() == null || tx.getStatus().trim().isEmpty()) {
            errors.add(new ValidationError("MISSING_STATUS", "Status is required", "status"));
        }

        if (tx.getPaymentMethod() == null || tx.getPaymentMethod().trim().isEmpty()) {
            errors.add(new ValidationError("MISSING_PAYMENT_METHOD", "Payment method is required", "paymentMethod"));
        }
    }

    private void validateFieldValues(Transaction tx, List<ValidationError> errors) {
        if (!VALID_TYPES.contains(tx.getType())) {
            errors.add(new ValidationError("INVALID_TYPE",
                "Type must be one of: " + VALID_TYPES, "type"));
        }

        if (!VALID_STATUSES.contains(tx.getStatus())) {
            errors.add(new ValidationError("INVALID_STATUS",
                "Status must be one of: " + VALID_STATUSES, "status"));
        }

        if (!VALID_PAYMENT_METHODS.contains(tx.getPaymentMethod())) {
            errors.add(new ValidationError("INVALID_PAYMENT_METHOD",
                "Payment method must be one of: " + VALID_PAYMENT_METHODS, "paymentMethod"));
        }

        if (tx.getCurrency() != null && tx.getCurrency().length() != 3) {
            errors.add(new ValidationError("INVALID_CURRENCY",
                "Currency code must be 3 characters (ISO 4217)", "currency"));
        }
    }

    private void validateTimestamp(Transaction tx, List<ValidationError> errors) {
        if (tx.getTimestamp() == null || tx.getTimestamp().trim().isEmpty()) {
            errors.add(new ValidationError("MISSING_TIMESTAMP", "Timestamp is required", "timestamp"));
            return;
        }

        try {
            Instant.parse(tx.getTimestamp());
        } catch (DateTimeParseException e) {
            errors.add(new ValidationError("INVALID_TIMESTAMP",
                "Timestamp must be ISO 8601 format (e.g., 2026-10-18T22:15:30Z)", "timestamp"));
        }
    }

    private void validateAmount(Transaction tx, List<ValidationError> errors) {
        if (tx.getAmount() == null) {
            return;
        }

        if (tx.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new ValidationError("INVALID_AMOUNT",
                "Amount must be greater than 0", "amount"));
        }

        if (tx.getAmount().scale() > 2) {
            errors.add(new ValidationError("INVALID_AMOUNT_PRECISION",
                "Amount must have at most 2 decimal places", "amount"));
        }

        if (tx.getAmount().compareTo(new BigDecimal("999999999.99")) > 0) {
            errors.add(new ValidationError("AMOUNT_TOO_LARGE",
                "Amount exceeds maximum (999999999.99)", "amount"));
        }
    }
}
