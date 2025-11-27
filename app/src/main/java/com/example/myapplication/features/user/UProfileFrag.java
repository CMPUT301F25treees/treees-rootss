package com.example.myapplication.features.user;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.R;
import com.example.myapplication.MainActivity;
import com.example.myapplication.core.DeviceLoginStore;
import com.example.myapplication.core.UserSession;
import com.example.myapplication.data.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays the user profile screen.
 * <p>
 * From this screen the user can:
 * <ul>
 *     <li>Switch between User and Organizer roles</li>
 *     <li>Navigate to their notifications</li>
 *     <li>Navigate to their past events</li>
 *     <li>Navigate to edit their profile information</li>
 *     <li>Delete their profile and associated data</li>
 * </ul>
 * Admin role display is also supported for users with that role.
 */
public class UProfileFrag extends Fragment {

    /**
     * Firestore instance used for deleting user-related documents.
     */
    private FirebaseFirestore firestore;

    /**
     * Reference to the delete-profile card in the UI, used to enable/disable it
     * while a delete operation is in progress.
     */
    private View deleteProfileCard;

    /**
     * Flag indicating whether a profile deletion operation is currently in progress.
     */
    private boolean isDeleting = false;

    /**
     * Constructs the fragment and associates it with the {@code fragment_u_profile} layout.
     */
    public UProfileFrag() {
        super(R.layout.fragment_u_profile);
    }

    /**
     * Called when the fragment's view has been created.
     * <p>
     * Initializes the role switcher, navigation cards (notifications, past events,
     * edit profile), delete-profile behaviour, and welcome text based on the
     * currently logged-in user stored in {@link UserSession}.
     *
     * @param view               The fragment's root view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton roleButton = view.findViewById(R.id.btnRole);
        View cardPastEvents = view.findViewById(R.id.cardPastEvents);
        View cardNotifications = view.findViewById(R.id.cardNotifications);
        View cardEditInfo = view.findViewById(R.id.cardEditInfo);
        deleteProfileCard = view.findViewById(R.id.cardDeleteProfile);
        TextView welcomeText = view.findViewById(R.id.tvWelcomeUser);

        firestore = FirebaseFirestore.getInstance();

        UserSession session = UserSession.getInstance();
        User currentUser = session.getCurrentUser();

        if (currentUser != null && welcomeText != null && currentUser.getUsername() != null) {
            welcomeText.setText(String.format(Locale.getDefault(), "Welcome %s", currentUser.getUsername()));
        }

        roleButton.setText(formatRoleLabel(currentUser != null ? currentUser.getRole() : null));

        roleButton.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            menu.getMenu().add("User");
            menu.getMenu().add("Organizer");
            menu.setOnMenuItemClickListener(item -> {
                applyRoleSelection(item.getTitle().toString(), roleButton);
                return true;
            });
            menu.show();
        });

        cardNotifications.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.navigation_user_notifications);
        });

        cardPastEvents.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.navigation_user_past_events);
        });

        if (cardEditInfo != null) {
            cardEditInfo.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_user_profile_to_user_edit_profile));
        }

        if (deleteProfileCard != null) {
            deleteProfileCard.setOnClickListener(v -> confirmDeleteProfile());
        }
    }

    /**
     * Applies the selected role (User or Organizer) to the current session and
     * updates the UI and navigation to match the new role.
     *
     * @param roleLabel the label chosen from the role popup menu (e.g., "User", "Organizer")
     * @param roleButton the button whose text is updated to reflect the current role
     */
    private void applyRoleSelection(String roleLabel, MaterialButton roleButton) {
        String normalized = "organizer".equalsIgnoreCase(roleLabel) ? "organizer" : "user";
        roleButton.setText(formatRoleLabel(normalized));

        UserSession session = UserSession.getInstance();
        User user = session.getCurrentUser();
        if (user != null) {
            user.setRole(normalized);
            session.setCurrentUser(user);
        }

        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) {
                activity.refreshNavigationForRole();
                int destination = normalized.equals("organizer")
                        ? R.id.navigation_organizer_home
                        : R.id.navigation_user_home;
                activity.navigateToBottomDestination(destination);
            }
        }
    }

    /**
     * Converts an internal role identifier into a user-facing label.
     * <p>
     * Recognized roles include "user", "organizer", and "admin". Any other non-empty
     * string is capitalized for display.
     *
     * @param role internal role value, may be {@code null}
     * @return human-readable label for display on the role button
     */
    private String formatRoleLabel(String role) {
        if (role == null || role.trim().isEmpty()) {
            return "User";
        }
        if ("organizer".equalsIgnoreCase(role)) {
            return "Organizer";
        }
        if ("admin".equalsIgnoreCase(role)) {
            return "Admin";
        }
        String lower = role.toLowerCase(Locale.getDefault());
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /**
     * Prompts the user to confirm permanent profile deletion.
     * <p>
     * If a deletion is already in progress, this method returns without
     * showing a second dialog.
     */
    private void confirmDeleteProfile() {
        if (isDeleting) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_profile_title)
                .setMessage(R.string.delete_profile_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete_profile_confirm, (dialog, which) -> performDeleteProfile())
                .show();
    }

    /**
     * Starts the deletion workflow for the current user's profile.
     * <p>
     * This includes deleting all events owned by the user, removing the
     * Firestore user document, and finally deleting the FirebaseAuth user.
     * If no authenticated user is found, an error message is shown and
     * the workflow is aborted.
     */
    private void performDeleteProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            showToast(getString(R.string.delete_profile_auth_missing));
            return;
        }
        setDeleting(true);
        String uid = firebaseUser.getUid();
        deleteUserEvents(uid,
                () -> deleteUserDocument(uid, () -> deleteAuthUser(firebaseUser), this::handleDeleteFailure),
                this::handleDeleteFailure);
    }

    /**
     * Deletes every event document owned by the supplied UID before removing the user.
     * <p>
     * This method accounts for legacy field naming by attempting deletions using
     * both {@code organizerID} and {@code organizerId} fields.
     *
     * @param uid        the unique ID of the user whose events should be deleted
     * @param onComplete callback invoked when event deletions are complete
     * @param onFailure  error callback invoked if any query or deletion fails
     */
    private void deleteUserEvents(String uid, Runnable onComplete, OnFailureListener onFailure) {
        deleteEventsByField("organizerID", uid,
                () -> deleteEventsByField("organizerId", uid, onComplete, onFailure),
                onFailure);
    }

    /**
     * Queries the events collection by the provided field name and deletes
     * all matching documents.
     *
     * @param fieldName  the field used to identify ownership (e.g., "organizerID")
     * @param uid        the UID value to match in the given field
     * @param onComplete callback invoked when all deletions have completed
     * @param onFailure  error callback invoked if fetching or deletion fails
     */
    private void deleteEventsByField(String fieldName, String uid, Runnable onComplete, OnFailureListener onFailure) {
        firestore.collection("events")
                .whereEqualTo(fieldName, uid)
                .get()
                .addOnSuccessListener(snapshot -> handleEventDeletionResult(snapshot, onComplete, onFailure))
                .addOnFailureListener(onFailure);
    }

    /**
     * Handles the result of an events query by deleting all matching documents
     * and then advancing the deletion workflow.
     *
     * @param snapshot   query snapshot containing event documents to delete
     * @param onComplete callback invoked when all deletions have finished
     * @param onFailure  error callback invoked if any deletion fails
     */
    private void handleEventDeletionResult(QuerySnapshot snapshot, Runnable onComplete, OnFailureListener onFailure) {
        if (snapshot == null || snapshot.isEmpty()) {
            onComplete.run();
            return;
        }

        List<Task<Void>> deletions = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            deletions.add(doc.getReference().delete());
        }

        Tasks.whenAllComplete(deletions)
                .addOnSuccessListener(tasks -> onComplete.run())
                .addOnFailureListener(onFailure);
    }

    /**
     * Deletes the Firestore user document for the specified UID and invokes
     * the provided completion callback if successful.
     *
     * @param uid        the UID of the user document to delete
     * @param onComplete callback invoked after successful deletion
     * @param onFailure  error callback invoked if deletion fails
     */
    private void deleteUserDocument(String uid, Runnable onComplete, OnFailureListener onFailure) {
        firestore.collection("users")
                .document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> onComplete.run())
                .addOnFailureListener(onFailure);
    }

    /**
     * Deletes the FirebaseAuth user, clears local login state, and navigates
     * back to the welcome screen upon success.
     * <p>
     * If the deletion fails, {@link #handleDeleteFailure(Exception)} is invoked.
     *
     * @param firebaseUser the authenticated user object to delete
     */
    private void deleteAuthUser(FirebaseUser firebaseUser) {
        firebaseUser.delete()
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) {
                        return;
                    }
                    showToast(getString(R.string.delete_profile_success));
                    FirebaseAuth.getInstance().signOut();
                    DeviceLoginStore.markLoggedOut(requireContext());
                    UserSession.getInstance().setCurrentUser(null);
                    setDeleting(false);
                    navigateToWelcomeScreen();
                })
                .addOnFailureListener(this::handleDeleteFailure);
    }

    /**
     * Centralized error handler for the delete-profile workflow.
     * <p>
     * Re-enables the delete card and displays an error message in a toast.
     *
     * @param e the exception that occurred, may be {@code null}
     */
    private void handleDeleteFailure(Exception e) {
        if (!isAdded()) {
            return;
        }
        setDeleting(false);
        showToast(getString(R.string.delete_profile_failed, e != null ? e.getMessage() : ""));
    }

    /**
     * Toggles the delete-profile UI state to prevent duplicate submissions while
     * a delete operation is running.
     *
     * @param deleting {@code true} if a delete operation is in progress, {@code false} otherwise
     */
    private void setDeleting(boolean deleting) {
        isDeleting = deleting;
        if (deleteProfileCard != null) {
            deleteProfileCard.setEnabled(!deleting);
            deleteProfileCard.setAlpha(deleting ? 0.5f : 1f);
        }
    }

    /**
     * Displays a short toast if the fragment is currently attached to an activity.
     * <p>
     * If the fragment is detached, this method does nothing.
     *
     * @param message the message to display in the toast
     */
    private void showToast(String message) {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Clears the navigation back stack and redirects the user to the welcome screen
     * after their account has been removed.
     */
    private void navigateToWelcomeScreen() {
        if (!isAdded()) {
            return;
        }
        NavController navController = NavHostFragment.findNavController(this);
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(navController.getGraph().getId(), true)
                .build();
        navController.navigate(R.id.navigation_welcome, null, options);
    }
}
