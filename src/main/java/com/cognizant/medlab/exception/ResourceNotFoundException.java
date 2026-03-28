package com.cognizant.medlab.exception;

/** Thrown when a requested resource does not exist (or is soft-deleted). → 404 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String entity, Object id) {
        super(entity + " not found with id: " + id);
    }
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
