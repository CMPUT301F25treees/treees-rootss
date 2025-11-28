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
     */
    public List<UserEvent> getEvents() {
        return new ArrayList<>(myEvents);
    }
}
