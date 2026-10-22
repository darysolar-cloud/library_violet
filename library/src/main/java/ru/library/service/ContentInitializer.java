package ru.library.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ContentInitializer {

    private final ContentService content;

    public ContentInitializer(ContentService content) {
        this.content = content;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        content.seedDefaults();
    }
}