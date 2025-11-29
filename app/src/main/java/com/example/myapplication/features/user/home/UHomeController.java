package com.example.myapplication.features.user.home;

import androidx.annotation.Nullable;

import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.features.user.UserEvent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * Controller for the user home screen. Coordinates data loading and filtering.
 */
public class UHomeController {
    private final FirebaseEventRepository repository;
    private final FirebaseAuth auth;
    private final UHomeModel model;
    private UHomeView view;
    private String searchQuery = "";
    @Nullable
    private String currentUserId;

    /**
     * Constructs a UHomeController with the specified dependencies.
     *
     * @param repository The Firebase repository for event data operations.
     * @param auth The Firebase authentication instance for user management.
     * @param model The model that holds event data and filter state for this screen.
     * @param view The view interface for displaying user home screen content.
     */
    public UHomeController(FirebaseEventRepository repository,
                           FirebaseAuth auth,
                           UHomeModel model,
                           UHomeView view) {
        this.repository = repository;
        this.auth = auth;
        this.model = model;
        this.view = view;
    }

    /**
     * Clears the attached view reference to avoid using a dead Fragment.
     */
    public void detachView() {
        view = null;
    }

    /**
     * Initiates loading of all events and applies the current filters.
     * Fetches events from the repository, filters them for display based on the current user,
     * and updates the view with the filtered results.
     */
    public void loadEvents() {
        final String userId = resolveUserId();
        if (userId == null) {
            if (view != null) {
                view.showError("User not authenticated");
            }
            return;
        }

        repository.getAllEvents(new FirebaseEventRepository.EventListCallback() {
            @Override
            public void onEventsFetched(List<UserEvent> events) {
                if (view == null) {
                    return;
                }
                List<UserEvent> displayable = UHomeModel.filterEventsForDisplay(events, userId);
                model.setEvents(displayable);
                applyFiltersInternal(true);
            }

            @Override
            public void onError(Exception e) {
                if (view != null) {
                    view.showError("Failed to fetch: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Updates the stored search query and reapplies filters.
     * Refreshes the view to display events matching the new search criteria.
     *
     * @param query The new search query, or null to clear the search.
     */
    public void onSearchQueryChanged(@Nullable String query) {
        searchQuery = query == null ? "" : query;
        applyFiltersInternal(false);
    }

    /**
     * Updates the selected interests and reapplies filters.
     * Refreshes the view to display only events matching the selected interests.
     *
     * @param interests The list of interest tags to filter by.
     */
    public void updateInterests(List<String> interests) {
        model.setSelectedInterests(interests);
        applyFiltersInternal(false);
    }

    /**
     * Updates the availability range and reapplies filters.
     * Refreshes the view to display only events within the specified time range.
     *
     * @param startMillis The start of the availability range in milliseconds since epoch, or null.
     * @param endMillis The end of the availability range in milliseconds since epoch, or null.
     */
    public void updateAvailability(@Nullable Long startMillis, @Nullable Long endMillis) {
        model.setAvailabilityRange(startMillis, endMillis);
        applyFiltersInternal(false);
    }

    /**
     * Clears the availability range and reapplies filters.
     * Removes any time-based filtering and refreshes the view.
     */
    public void clearAvailability() {
        model.clearAvailability();
        applyFiltersInternal(false);
    }

    /**
     * Returns the currently selected interest filters.
     *
     * @return A list of selected interest tags.
     */
    public List<String> getSelectedInterests() {
        return model.getSelectedInterests();
    }

    /**
     * Returns the start of the current availability filter range.
     *
     * @return The start time in milliseconds since epoch, or null if not set.
     */
    @Nullable
    public Long getAvailabilityStartMillis() {
        return model.getAvailabilityStartMillis();
    }

    /**
     * Returns the end of the current availability filter range.
     *
     * @return The end time in milliseconds since epoch, or null if not set.
     */
    @Nullable
    public Long getAvailabilityEndMillis() {
        return model.getAvailabilityEndMillis();
    }

    /**
     * Checks whether an availability filter is currently active.
     *
     * @return True if both start and end times are set, false otherwise.
     */
    public boolean hasAvailabilityFilter() {
        return model.getAvailabilityStartMillis() != null && model.getAvailabilityEndMillis() != null;
    }

    /**
     * Applies current filters to the event list and updates the view.
     *
     * @param notifyIfEmpty Whether to show an empty state message if no events match the filters.
     */
    private void applyFiltersInternal(boolean notifyIfEmpty) {
        if (view == null) {
            return;
        }
        List<UserEvent> filtered = model.buildDisplayEvents();
        if (filtered.isEmpty()) {
            view.showEmptyState(notifyIfEmpty ? "No events found" : null);
        } else {
            view.showEvents(filtered, searchQuery);
        }
    }

    /**
     * Resolves and caches the current user's ID from Firebase Authentication.
     * Returns a cached value if available, otherwise fetches from FirebaseAuth.
     *
     * @return The current user's ID, or null if no user is authenticated.
     */
    @Nullable
    private String resolveUserId() {
        if (currentUserId != null) {
            return currentUserId;
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        }
        return currentUserId;
    }
}