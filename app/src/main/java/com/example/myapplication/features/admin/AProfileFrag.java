package com.example.myapplication.features.admin;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.R;

public class AProfileFrag extends Fragment {
    public AProfileFrag() { super(R.layout.fragment_a_profile); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View cardNotifications = view.findViewById(R.id.cardNotifications);

        //Open Notifications Fragment
        cardNotifications.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.navigation_user_notifications);
        });

    }
}
