package com.cognizant.medlab.exception;

/** Thrown when a workflow state-machine rule is violated. → 409 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
