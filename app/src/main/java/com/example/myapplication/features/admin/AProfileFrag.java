package com.example.myapplication.features.admin;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.R;

/**
 * Admin Profile fragment serving as a simple entry point to admin-related sections.
 * Currently provides navigation to the admin notifications screen.
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

        cardNotifications.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.navigation_admin_notifications);
        });

    }
}
