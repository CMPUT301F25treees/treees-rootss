package com.example.myapplication.features.organizer;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
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
 * This class is for the organizers profile screen.
 *
 * On this screen users can view and send notifications, edit their info (still to be
 * implemented), switch between User and Organizer, and delete their profile.
 */
public class OProfileFrag extends Fragment implements DeleteProfileView {

    private View deleteProfileCard;
    private boolean isDeleting = false;
    private DeleteProfileController controller;

    /**
     * Constructor for OProfileFrag.
     */
    public OProfileFrag() {
        super(R.layout.fragment_o_profile);
    }

    /**
     * Configures the role selection, navigation cards, and delete profile workflow.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton roleButton = view.findViewById(R.id.btnRole);
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
                    .navigate(R.id.navigation_organizer_notifications);
        });

        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).performLogout();
                }
            });
        }

        if (cardEditInfo != null) {
            cardEditInfo.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            // Organizer edits use the same flow as users
                            .navigate(R.id.navigation_u_edit_profile));
        }

        if (deleteProfileCard != null) {
            deleteProfileCard.setOnClickListener(v -> controller.onDeleteProfileClicked());
        }
    }

    /**
     * Updates the role button label, user session, and main activity navigation.
     * @param roleLabel The selected role label.
     * @param roleButton The button to update the label on.
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
     * Formats the role label for display.
     * @param role The role string to format.
     * @return A user-friendly role label.
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

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_profile);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView titleView = dialog.findViewById(R.id.dialogTitle);
        TextView messageView = dialog.findViewById(R.id.dialogMessage);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnDelete = dialog.findViewById(R.id.btnDelete);

        // Use your string resources if you want consistency
        if (titleView != null) {
            titleView.setText(getString(R.string.delete_profile_title));
        }
        if (messageView != null) {
            messageView.setText(getString(R.string.delete_profile_message));
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                controller.onDeleteConfirmed();
                dialog.dismiss();
            });
        }

        dialog.show();
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
