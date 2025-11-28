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
                if (mine.isEmpty()) {
                    view.showEmptyState("No events created by you");
                } else {
                    view.showEvents(model.getEvents(), searchQuery);
                }
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
     */
    public void onSearchQueryChanged(@Nullable String query) {
        searchQuery = query == null ? "" : query;
        if (view != null) {
            view.showEvents(model.getEvents(), searchQuery);
        }
    }

    /**
     * Handles a filter chip/menu selection.
     */
    public void onFilterSelected(String filterLabel) {
        if (view != null) {
            view.showInfo(filterLabel + " selected");
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
