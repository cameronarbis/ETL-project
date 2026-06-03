package com.dataplatform.ingress.controller;

import com.dataplatform.ingress.model.Transaction;
import com.dataplatform.ingress.service.KafkaProducerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final KafkaProducerService kafkaProducerService;

    public TransactionController(KafkaProducerService kafkaProducerService){
        this.kafkaProducerService = kafkaProducerService;
        }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTransaction(
            @Valid @RequestBody Transaction transaction) {

        log.info("Received transaction: {}", transaction.getTransactionId());
        kafkaProducerService.publishTransaction(transaction);
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("TransactionId", transaction.getTransactionId());
        responseData.put("status", "ACCEPTED");
        responseData.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.accepted().body(responseData);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> GetResponse = new HashMap<>();
        GetResponse.put("service", "ingress-service");
        GetResponse.put("status", "OK");
        GetResponse.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.OK).body(GetResponse);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        Map<String, Object> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((org.springframework.validation.FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ERROR");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
