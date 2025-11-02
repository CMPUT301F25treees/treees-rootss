package com.example.myapplication.features.user;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

public class UProfileFrag extends Fragment {
    public UProfileFrag() {
        super(R.layout.fragment_u_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton roleButton = view.findViewById(R.id.btnRole);

        roleButton.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            menu.getMenu().add("User");
            menu.getMenu().add("Organizer");
            menu.setOnMenuItemClickListener(item -> {
                roleButton.setText(item.getTitle());
                return true;
            });
            menu.show();
        });
    }
}
