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
 * Admin Profile Detail fragment.
 * <p>
 * Displays a read-only view of a user's profile (name, email, role, avatar) with
 * administrative controls to navigate back or delete the user's profile document.
 * If the user is an organizer, related events can be flagged as disabled prior to deletion.
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

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_profile);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );
        }

        TextView titleView = dialog.findViewById(R.id.dialogTitle);
        TextView messageView = dialog.findViewById(R.id.dialogMessage);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnDelete = dialog.findViewById(R.id.btnDelete);

        if (titleView != null) {
            titleView.setText("Delete profile?");
        }

        if (messageView != null) {
            messageView.setText(
                    "This will remove " + who + " from the app. " +
                            "It deletes their profile document in Firestore. " +
                            "It does not remove their sign-in account."
            );
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                controller.onAdminDeleteConfirmed(uid, role);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    @Override
    public void showProgress(boolean show) {
        // Optionally show a progress indicator or disable buttons
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
