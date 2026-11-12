package com.cybershield.exception;

/**
 * Thrown when an operation breaks a business rule (invalid state transition, duplicate code,
 * invalid deadline, ...). The message is safe to show to the user.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
