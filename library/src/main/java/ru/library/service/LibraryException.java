package ru.library.service;

/** Понятная пользователю ошибка бизнес-логики (показывается в красной плашке). */
public class LibraryException extends RuntimeException {
    public LibraryException(String message) {
        super(message);
    }
}
