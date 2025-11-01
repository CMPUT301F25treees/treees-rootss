package com.example.myapplication.features.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

public class WelcomeFrag extends Fragment {
    public WelcomeFrag() {}

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        return i.inflate(R.layout.fragment_auth_welcome, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        MaterialButton btnLogin = v.findViewById(R.id.btnLogin);
        MaterialButton btnRegister = v.findViewById(R.id.btnRegister);
        btnLogin.setOnClickListener(x ->
                NavHostFragment.findNavController(this).navigate(R.id.navigation_login));
        btnRegister.setOnClickListener(x ->
                NavHostFragment.findNavController(this).navigate(R.id.navigation_register));
    }
}
