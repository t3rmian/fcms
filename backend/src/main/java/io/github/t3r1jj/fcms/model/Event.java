package io.github.t3r1jj.fcms.model;

import java.time.Instant;

public class Event {
    private final String title;
    private final String description;
    private final EventType type;
    private final Instant time;

    public Event(String title, String description, EventType type) {
        this(title, description, type, Instant.now());
    }

    public Event(String title, String description, EventType type, Instant time) {
        this.title = title;
        this.description = description;
        this.time = time;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public EventType getType() {
        return type;
    }

    public Instant getTime() {
        return time;
    }

    public enum EventType {
        INFO, WARNING, ERROR
    }
}
