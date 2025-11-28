package com.example.myapplication.features.user.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.features.user.UserEvent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Model for the user home screen. Holds the event data and active filters.
 */
public class UHomeModel {
    private final List<UserEvent> events = new ArrayList<>();
    private final List<String> selectedInterests = new ArrayList<>();
    @Nullable
    private Long availabilityStartMillis;
    @Nullable
    private Long availabilityEndMillis;

    /**
     * Replaces the current event list with the provided collection.
     * Clears existing events and adds all non-null events from the provided list.
     *
     * @param newEvents The list of events to store, or null to clear all events.
     */
    public void setEvents(@Nullable List<UserEvent> newEvents) {
        events.clear();
        if (newEvents == null) {
            return;
        }
        for (UserEvent event : newEvents) {
            if (event != null) {
                events.add(event);
            }
        }
    }

    /**
     * Returns a copy of the stored events.
     * Creates a new list to prevent external modification of the internal event list.
     *
     * @return A new ArrayList containing all stored events.
     */
    public List<UserEvent> getEvents() {
        return new ArrayList<>(events);
    }

    /**
     * Stores the current interest filters.
     * Clears existing interests and replaces them with the provided list.
     *
     * @param interests The list of interest tags to filter by, or null to clear all interests.
     */
    public void setSelectedInterests(@Nullable List<String> interests) {
        selectedInterests.clear();
        if (interests != null) {
            selectedInterests.addAll(interests);
        }
    }

    /**
     * Returns the current selected interests as a copy.
     * Creates a new list to prevent external modification of the internal interest list.
     *
     * @return A new ArrayList containing all selected interest tags.
     */
    public List<String> getSelectedInterests() {
        return new ArrayList<>(selectedInterests);
    }

    /**
     * Persists the selected availability range.
     * Sets the time range filter for displaying events.
     *
     * @param startMillis The start of the availability range in milliseconds since epoch, or null.
     * @param endMillis The end of the availability range in milliseconds since epoch, or null.
     */
    public void setAvailabilityRange(@Nullable Long startMillis, @Nullable Long endMillis) {
        availabilityStartMillis = startMillis;
        availabilityEndMillis = endMillis;
    }

    /**
     * Clears the availability range filter.
     * Removes any time-based filtering by setting both start and end times to null.
     */
    public void clearAvailability() {
        availabilityStartMillis = null;
        availabilityEndMillis = null;
    }

    /**
     * Returns the start of the current availability filter range.
     *
     * @return The start time in milliseconds since epoch, or null if not set.
     */
    @Nullable
    public Long getAvailabilityStartMillis() {
        return availabilityStartMillis;
    }

    /**
     * Returns the end of the current availability filter range.
     *
     * @return The end time in milliseconds since epoch, or null if not set.
     */
    @Nullable
    public Long getAvailabilityEndMillis() {
        return availabilityEndMillis;
    }

    /**
     * Applies the currently configured filters to the stored events.
     * Filters out past events, applies interest matching, and applies availability range filtering.
     *
     * @return A new list containing only events that match all active filters.
     */
    public List<UserEvent> buildDisplayEvents() {
        long now = System.currentTimeMillis();
        List<UserEvent> working = new ArrayList<>();
        for (UserEvent event : events) {
            if (event != null && isUpcomingEvent(event, now)) {
                working.add(event);
            }
        }

        if (!selectedInterests.isEmpty()) {
            working = filterEventsByInterests(working, selectedInterests);
        }
        if (availabilityStartMillis != null && availabilityEndMillis != null) {
            long start = startOfDay(availabilityStartMillis);
            long end = endOfDay(availabilityEndMillis);
            working = filterEventsByAvailability(working, start, end);
        }
        return working;
    }

    /**
     * Filters out events belonging to the current user.
     * Returns only events where the organizer ID does not match the current user ID.
     *
     * @param allEvents The complete list of events to filter.
     * @param currentUserId The ID of the current user whose events should be excluded.
     * @return A new list containing only events not organized by the current user.
     */
    public static List<UserEvent> filterEventsForDisplay(@Nullable List<UserEvent> allEvents,
                                                         @Nullable String currentUserId) {
        List<UserEvent> filtered = new ArrayList<>();
        if (allEvents == null || currentUserId == null) {
            return filtered;
        }
        for (UserEvent event : allEvents) {
            if (event == null) {
                continue;
            }
            String organizerId = event.getOrganizerID();
            if (organizerId == null || !organizerId.equals(currentUserId)) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    /**
     * Returns whether an event has not yet finished.
     * Compares the event's end time (or start time if end is not set) against the current time.
     *
     * @param event The event to check.
     * @param currentMillis The current time in milliseconds since epoch.
     * @return True if the event's end time is in the future, false otherwise.
     */
    public static boolean isUpcomingEvent(@NonNull UserEvent event, long currentMillis) {
        long start = event.getStartTimeMillis();
        long end = event.getEndTimeMillis();
        long actualEnd = (end > 0) ? end : start;
        return actualEnd >= currentMillis;
    }

    /**
     * Filters events by matching their theme with the provided interests.
     * Uses case-insensitive matching between event themes and interest tags.
     *
     * @param allEvents The list of events to filter.
     * @param interests The list of interest tags to match against.
     * @return A new list containing only events whose theme matches at least one interest.
     */
    public static List<UserEvent> filterEventsByInterests(@Nullable List<UserEvent> allEvents,
                                                          @Nullable List<String> interests) {
        List<UserEvent> filtered = new ArrayList<>();
        if (allEvents == null || allEvents.isEmpty()) {
            return filtered;
        }
        if (interests == null || interests.isEmpty()) {
            filtered.addAll(allEvents);
            return filtered;
        }
        for (UserEvent event : allEvents) {
            if (event == null) {
                continue;
            }
            String theme = event.getTheme();
            if (theme == null || theme.isEmpty()) {
                continue;
            }
            for (String interest : interests) {
                if (interest != null && theme.equalsIgnoreCase(interest)) {
                    filtered.add(event);
                    break;
                }
            }
        }
        return filtered;
    }

    /**
     * Filters events to those that overlap with the provided availability range.
     * An event overlaps if any part of it falls within the specified time range.
     *
     * @param allEvents The list of events to filter.
     * @param startTime The start of the availability range in milliseconds since epoch.
     * @param endTime The end of the availability range in milliseconds since epoch.
     * @return A new list containing only events that overlap with the time range.
     */
    public static List<UserEvent> filterEventsByAvailability(@Nullable List<UserEvent> allEvents,
                                                             long startTime,
                                                             long endTime) {
        List<UserEvent> filtered = new ArrayList<>();
        if (allEvents == null || allEvents.isEmpty()) {
            return filtered;
        }
        long normalizedStart = Math.min(startTime, endTime);
        long normalizedEnd = Math.max(startTime, endTime);
        for (UserEvent event : allEvents) {
            if (event == null) {
                continue;
            }
            long eventStart = event.getStartTimeMillis();
            long eventEnd = event.getEndTimeMillis();
            if (eventStart == 0 && eventEnd == 0) {
                continue;
            }
            long actualEnd = eventEnd > 0 ? eventEnd : eventStart;
            if (actualEnd >= normalizedStart && eventStart <= normalizedEnd) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    /**
     * Calculates the start of the day (00:00:00.000) for the given timestamp.
     *
     * @param timeMillis A timestamp in milliseconds since epoch.
     * @return The timestamp of the start of that day in milliseconds since epoch.
     */
    private static long startOfDay(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Calculates the end of the day (23:59:59.999) for the given timestamp.
     *
     * @param timeMillis A timestamp in milliseconds since epoch.
     * @return The timestamp of the end of that day in milliseconds since epoch.
     */
    private static long endOfDay(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }
}