package com.example.myapplication.features.user.profile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.R;
import com.example.myapplication.core.DeviceLoginStore;
import com.example.myapplication.core.UserSession;
import com.example.myapplication.data.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class UEditProfileFrag extends Fragment implements ProfileView {
    private TextInputEditText inputFirstName, inputLastName, inputEmail, inputPhone;
    private TextInputLayout tilFirstName, tilEmail;
    private View progressView;
    private MaterialButton saveButton;
    private ProfileController controller;

    private FirebaseAuth testAuth;
    private FirebaseFirestore testDb;

    public UEditProfileFrag() {
        super(R.layout.fragment_u_edit_profile);
    }

    public void setAuth(FirebaseAuth auth) {
        this.testAuth = auth;
    }

    public void setDb(FirebaseFirestore db) {
        this.testDb = db;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (controller == null) {
            FirebaseAuth auth = testAuth != null ? testAuth : FirebaseAuth.getInstance();
            FirebaseFirestore db = testDb != null ? testDb : FirebaseFirestore.getInstance();
            ProfileModel model = new ProfileModel(db);
            controller = new ProfileController(this, model, auth);
        }
        initViews(view);
        controller.loadProfile();
    }

    private void initViews(View view) {
        inputFirstName = view.findViewById(R.id.edit_first_name_input);
        inputLastName = view.findViewById(R.id.edit_last_name_input);
        inputEmail = view.findViewById(R.id.edit_email_input);
        inputPhone = view.findViewById(R.id.edit_phone_input);
        tilFirstName = view.findViewById(R.id.tilEditFirstName);
        tilEmail = view.findViewById(R.id.tilEditEmail);
        progressView = view.findViewById(R.id.editProfileProgress);
        saveButton = view.findViewById(R.id.btnSaveProfile);
        MaterialButton backButton = view.findViewById(R.id.btnBackProfile);
        ImageButton goBackButton = view.findViewById(R.id.bckButton);

        saveButton.setOnClickListener(v -> {
            // Clear previous errors
            tilFirstName.setError(null);
            tilEmail.setError(null);
            
            String firstName = textOf(inputFirstName);
            String lastName = textOf(inputLastName);
            String email = textOf(inputEmail);
            String phone = textOf(inputPhone);
            controller.saveProfile(firstName, lastName, email, phone);
        });

        View.OnClickListener backListener = v -> navigateBack();
        backButton.setOnClickListener(backListener);
        goBackButton.setOnClickListener(backListener);
    }

    private String textOf(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }

    @Override
    public void showLoading(boolean isLoading) {
        if (progressView != null) {
            progressView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (saveButton != null) {
            saveButton.setEnabled(!isLoading);
        }
    }

    @Override
    public void showProfileData(String firstName, String lastName, String email, String phone) {
        if (inputFirstName != null) inputFirstName.setText(firstName);
        if (inputLastName != null) inputLastName.setText(lastName);
        if (inputEmail != null) inputEmail.setText(email);
        if (inputPhone != null) inputPhone.setText(phone);
    }

    @Override
    public void showFirstNameError(String error) {
        if (tilFirstName != null) tilFirstName.setError(error);
    }

    @Override
    public void showEmailError(String error) {
        if (tilEmail != null) tilEmail.setError(error);
    }

    @Override
    public void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void navigateBack() {
        NavHostFragment.findNavController(this).popBackStack();
    }

    @Override
    public void showPasswordDialog() {
        if (getContext() == null) return;
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_password_input, null);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.dialog_password_input);
        TextInputLayout passwordLayout = dialogView.findViewById(R.id.dialog_password_layout);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Confirm Password")
                .setMessage("For security, please enter your password to change your email")
                .setView(dialogView)
                .setPositiveButton("Confirm", null)
                .setNegativeButton("Cancel", (d, which) -> {
                    controller.onPasswordDialogCancelled();
                    d.dismiss();
                })
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String password = passwordInput.getText() != null ?
                        passwordInput.getText().toString().trim() : "";

                if (TextUtils.isEmpty(password)) {
                    passwordLayout.setError("Password required");
                    return;
                }

                passwordLayout.setError(null);
                dialog.dismiss();
                controller.onPasswordConfirmed(password);
            });
        });

        dialog.show();
    }

    @Override
    public void showReauthError(String error) {
        showToast(error);
    }

    @Override
    public void updateLocalSession(String firstName, String email) {
        User user = UserSession.getInstance().getCurrentUser();
        if (user != null) {
            user.setUsername(firstName);
            user.setEmail(email);
            UserSession.getInstance().setCurrentUser(user);
            if (getContext() != null) {
                DeviceLoginStore.rememberUser(getContext(), user);
            }
        }
    }
}