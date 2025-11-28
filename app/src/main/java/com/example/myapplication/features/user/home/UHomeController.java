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
     */
    public void onSearchQueryChanged(@Nullable String query) {
        searchQuery = query == null ? "" : query;
        applyFiltersInternal(false);
    }

    /**
     * Updates the selected interests and reapplies filters.
     */
    public void updateInterests(List<String> interests) {
        model.setSelectedInterests(interests);
        applyFiltersInternal(false);
    }

    /**
     * Updates the availability range and reapplies filters.
     */
    public void updateAvailability(@Nullable Long startMillis, @Nullable Long endMillis) {
        model.setAvailabilityRange(startMillis, endMillis);
        applyFiltersInternal(false);
    }

    /**
     * Clears the availability range and reapplies filters.
     */
    public void clearAvailability() {
        model.clearAvailability();
        applyFiltersInternal(false);
    }

    public List<String> getSelectedInterests() {
        return model.getSelectedInterests();
    }

    @Nullable
    public Long getAvailabilityStartMillis() {
        return model.getAvailabilityStartMillis();
    }

    @Nullable
    public Long getAvailabilityEndMillis() {
        return model.getAvailabilityEndMillis();
    }

    public boolean hasAvailabilityFilter() {
        return model.getAvailabilityStartMillis() != null && model.getAvailabilityEndMillis() != null;
    }

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
