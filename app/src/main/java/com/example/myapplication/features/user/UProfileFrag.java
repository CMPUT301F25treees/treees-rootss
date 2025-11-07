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

public class UProfileFrag extends Fragment {

    private FirebaseFirestore firestore;
    private View deleteProfileCard;
    private boolean isDeleting = false;

    public UProfileFrag() {
        super(R.layout.fragment_u_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton roleButton = view.findViewById(R.id.btnRole);
        View cardNotifications = view.findViewById(R.id.cardNotifications);
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

        if (deleteProfileCard != null) {
            deleteProfileCard.setOnClickListener(v -> confirmDeleteProfile());
        }
    }

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
     * Starts the cascade that removes events, Firestore user data, and the auth user.
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
     */
    private void deleteUserEvents(String uid, Runnable onComplete, OnFailureListener onFailure) {
        deleteEventsByField("organizerID", uid,
                () -> deleteEventsByField("organizerId", uid, onComplete, onFailure),
                onFailure);
    }

    /**
     * Queries by the provided organizer field (legacy naming support) and deletes matching docs.
     */
    private void deleteEventsByField(String fieldName, String uid, Runnable onComplete, OnFailureListener onFailure) {
        firestore.collection("events")
                .whereEqualTo(fieldName, uid)
                .get()
                .addOnSuccessListener(snapshot -> handleEventDeletionResult(snapshot, onComplete, onFailure))
                .addOnFailureListener(onFailure);
    }

    /**
     * Handles batch deletion completion for event snapshots, then advances the workflow.
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
     * Deletes the Firestore user document and signals when the next step can execute.
     */
    private void deleteUserDocument(String uid, Runnable onComplete, OnFailureListener onFailure) {
        firestore.collection("users")
                .document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> onComplete.run())
                .addOnFailureListener(onFailure);
    }

    /**
     * Deletes the FirebaseAuth user, signs out locally, and returns to the welcome screen.
     */
    private void deleteAuthUser(FirebaseUser firebaseUser) {
        firebaseUser.delete()
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) {
                        return;
                    }
                    showToast(getString(R.string.delete_profile_success));
                    FirebaseAuth.getInstance().signOut();
                    UserSession.getInstance().setCurrentUser(null);
                    setDeleting(false);
                    navigateToWelcomeScreen();
                })
                .addOnFailureListener(this::handleDeleteFailure);
    }

    /**
     * Centralized error handler that re-enables UI and surfaces the message.
     */
    private void handleDeleteFailure(Exception e) {
        if (!isAdded()) {
            return;
        }
        setDeleting(false);
        showToast(getString(R.string.delete_profile_failed, e != null ? e.getMessage() : ""));
    }

    /**
     * Toggles the delete card enabled state to prevent duplicate submissions.
     */
    private void setDeleting(boolean deleting) {
        isDeleting = deleting;
        if (deleteProfileCard != null) {
            deleteProfileCard.setEnabled(!deleting);
            deleteProfileCard.setAlpha(deleting ? 0.5f : 1f);
        }
    }

    /**
     * Safe toast helper that no-ops when the fragment is detached.
     */
    private void showToast(String message) {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Clears the back stack and navigates to the welcome screen after account removal.
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
