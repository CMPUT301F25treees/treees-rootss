package com.example.myapplication.features.organizer.home;

import androidx.annotation.Nullable;

import com.example.myapplication.features.user.UserEvent;

import java.util.List;

/**
 * View contract for the organizer home screen.
 */
public interface OHomeView {
    /**
     * Displays a list of events, optionally filtered by a search query.
     *
     * @param events The list of events to display.
     * @param searchQuery The search query to highlight or filter by, or null if no search is active.
     */
    void showEvents(List<UserEvent> events, @Nullable String searchQuery);

    /**
     * Displays an empty state message when no events are available.
     *
     * @param message The message to display explaining why no events are shown.
     */
    void showEmptyState(String message);

    /**
     * Displays an error message to the user.
     *
     * @param message The error message to display.
     */
    void showError(String message);

    /**
     * Displays an informational message to the user.
     *
     * @param message The informational message to display.
     */
    void showInfo(String message);
}