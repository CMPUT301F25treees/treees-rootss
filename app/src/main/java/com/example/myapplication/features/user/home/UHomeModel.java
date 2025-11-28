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
     */
    public List<UserEvent> getEvents() {
        return new ArrayList<>(events);
    }

    /**
     * Stores the current interest filters.
     */
    public void setSelectedInterests(@Nullable List<String> interests) {
        selectedInterests.clear();
        if (interests != null) {
            selectedInterests.addAll(interests);
        }
    }

    /**
     * Returns the current selected interests as a copy.
     */
    public List<String> getSelectedInterests() {
        return new ArrayList<>(selectedInterests);
    }

    /**
     * Persists the selected availability range.
     */
    public void setAvailabilityRange(@Nullable Long startMillis, @Nullable Long endMillis) {
        availabilityStartMillis = startMillis;
        availabilityEndMillis = endMillis;
    }

    /**
     * Clears the availability range filter.
     */
    public void clearAvailability() {
        availabilityStartMillis = null;
        availabilityEndMillis = null;
    }

    @Nullable
    public Long getAvailabilityStartMillis() {
        return availabilityStartMillis;
    }

    @Nullable
    public Long getAvailabilityEndMillis() {
        return availabilityEndMillis;
    }

    /**
     * Applies the currently configured filters to the stored events.
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
     */
    public static boolean isUpcomingEvent(@NonNull UserEvent event, long currentMillis) {
        long start = event.getStartTimeMillis();
        long end = event.getEndTimeMillis();
        long actualEnd = (end > 0) ? end : start;
        return actualEnd >= currentMillis;
    }

    /**
     * Filters events by matching their theme with the provided interests.
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

    private static long startOfDay(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

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
