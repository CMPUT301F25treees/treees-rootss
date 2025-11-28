package com.example.myapplication.features.organizer.home;

import androidx.annotation.Nullable;

import com.example.myapplication.features.user.UserEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for organizer home screen event data.
 */
public class OHomeModel {
    private final List<UserEvent> myEvents = new ArrayList<>();

    /**
     * Replaces the stored events with the provided list.
     * Clears the current event list and adds all non-null events from the provided list.
     * Null events in the provided list are filtered out.
     *
     * @param events The list of events to store, or null to clear all events.
     */
    public void setEvents(@Nullable List<UserEvent> events) {
        myEvents.clear();
        if (events == null) {
            return;
        }
        for (UserEvent event : events) {
            if (event != null) {
                myEvents.add(event);
            }
        }
    }

    /**
     * Returns a copy of the organizer's events.
     * Creates a new list to prevent external modification of the internal event list.
     *
     * @return A new ArrayList containing all stored events.
     */
    public List<UserEvent> getEvents() {
        return new ArrayList<>(myEvents);
    }
}