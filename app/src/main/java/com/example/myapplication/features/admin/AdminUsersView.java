package com.example.myapplication.features.admin;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * View interface for the admin "Browse Profiles" screen.
 * <p>
 * Implemented by UI components (e.g., {@link AUsersFrag}) that display
 * a list of non-admin users and expose inline actions such as organizer
 * demotion. The corresponding controller {@link AdminUsersController}
 * handles data loading and business rules.
 * <p>
 */
public interface AdminUsersView {

    /**
     * Displays the current list of user rows.
     *
     * @param users immutable list of user rows to render
     */
    void showUsers(@NonNull List<AdminUserAdapter.UserRow> users);

    /**
     * Indicates whether the "empty state" view should be visible.
     *
     * @param showEmpty true if there are no users to show, false otherwise
     */
    void showEmptyState(boolean showEmpty);

    /**
     * Shows or hides a progress indicator while user data is loading.
     *
     * @param loading true to indicate loading, false to hide the indicator
     */
    void showLoading(boolean loading);

    /**
     * Displays an informational or error message to the admin, typically
     * via a Toast or Snackbar.
     *
     * @param message human-readable message to display
     */
    void showMessage(@NonNull String message);
}
