package com.example.myapplication.features.admin;

import androidx.annotation.NonNull;

import com.example.myapplication.features.user.UserEvent;

import java.util.List;

/**
 * View interface for the admin home screen.
 * <p>
 * Implemented by {@code Fragment} classes (such as {@link AHomeFrag}) that
 * display a grid of events / photos for administrators. The corresponding
 * controller {@link AdminHomeController} coordinates data loading and
 * filtering and invokes these callbacks to update the UI.
 * <p>
 */
public interface AdminHomeView {

    /**
     * Displays the filtered list of events for the current mode and query.
     *
     * @param events immutable list of events to render
     */
    void showEvents(@NonNull List<UserEvent> events);

    /**
     * Updates the view to reflect the current mode (events vs photos).
     * Typical implementations will update labels, hints, and/or icons.
     *
     * @param mode the active admin home mode
     */
    void showMode(@NonNull AdminHomeMode mode);

    /**
     * Shows or hides a progress indicator while events are being loaded or
     * a long-running operation is in progress.
     *
     * @param loading true to indicate loading, false otherwise
     */
    void showLoading(boolean loading);

    /**
     * Displays an error message to the admin when data loading or filtering
     * fails. Implementations will typically use a Toast or Snackbar.
     *
     * @param message human-readable error message
     */
    void showError(@NonNull String message);
}
