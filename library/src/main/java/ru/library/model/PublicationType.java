package ru.library.model;

public enum PublicationType {
    BOOK("Книга"), MAGAZINE("Журнал");

    private final String title;

    PublicationType(String title) { this.title = title; }

    public String getTitle() { return title; }
}
