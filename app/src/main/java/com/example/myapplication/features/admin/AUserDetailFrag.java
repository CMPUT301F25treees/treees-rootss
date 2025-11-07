package com.example.myapplication.features.admin;

import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;

public class AUserDetailFrag extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        return i.inflate(R.layout.fragment_a_user_detail, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        String name  = getStringArg("name");
        String email = getStringArg("email");
        String role  = getStringArg("role");
        String avatar= getStringArg("avatarUrl");

        ImageView iv = v.findViewById(R.id.ivAvatar);
        TextView tvN = v.findViewById(R.id.tvName);
        TextView tvE = v.findViewById(R.id.tvEmail);
        TextView tvR = v.findViewById(R.id.tvRole);
        MaterialButton back = v.findViewById(R.id.btnBack);

        tvN.setText(name);
        tvE.setText(email);
        tvR.setText(role);
        if (avatar != null && !avatar.isEmpty()) Glide.with(iv).load(avatar).into(iv);

        back.setOnClickListener(x -> NavHostFragment.findNavController(this).navigateUp());
    }

    private String getStringArg(String key){
        Bundle a = getArguments();
        return a == null ? "" : a.getString(key, "");
    }
}
