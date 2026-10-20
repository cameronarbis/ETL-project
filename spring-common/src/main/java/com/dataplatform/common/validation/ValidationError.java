package com.dataplatform.common.validation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a single validation error.
 */
public class ValidationError implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String message;
    private String field;

    public ValidationError(String code) {
        this.code = code;
        this.message = code;
    }

    public ValidationError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ValidationError(String code, String message, String field) {
        this.code = code;
        this.message = message;
        this.field = field;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationError that = (ValidationError) o;
        return Objects.equals(code, that.code) && Objects.equals(field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, field);
    }

    @Override
    public String toString() {
        return "ValidationError{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", field='" + field + '\'' +
                '}';
    }
}
