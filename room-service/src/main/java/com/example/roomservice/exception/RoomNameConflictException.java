package com.example.roomservice.exception;

public class RoomNameConflictException extends RuntimeException {
    public RoomNameConflictException(String message) {
        super(message);
    }
}
