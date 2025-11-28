package com.example.myapplication.features.organizer.home;

import androidx.annotation.Nullable;

import com.example.myapplication.features.user.UserEvent;

import java.util.List;

/**
 * View contract for the organizer home screen.
 */
public interface OHomeView {
    void showEvents(List<UserEvent> events, @Nullable String searchQuery);

    void showEmptyState(String message);

    void showError(String message);

    void showInfo(String message);
}
