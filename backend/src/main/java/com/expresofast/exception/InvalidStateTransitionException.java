package com.expresofast.exception;
 

public class InvalidStateTransitionException extends RuntimeException {
 
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}