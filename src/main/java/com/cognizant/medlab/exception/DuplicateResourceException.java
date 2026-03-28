package com.cognizant.medlab.exception;

/** Thrown when a unique-constraint violation would occur at the business layer. → 409 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String entity, String field, Object value) {
        super(entity + " already exists with " + field + ": " + value);
    }
}
