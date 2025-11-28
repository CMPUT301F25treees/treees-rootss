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
     */
    void showEvents(List<UserEvent> events, @Nullable String searchQuery);

    /**
     * Clears the current list and optionally surfaces a message.
     */
    void showEmptyState(@Nullable String message);

    /**
     * Surfaces an unrecoverable error to the user.
     */
    void showError(String message);
}
