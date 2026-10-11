package ru.practicum.moviehub.api;

public class ErrorResponse {
    private final String error;
    private final String message;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }
}

