package com.example.myapplication.features.organizer.home;

import androidx.annotation.Nullable;

import com.example.myapplication.features.user.UserEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for organizer home screen event data.
 */
public class OHomeModel {
    
    public enum FilterType {
        UPCOMING,
        PAST
    }

    private final List<UserEvent> myEvents = new ArrayList<>();
    private FilterType activeFilter = FilterType.UPCOMING;

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
     * Sets the active filter type.
     * @param filter The new filter to apply.
     */
    public void setFilter(FilterType filter) {
        this.activeFilter = filter;
    }

    /**
     * Returns the current active filter.
     * @return The active filter type.
     */
    public FilterType getFilter() {
        return activeFilter;
    }

    /**
     * Returns a filtered list of the organizer's events based on the active filter.
     *
     * @return A new ArrayList containing events matching the current filter.
     */
    public List<UserEvent> getEvents() {
        long now = System.currentTimeMillis();
        List<UserEvent> filtered = new ArrayList<>();
        
        for (UserEvent event : myEvents) {
            if (matchesFilter(event, activeFilter, now)) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    /**
     * Checks if an event matches the specified filter type.
     *
     * @param event The event to check.
     * @param filter The filter type to apply.
     * @param now The current time in milliseconds since epoch.
     * @return True if the event matches the filter, false otherwise.
     */
    private boolean matchesFilter(UserEvent event, FilterType filter, long now) {
        long start = event.getStartTimeMillis();
        long end = event.getEndTimeMillis();
        long actualEnd = (end > 0) ? end : start;
        
        if (filter == FilterType.UPCOMING) {
            return actualEnd >= now;
        } else { // PAST
            return actualEnd < now;
        }
    }
}