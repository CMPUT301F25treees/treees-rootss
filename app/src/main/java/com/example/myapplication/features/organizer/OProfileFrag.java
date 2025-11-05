package com.example.myapplication.features.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.core.UserSession;
import com.example.myapplication.data.model.User;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class OProfileFrag extends Fragment {
    public OProfileFrag() {
        super(R.layout.fragment_o_profile);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton roleButton = view.findViewById(R.id.btnRole);
        View cardNotifications = view.findViewById(R.id.cardNotifications);
        TextView welcomeText = view.findViewById(R.id.tvWelcomeUser);

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
                    .navigate(R.id.navigation_organizer_notifications);
        });
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
}
