package com.example.myapplication.features.admin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseUserRepository;
import com.example.myapplication.features.profile.DeleteProfileController;
import com.example.myapplication.features.profile.DeleteProfileView;
import com.google.android.material.button.MaterialButton;

/**
 * Admin user detail fragment that displays profile information for a
 * single user and exposes a delete action.
 * <p>
 * This fragment implements {@link com.example.myapplication.features.profile.DeleteProfileView}
 * and delegates profile deletion to {@link com.example.myapplication.features.profile.DeleteProfileController},
 * which performs the actual model updates (user document and events).
 * <p>
 */
public class AUserDetailFrag extends Fragment implements DeleteProfileView {

    /**
     *  The user's profile data.
     */
    private String uid, name, email, role, avatarUrl;

    private DeleteProfileController controller;

    /**
     * Inflates the admin profile detail layout.
     *
     * @param i the {@link LayoutInflater}
     * @param c the optional parent container
     * @param b previously saved state, or {@code null}
     * @return the inflated root view
     */
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        return i.inflate(R.layout.fragment_a_user_detail, c, false);
    }

    /**
     * Binds argument values to UI and wires the Back and Delete actions.
     *
     * @param v the root view returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param b previously saved state, or {@code null}
     */
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        Bundle args = getArguments() != null ? getArguments() : Bundle.EMPTY;
        uid       = args.getString("uid", "");
        name      = args.getString("name", "");
        email     = args.getString("email", "");
        role      = args.getString("role", "");
        avatarUrl = args.getString("avatarUrl", "");

        controller = new DeleteProfileController(this, new FirebaseUserRepository());

        ImageView iv = v.findViewById(R.id.ivAvatar);
        TextView tvN = v.findViewById(R.id.tvName);
        TextView tvE = v.findViewById(R.id.tvEmail);
        TextView tvR = v.findViewById(R.id.tvRole);
        MaterialButton back = v.findViewById(R.id.btnBack);
        MaterialButton delete = v.findViewById(R.id.btnDeleteProfile);

        tvN.setText(name);
        tvE.setText(email);
        tvR.setText(role);
        if (!avatarUrl.isEmpty()) Glide.with(iv).load(avatarUrl).into(iv);

        back.setOnClickListener(x -> NavHostFragment.findNavController(this).navigateUp());
        delete.setOnClickListener(x -> controller.onDeleteProfileClicked());
    }

    @Override
    public void showConfirmationDialog() {
        if (!isAdded()) {
            return;
        }

        String who = !name.isEmpty() ? name : email;
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete profile?")
                .setMessage("This will remove " + who + " from the app. "
                        + "It deletes their profile and all events they created, "
                        + "and prevents them from using this app again.")
                .setPositiveButton("Delete", (d, w) -> controller.onAdminDeleteConfirmed(uid, role))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void showProgress(boolean show) {
    }

    @Override
    public void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void navigateOnSuccess() {
        NavHostFragment.findNavController(this).navigateUp();
    }
}
