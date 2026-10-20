package com.dataplatform.common.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a validation operation.
 */
public class ValidationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean valid;
    private final List<ValidationError> errors;

    public ValidationResult(boolean valid) {
        this.valid = valid;
        this.errors = new ArrayList<>();
    }

    public ValidationResult(boolean valid, List<ValidationError> errors) {
        this.valid = valid;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    public static ValidationResult success() {
        return new ValidationResult(true);
    }

    public static ValidationResult failure(List<ValidationError> errors) {
        return new ValidationResult(false, errors);
    }

    public static ValidationResult failure(ValidationError error) {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(error);
        return new ValidationResult(false, errors);
    }

    public boolean isValid() { return valid; }
    public List<ValidationError> getErrors() { return Collections.unmodifiableList(errors); }

    public boolean hasError(String code) {
        return errors.stream().anyMatch(e -> e.getCode().equals(code));
    }

    @Override
    public String toString() {
        return "ValidationResult{" + "valid=" + valid + ", errorCount=" + errors.size() + '}';
    }
}
