package com.example.myapplication.features.user.home;

import androidx.annotation.Nullable;

import com.example.myapplication.features.user.UserEvent;

import java.util.List;

/**
 * View contract for the user home screen.
 */
public interface UHomeView {
    /**
     * Renders the provided events and reapplies any active search query.
     * Updates the display with the filtered event list and highlights matches based on the search query.
     *
     * @param events The list of events to display.
     * @param searchQuery The search query to highlight or filter by, or null if no search is active.
     */
    void showEvents(List<UserEvent> events, @Nullable String searchQuery);

    /**
     * Clears the current list and optionally surfaces a message.
     * Displays an empty state when no events are available or match the current filters.
     *
     * @param message The message explaining why no events are shown, or null to show a default empty state.
     */
    void showEmptyState(@Nullable String message);

    /**
     * Surfaces an unrecoverable error to the user.
     * Displays an error message, typically for network failures or data loading issues.
     *
     * @param message The error message to display.
     */
    void showError(String message);
}