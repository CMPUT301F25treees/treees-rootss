package com.example.myapplication.features.user;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UEditProfileFrag extends Fragment {

    private TextInputEditText inputFirstName, inputLastName, inputEmail, inputPhone;
    private TextInputLayout tilFirstName, tilEmail;
    private View progressView;
    private MaterialButton saveButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    public UEditProfileFrag() {
        super(R.layout.fragment_u_edit_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews(view);
        populateFields();
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

        saveButton.setOnClickListener(v -> attemptSave());

        View.OnClickListener backListener = v -> NavHostFragment.findNavController(this).popBackStack();
        backButton.setOnClickListener(backListener);
        goBackButton.setOnClickListener(backListener);
    }

    private void populateFields() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;

        setLoading(true);
        db.collection("users").document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    setLoading(false);
                    if (snapshot.exists()) {
                        String firstName = snapshot.getString("firstName");
                        if (TextUtils.isEmpty(firstName)) firstName = snapshot.getString("username");

                        inputFirstName.setText(firstName);
                        inputLastName.setText(snapshot.getString("lastName"));
                        inputPhone.setText(snapshot.getString("cell"));
                        // Always display the actual Auth email
                        inputEmail.setText(firebaseUser.getEmail());
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Failed to load profile");
                });
    }

    private void attemptSave() {
        tilFirstName.setError(null);
        tilEmail.setError(null);

        String firstName = textOf(inputFirstName);
        String lastName = textOf(inputLastName);
        String email = textOf(inputEmail);
        String phone = textOf(inputPhone);

        if (TextUtils.isEmpty(firstName)) {
            tilFirstName.setError(getString(R.string.edit_profile_first_name_required));
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.edit_profile_email_invalid));
            return;
        }

        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;

        setLoading(true);

        boolean emailChanged = !Objects.equals(firebaseUser.getEmail(), email);

        // 1. Batch Update: Save Name/Phone to Firestore first
        WriteBatch batch = db.batch();
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("username", firstName);
        updates.put("lastName", TextUtils.isEmpty(lastName) ? null : lastName);
        updates.put("cell", TextUtils.isEmpty(phone) ? null : phone);

        if (!emailChanged) {
            updates.put("email", email);
        }

        batch.set(db.collection("users").document(firebaseUser.getUid()), updates, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(v -> {
                    if (emailChanged) {
                        // 2. Email Change Flow (Attempt Direct Update)
                        handleEmailChange(firebaseUser, email, firstName);
                    } else {
                        updateLocalSession(firstName, email);
                        setLoading(false);
                        toast("Profile updated");
                        NavHostFragment.findNavController(this).popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Update failed: " + e.getMessage());
                });
    }

    private void handleEmailChange(FirebaseUser user, String newEmail, String firstName) {
        // DIRECT UPDATE (No verification email)
        user.updateEmail(newEmail)
                .addOnSuccessListener(v -> {
                    // Success! Sync Firestore.
                    db.collection("users").document(user.getUid()).update("email", newEmail);
                    updateLocalSession(firstName, newEmail);
                    setLoading(false);
                    toast("Email updated successfully");
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                        // Security Check: Prompt for Password
                        showReauthDialog(user, newEmail, firstName);
                    } else {
                        // This catches "Operation Not Allowed" or "Email Already in Use"
                        toast("Update Error: " + e.getMessage());
                    }
                });
    }

    private void showReauthDialog(FirebaseUser user, String newEmail, String firstName) {
        if (getContext() == null) return;

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Current Password");
        // Add padding to make it look nicer
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding/2, padding, padding/2);

        new AlertDialog.Builder(getContext())
                .setTitle("Security Check")
                .setMessage("To change your email, please confirm your current password.")
                .setView(input)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String password = input.getText().toString();
                    if (!TextUtils.isEmpty(password)) {
                        performReauth(user, password, newEmail, firstName);
                    } else {
                        toast("Password cannot be empty");
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void performReauth(FirebaseUser user, String password, String newEmail, String firstName) {
        setLoading(true);
        // Important: Use the OLD email to re-authenticate
        AuthCredential credential = EmailAuthProvider.getCredential(Objects.requireNonNull(user.getEmail()), password);

        user.reauthenticate(credential)
                .addOnSuccessListener(v -> {
                    // Re-auth successful, retry the update
                    handleEmailChange(user, newEmail, firstName);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Incorrect password or login type");
                });
    }

    private void updateLocalSession(String firstName, String email) {
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

    private void setLoading(boolean loading) {
        if (progressView != null) progressView.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (saveButton != null) saveButton.setEnabled(!loading);
    }

    private String textOf(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }

    private void toast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}