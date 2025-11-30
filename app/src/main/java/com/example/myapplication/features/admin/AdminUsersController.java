package com.example.myapplication.features.admin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller for the admin "Browse Profiles" screen.
 * <p>
 * This class coordinates loading user documents from the Firestore
 * {@code users} collection, mapping them into {@link AdminUserAdapter.UserRow}
 * instances, applying simple text-based filtering, and handling admin actions
 * such as organizer demotion. Results and status updates are forwarded to
 * an {@link AdminUsersView}.
 * <p>
 */
public class AdminUsersController {

    private final AdminUsersView view;
    private final FirebaseFirestore db;

    private ListenerRegistration registration;
    private final List<AdminUserAdapter.UserRow> allUsers = new ArrayList<>();
    private String currentQuery = "";

    /**
     * Creates a new controller using the default Firestore database.
     *
     * @param view the view instance that will render user rows
     */
    public AdminUsersController(@NonNull AdminUsersView view) {
        this(view, FirebaseFirestore.getInstance());
    }

    /**
     * Creates a new controller instance with a specific Firestore instance.
     * <p>
     * This overload is primarily intended for unit testing with a mocked or
     * emulator-backed database.
     *
     * @param view the view instance to notify
     * @param db   Firestore reference used to query the users collection
     */
    public AdminUsersController(@NonNull AdminUsersView view,
                                @NonNull FirebaseFirestore db) {
        this.view = view;
        this.db = db;
    }

    /**
     * Starts listening for changes in the {@code users} collection and
     * updates the view whenever the underlying data changes.
     * <p>
     * Only non-admin users (roles "User" and "Organizer") are exposed
     * to the view.
     */
    public void start() {
        if (registration != null) {
            return; // already started
        }

        view.showLoading(true);

        registration = db.collection("users")
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        view.showLoading(false);
                        view.showMessage("Failed to load users: " + error.getMessage());
                        return;
                    }

                    allUsers.clear();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            AdminUserAdapter.UserRow row = toUserRow(doc);
                            if (row != null) {
                                allUsers.add(row);
                            }
                        }
                    }

                    view.showLoading(false);
                    applyFilterAndNotify();
                });
    }

    /**
     * Stops listening for user updates. Should be called from the view's
     * lifecycle (e.g., {@code onDestroyView}) to avoid leaks.
     */
    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    /**
     * Updates the current search query for the user list and reapplies
     * text filtering over names, emails, and roles.
     *
     * @param query search query text, may be null or empty
     */
    public void onSearchQueryChanged(@Nullable String query) {
        currentQuery = (query == null) ? "" : query;
        applyFilterAndNotify();
    }

    /**
     * Handles an admin request to demote an organizer to a regular user.
     * <p>
     * This method updates the corresponding {@code users/{uid}} document
     * and sets the {@code role} field to "User" and {@code suspended} to
     * {@code true}. The view is notified on success or failure.
     *
     * @param userRow row representing the user to be demoted
     */
    public void onDemoteOrganizerRequested(@NonNull AdminUserAdapter.UserRow userRow) {
        String uid = userRow.id;
        if (uid == null || uid.isEmpty()) {
            view.showMessage("Invalid user id.");
            return;
        }

        view.showLoading(true);

        db.collection("users")
                .document(uid)
                .update("role", "User", "suspended", true)
                .addOnSuccessListener(aVoid -> {
                    view.showLoading(false);
                    view.showMessage("Organizer demoted and suspended.");
                })
                .addOnFailureListener(onError("Failed to demote organizer"));
    }

    private OnFailureListener onError(@NonNull String prefix) {
        return e -> {
            view.showLoading(false);
            String message = prefix;
            if (e != null && e.getMessage() != null) {
                message += ": " + e.getMessage();
            }
            view.showMessage(message);
        };
    }


    @Nullable
    private AdminUserAdapter.UserRow toUserRow(@NonNull DocumentSnapshot doc) {
        String role = safe(doc.getString("role"));
        if (role.isEmpty()) {
            return null;
        }
        String lowerRole = role.toLowerCase(Locale.getDefault());

        if (!"user".equals(lowerRole) && !"organizer".equals(lowerRole)) {
            return null;
        }

        String id = doc.getId();
        String first = safe(doc.getString("firstName"));
        String last = safe(doc.getString("lastName"));
        String displayName;

        if (!first.isEmpty() || !last.isEmpty()) {
            displayName = (first + " " + last).trim();
        } else {
            displayName = safe(doc.getString("name"));
        }

        String email = safe(doc.getString("email"));
        String avatarUrl = safe(doc.getString("avatarUrl"));
        if (avatarUrl.isEmpty()) {
            avatarUrl = safe(doc.getString("photoUrl"));
        }

        AdminUserAdapter.UserRow row = new AdminUserAdapter.UserRow();
        row.id = id;
        row.name = displayName;
        row.email = email;
        row.role = role;
        row.avatarUrl = avatarUrl;

        return row;
    }

    private void applyFilterAndNotify() {
        List<AdminUserAdapter.UserRow> toShow = new ArrayList<>();

        if (currentQuery.trim().isEmpty()) {
            toShow.addAll(allUsers);
        } else {
            String q = currentQuery.trim().toLowerCase(Locale.getDefault());
            for (AdminUserAdapter.UserRow row : allUsers) {
                if (matchesQuery(row, q)) {
                    toShow.add(row);
                }
            }
        }

        view.showUsers(toShow);
        view.showEmptyState(toShow.isEmpty());
    }

    private boolean matchesQuery(@NonNull AdminUserAdapter.UserRow row,
                                 @NonNull String query) {
        return contains(row.name, query)
                || contains(row.email, query)
                || contains(row.role, query);
    }

    private boolean contains(@Nullable String value, @NonNull String query) {
        return value != null
                && value.toLowerCase(Locale.getDefault()).contains(query);
    }

    private String safe(@Nullable String s) {
        return (s == null) ? "" : s;
    }
}
