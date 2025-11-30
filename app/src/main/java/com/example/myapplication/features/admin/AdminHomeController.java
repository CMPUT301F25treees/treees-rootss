package com.example.myapplication.features.admin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.features.user.UserEvent;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller for the admin home screen.
 * <p>
 * This class implements the "control" part of the MVC pattern for the
 * administrator home view. It subscribes to the Firestore {@code events}
 * collection, maintains the current display mode ({@link AdminHomeMode})
 * and search query, and applies admin-specific filtering (events vs photos
 * and text search) before forwarding the resulting list of {@link UserEvent}
 * instances to an {@link AdminHomeView}.
 * <p>
 */
public class AdminHomeController {

    private final AdminHomeView view;
    private final FirebaseFirestore db;

    private ListenerRegistration registration;

    private final List<UserEvent> allEvents = new ArrayList<>();
    private final List<UserEvent> allImageEvents = new ArrayList<>();
    private final Map<String, DocumentSnapshot> eventDocsById = new HashMap<>();

    private AdminHomeMode currentMode = AdminHomeMode.EVENTS;
    private String currentQuery = "";

    /**
     * Creates a new controller instance using the default Firestore database.
     *
     * @param view the admin home view to be updated by this controller
     */
    public AdminHomeController(@NonNull AdminHomeView view) {
        this(view, FirebaseFirestore.getInstance());
    }

    /**
     * Creates a new controller instance.
     * <p>
     * This constructor is primarily intended for testing where a mocked or
     * emulator-backed {@link FirebaseFirestore} can be supplied.
     *
     * @param view the admin home view to be updated
     * @param db   Firestore instance used to load events
     */
    public AdminHomeController(@NonNull AdminHomeView view,
                               @NonNull FirebaseFirestore db) {
        this.view = view;
        this.db = db;
    }

    /**
     * Starts listening for changes in the {@code events} collection and
     * immediately requests an initial snapshot.
     * <p>
     * The view will be notified through {@link AdminHomeView#showLoading(boolean)}
     * and {@link AdminHomeView#showEvents(List)} as data becomes available.
     */
    public void start() {
        if (registration != null) {
            return; // already started
        }

        view.showLoading(true);

        registration = db.collection("events")
                .orderBy("startTimeMillis", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        view.showLoading(false);
                        view.showError("Failed to load events: " + error.getMessage());
                        return;
                    }

                    allEvents.clear();
                    allImageEvents.clear();
                    eventDocsById.clear();

                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            UserEvent event = doc.toObject(UserEvent.class);
                            if (event == null) {
                                continue;
                            }
                            event.setId(doc.getId());
                            allEvents.add(event);
                            eventDocsById.put(event.getId(), doc);

                            String imageUrl = safe(doc.getString("imageUrl"));
                            String posterUrl = safe(doc.getString("posterUrl"));
                            if (!imageUrl.isEmpty() || !posterUrl.isEmpty()) {
                                allImageEvents.add(event);
                            }
                        }
                    }

                    view.showLoading(false);
                    applyFilterAndNotify();
                });
    }

    /**
     * Stops listening to Firestore updates, if currently active.
     * <p>
     * This should be called from the corresponding view's lifecycle
     * (e.g., {@code onDestroyView}) to avoid memory leaks.
     */
    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    /**
     * Updates the current display mode and reapplies filtering to the
     * underlying event lists. The view is notified both of the new mode
     * and the resulting filtered events.
     *
     * @param mode the new mode to activate
     */
    public void setMode(@NonNull AdminHomeMode mode) {
        if (currentMode == mode) {
            return;
        }
        currentMode = mode;
        // Reset query when mode changes to keep behavior simple and predictable
        currentQuery = "";
        applyFilterAndNotify();
        view.showMode(currentMode);
    }

    /**
     * Updates the current text query and reapplies filtering for the
     * active mode. The view is notified of the new filtered list.
     *
     * @param query search query text, may be null or empty
     */
    public void onSearchQueryChanged(@Nullable String query) {
        currentQuery = (query == null) ? "" : query;
        applyFilterAndNotify();
    }

    /**
     * Returns the current admin home mode.
     *
     * @return the active mode
     */
    @NonNull
    public AdminHomeMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Returns the current search query string.
     *
     * @return non-null query string (possibly empty)
     */
    @NonNull
    public String getCurrentQuery() {
        return currentQuery;
    }

    // Internal helper: apply current mode + query and notify view

    private void applyFilterAndNotify() {
        List<UserEvent> base =
                (currentMode == AdminHomeMode.EVENTS) ? allEvents : allImageEvents;

        if (base.isEmpty()) {
            view.showEvents(Collections.emptyList());
            return;
        }

        if (currentQuery.trim().isEmpty()) {
            view.showEvents(new ArrayList<>(base));
            return;
        }

        String q = currentQuery.trim().toLowerCase(Locale.getDefault());
        List<UserEvent> filtered = new ArrayList<>();

        for (UserEvent event : base) {
            DocumentSnapshot doc = eventDocsById.get(event.getId());
            if (matchesQuery(doc, q)) {
                filtered.add(event);
            }
        }

        view.showEvents(filtered);
    }

    private boolean matchesQuery(@Nullable DocumentSnapshot doc,
                                 @NonNull String query) {
        if (doc == null) {
            return false;
        }
        return contains(doc.getString("name"), query)
                || contains(doc.getString("location"), query)
                || contains(doc.getString("descr"), query)
                || contains(doc.getString("imageUrl"), query)
                || contains(doc.getString("posterUrl"), query);
    }

    private boolean contains(@Nullable String value, @NonNull String query) {
        return value != null
                && value.toLowerCase(Locale.getDefault()).contains(query);
    }

    private String safe(@Nullable String s) {
        return (s == null) ? "" : s;
    }
}
