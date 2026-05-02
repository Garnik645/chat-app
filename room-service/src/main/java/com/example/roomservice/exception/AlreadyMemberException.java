package com.example.roomservice.exception;

public class AlreadyMemberException extends RuntimeException {
    public AlreadyMemberException(String message) {
        super(message);
    }
}
