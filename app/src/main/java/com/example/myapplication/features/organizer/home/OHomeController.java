package com.example.myapplication.features.organizer.home;

import androidx.annotation.Nullable;

import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.features.user.UserEvent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the organizer home screen.
 */
public class OHomeController {
    private final FirebaseEventRepository repository;
    private final FirebaseAuth auth;
    private final OHomeModel model;
    private OHomeView view;
    private String searchQuery = "";
    @Nullable
    private String currentUserId;

    /**
     * Constructs an OHomeController with the specified dependencies.
     *
     * @param repository The Firebase repository for event data operations.
     * @param auth The Firebase authentication instance for user management.
     * @param model The model that holds the event data for this screen.
     * @param view The view interface for displaying organizer home screen content.
     */
    public OHomeController(FirebaseEventRepository repository,
                           FirebaseAuth auth,
                           OHomeModel model,
                           OHomeView view) {
        this.repository = repository;
        this.auth = auth;
        this.model = model;
        this.view = view;
    }

    /**
     * Clears the attached view reference to avoid dispatching to a destroyed fragment.
     */
    public void detachView() {
        view = null;
    }

    /**
     * Loads events owned by the signed-in organizer.
     * Fetches all events from the repository and filters them to show only events
     * where the current user is the organizer. Updates the view with the filtered
     * results or shows an empty state if no events are found.
     */
    public void loadOrganizerEvents() {
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
                List<UserEvent> mine = new ArrayList<>();
                if (events != null) {
                    for (UserEvent event : events) {
                        if (event != null && userId.equals(event.getOrganizerID())) {
                            mine.add(event);
                        }
                    }
                }
                model.setEvents(mine);
                refreshView();
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
     * Updates the search query and reapplies filtering.
     * Refreshes the view to display events matching the new search criteria.
     *
     * @param query The new search query, or null to clear the search.
     */
    public void onSearchQueryChanged(@Nullable String query) {
        searchQuery = query == null ? "" : query;
        refreshView();
    }

    /**
     * Handles a filter chip/menu selection.
     * Notifies the view that a filter option has been selected.
     *
     * @param filterLabel The label of the selected filter.
     */
    public void onFilterSelected(String filterLabel) {
        if (filterLabel.equalsIgnoreCase("Past Events") || filterLabel.equalsIgnoreCase("Past")) {
            model.setFilter(OHomeModel.FilterType.PAST);
        } else if (filterLabel.equalsIgnoreCase("Clear Filter")) {
            model.setFilter(OHomeModel.FilterType.UPCOMING); // Reset to default
        } else {
            model.setFilter(OHomeModel.FilterType.UPCOMING); // Default for "Upcoming" or unknown
        }
        
        if (view != null) {
            view.showInfo(filterLabel + " selected");
        }
        refreshView();
    }

    private void refreshView() {
        if (view == null) {
            return;
        }
        List<UserEvent> events = model.getEvents();
        view.showEvents(events, searchQuery);
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