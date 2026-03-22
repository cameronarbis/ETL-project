package com.dataplatform.ingress.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Transaction model matching the data generator output
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @NotBlank(message = "Transaction ID is required")
    @JsonProperty("transaction_id")
    private String transactionId;

    @NotBlank(message = "User ID is required")
    @JsonProperty("user_id")
    private String userId;

    @NotNull(message = "Timestamp is required")
    private String timestamp;

    @NotBlank(message = "Type is required")
    private String type;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Merchant is required")
    private String merchant;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Status is required")
    private String status;

    @JsonProperty("payment_method")
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private Location location;
    private Metadata metadata;

    @JsonProperty("customer_email")
    private String customerEmail;

    private String notes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private String city;
        
        @JsonProperty("country")
        private String countryCode;
        
        @JsonProperty("ip_address")
        private String ipAddress;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private String device;
        
        @JsonProperty("session_id")
        private String sessionId;
    }
}
