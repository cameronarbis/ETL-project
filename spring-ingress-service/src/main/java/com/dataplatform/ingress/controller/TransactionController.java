package com.dataplatform.ingress.controller;

import com.dataplatform.ingress.model.ApiResponse;
import com.dataplatform.ingress.model.Transaction;
import com.dataplatform.ingress.service.TransactionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/transactions")
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<ApiResponse<Map<String, String>>>> receiveTransaction(
            @Valid @RequestBody Transaction transaction) {
        
        log.info("Received transaction: {}", transaction.getTransactionId());

        return transactionService.processTransaction(transaction)
                .thenApply(result -> {
                    Map<String, String> responseData = new HashMap<>();
                    responseData.put("transactionId", transaction.getTransactionId());
                    responseData.put("status", "ACCEPTED");
                    
                    return ResponseEntity
                            .accepted()
                            .body(ApiResponse.success(
                                    responseData,
                                    "Transaction accepted and queued for processing"));
                })
                .exceptionally(ex -> {
                    log.error("Failed to process transaction: {}", ex.getMessage());
                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error(
                                    ex.getMessage(),
                                    "Failed to process transaction"));
                });
    }

    @PostMapping("/batch")
    public CompletableFuture<ResponseEntity<ApiResponse<Map<String, Object>>>> receiveTransactionBatch(
            @Valid @RequestBody java.util.List<Transaction> transactions) {
        
        log.info("Received batch of {} transactions", transactions.size());

        CompletableFuture<?>[] futures = transactions.stream()
                .map(transactionService::processTransaction)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures)
                .thenApply(result -> {
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("count", transactions.size());
                    responseData.put("status", "ACCEPTED");
                    
                    return ResponseEntity
                            .accepted()
                            .body(ApiResponse.success(
                                    responseData,
                                    "Batch of " + transactions.size() + " transactions accepted"));
                })
                .exceptionally(ex -> {
                    log.error("Failed to process batch: {}", ex.getMessage());
                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error(
                                    ex.getMessage(),
                                    "Failed to process transaction batch"));
                });
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("service", "ingress-service");
        health.put("status", "UP");
        
        return ResponseEntity.ok(ApiResponse.success(health, "Service is healthy"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation failed: {}", errors);
        
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(
                        "VALIDATION_ERROR",
                        "Request validation failed: " + errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid request: {}", ex.getMessage());
        
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(
                        "INVALID_REQUEST",
                        ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred"));
    }
}
