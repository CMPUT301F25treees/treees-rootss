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
import com.example.myapplication.data.firebase.FirebaseUserRepository;
import com.example.myapplication.data.model.User;
import com.example.myapplication.features.profile.DeleteProfileController;
import com.example.myapplication.features.profile.DeleteProfileView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

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
public class UProfileFrag extends Fragment implements DeleteProfileView {

    /**
     * Reference to the delete-profile card in the UI, used to enable/disable it
     * while a delete operation is in progress.
     */
    private View deleteProfileCard;

    /**
     * Flag indicating whether a profile deletion operation is currently in progress.
     */
    private boolean isDeleting = false;

    private DeleteProfileController controller;

    /**
     * Constructs the fragment and associates it with the {@code fragment_u_profile} layout.
     */
    public UProfileFrag() {
        super(R.layout.fragment_u_profile);
    }

    /**
     * Called when the fragment's view has been created.
     * <p>
     * Initializes the role switcher, navigation cards (notifications, waitlists, past events,
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
        View cardWaitlist = view.findViewById(R.id.cardWaitingList);
        View cardNotifications = view.findViewById(R.id.cardNotifications);
        View cardEditInfo = view.findViewById(R.id.cardEditInfo);
        View cardLogout = view.findViewById(R.id.cardLogout);
        deleteProfileCard = view.findViewById(R.id.cardDeleteProfile);
        TextView welcomeText = view.findViewById(R.id.tvWelcomeUser);

        controller = new DeleteProfileController(this, new FirebaseUserRepository());

        UserSession session = UserSession.getInstance();
        User currentUser = session.getCurrentUser();

        if (currentUser != null && welcomeText != null && currentUser.getUsername() != null) {
            welcomeText.setText(String.format(Locale.getDefault(), "Welcome %s", currentUser.getUsername()));
        }

        roleButton.setText(formatRoleLabel(currentUser != null ? currentUser.getRole() : null));

        roleButton.setOnClickListener(v -> roleSwitchDialog(roleButton));

        cardNotifications.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.navigation_user_notifications);
        });

        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).performLogout();
                }
            });
        }

        cardPastEvents.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.navigation_user_past_events);
        });

        if(cardWaitlist != null){
            cardWaitlist.setOnClickListener(v->{
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_user_profile_to_waitlists);
            });
        }

        if (cardEditInfo != null) {
            cardEditInfo.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_user_profile_to_user_edit_profile));
        }

        if (deleteProfileCard != null) {
            deleteProfileCard.setOnClickListener(v -> controller.onDeleteProfileClicked());
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
     * Displays the custom-made role switch pop-up dialogue. This lets the users choose between
     * 'User' and 'Organizer.'
     *
     * @param roleButton button on the profile screen that displays the current users role
     */
    private void roleSwitchDialog(MaterialButton roleButton) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_role_switch, null);

        View btnUser = dialogView.findViewById(R.id.btnUser);
        View btnOrganizer = dialogView.findViewById(R.id.btnOrganizer);

        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(requireContext());
        builder.setView(dialogView);

        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        if(dialog.getWindow() != null){
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnUser.setOnClickListener(v -> {
            applyRoleSelection("User", roleButton);
            dialog.dismiss();
        });

        btnOrganizer.setOnClickListener(v -> {
            applyRoleSelection("Organizer", roleButton);
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void showConfirmationDialog() {
        if (isDeleting) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_profile_title)
                .setMessage(R.string.delete_profile_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete_profile_confirm, (dialog, which) -> controller.onDeleteConfirmed())
                .show();
    }

    @Override
    public void showProgress(boolean show) {
        isDeleting = show;
        if (deleteProfileCard != null) {
            deleteProfileCard.setEnabled(!show);
            deleteProfileCard.setAlpha(show ? 0.5f : 1f);
        }
    }

    @Override
    public void showToast(String message) {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void navigateOnSuccess() {
        if (!isAdded()) {
            return;
        }
        // Cleanup session state
        FirebaseAuth.getInstance().signOut();
        DeviceLoginStore.markLoggedOut(requireContext());
        UserSession.getInstance().setCurrentUser(null);
        
        NavController navController = NavHostFragment.findNavController(this);
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(navController.getGraph().getId(), true)
                .build();
        navController.navigate(R.id.navigation_welcome, null, options);
    }
}
