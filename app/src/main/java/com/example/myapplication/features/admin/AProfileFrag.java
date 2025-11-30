package com.example.myapplication.features.admin;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.R;
import com.example.myapplication.MainActivity;

/**
 * Admin profile fragment that serves as the entry point for admin
 * options such as notifications and logout.
 * <p>
 * This fragment is a simple view that presents a set of cards leading
 * to other admin features. It relies on {@code MainActivity} to
 * perform the actual logout operation.
 * <p>
 */
public class AProfileFrag extends Fragment {

    /**
     * Default constructor inflating {@code R.layout.fragment_a_profile}.
     */
    public AProfileFrag() { super(R.layout.fragment_a_profile); }

    /**
     * Called after the view has been created; wires UI interactions.
     * Sets a click listener on the notifications card to navigate to
     * {@code R.id.navigation_admin_notifications}.
     *
     * @param view               the fragment's root view
     * @param savedInstanceState previously saved state, if any
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View cardNotifications = view.findViewById(R.id.cardNotifications);
        View cardLogout = view.findViewById(R.id.cardLogout);

        cardNotifications.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.navigation_admin_notifications);
        });

        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).performLogout();
                }
            });
        }

    }
}
