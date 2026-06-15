package com.dataplatform.etl.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Objects;

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

    public Transaction() {
    }

    public Transaction(String transactionId, String userId, String timestamp,
                       String type, BigDecimal amount, String currency,
                       String merchant, String category, String status,
                       String paymentMethod, Location location, Metadata metadata,
                       String customerEmail, String notes) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.timestamp = timestamp;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.merchant = merchant;
        this.category = category;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.location = location;
        this.metadata = metadata;
        this.customerEmail = customerEmail;
        this.notes = notes;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
    public String getMerchant() {
        return merchant;
    }
    public String getCategory() {
        return category;
    }
    public String getStatus() {
        return status;
    }
    public String getPaymentMethod() {
        return paymentMethod;
    }
    public Location getLocation() {
        return location;
    }
    public Metadata getMetadata() {
        return metadata;
    }
    public String getCustomerEmail() {
        return customerEmail;
    }
    public String getNotes() {
        return notes;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    public void setType(String type) {
        this.type = type;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    public void setLocation(Location location) {
        this.location = location;
    }
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public static class Location {
        private String city;

        @JsonProperty("country")
        private String countryCode;

        @JsonProperty("ip_address")
        private String ipAddress;

        public Location() {
        }

        public Location(String city, String countryCode, String ipAddress) {
            this.city = city;
            this.countryCode = countryCode;
            this.ipAddress = ipAddress;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }
    }

    public static class Metadata {
        private String device;

        @JsonProperty("session_id")
        private String sessionId;

        public Metadata() {
        }
        public Metadata(String device, String sessionId) {
            this.device = device;
            this.sessionId = sessionId;
        }
        public String getDevice() {
            return device;
        }
        public void setDevice(String device) {
            this.device = device;
        }
        public String getSessionId() {
            return sessionId;
        }
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", userId='" + userId + '\'' +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", merchant='" + merchant + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
}



