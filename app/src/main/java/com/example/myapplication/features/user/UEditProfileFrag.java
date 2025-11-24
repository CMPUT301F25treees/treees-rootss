package com.example.myapplication.features.user;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.R;
import com.example.myapplication.core.UserSession;
import com.example.myapplication.data.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Fragment that allows the user to edit their profile details.
 */
public class UEditProfileFrag extends Fragment {

    private TextInputEditText inputFirstName;
    private TextInputEditText inputLastName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPhone;
    private TextInputLayout tilFirstName;
    private TextInputLayout tilEmail;
    private View progressView;
    private MaterialButton saveButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    /**
     * Constructor for UEditProfileFrag.
     * @param None
     * @return void
     */
    public UEditProfileFrag() {
        super(R.layout.fragment_u_edit_profile);
    }

    /**
     * Initializes Firebase instances, input fields, and button listeners.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return void
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inputFirstName = view.findViewById(R.id.edit_first_name_input);
        inputLastName = view.findViewById(R.id.edit_last_name_input);
        inputEmail = view.findViewById(R.id.edit_email_input);
        inputPhone = view.findViewById(R.id.edit_phone_input);
        tilFirstName = view.findViewById(R.id.tilEditFirstName);
        tilEmail = view.findViewById(R.id.tilEditEmail);
        progressView = view.findViewById(R.id.editProfileProgress);

        saveButton = view.findViewById(R.id.btnSaveProfile);
        MaterialButton backButton = view.findViewById(R.id.btnBackProfile);

        backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        saveButton.setOnClickListener(v -> attemptSave());

        populateFields();
    }

    /**
     * Loads the current profile information from Firestore for display.
     */
    private void populateFields() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            toast(getString(R.string.edit_profile_auth_missing));
            return;
        }

        setLoading(true);
        db.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(this::applyDocument)
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast(getString(R.string.edit_profile_load_failed));
                });
    }

    /**
     * Applies the loaded document data to the input fields.
     *
     * @param snapshot The Firestore document snapshot containing user profile data.
     * @return void
     */
    private void applyDocument(DocumentSnapshot snapshot) {
        setLoading(false);
        if (snapshot == null || !snapshot.exists()) {
            User stored = UserSession.getInstance().getCurrentUser();
            inputFirstName.setText(stored != null ? stored.getUsername() : null);
            inputEmail.setText(stored != null ? stored.getEmail() : null);
            return;
        }

        String firstName = snapshot.getString("firstName");
        if (TextUtils.isEmpty(firstName)) {
            firstName = snapshot.getString("username");
        }
        inputFirstName.setText(firstName);
        inputLastName.setText(snapshot.getString("lastName"));

        String email = snapshot.getString("email");
        if (TextUtils.isEmpty(email)) {
            FirebaseUser firebaseUser = auth.getCurrentUser();
            email = firebaseUser != null ? firebaseUser.getEmail() : null;
        }
        inputEmail.setText(email);
        inputPhone.setText(snapshot.getString("cell"));
    }

    /**
     * Validates the inputs and starts the save flow.
     * @param None
     * @return void
     */
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
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.edit_profile_email_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.edit_profile_email_invalid));
            return;
        }

        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            toast(getString(R.string.edit_profile_auth_missing));
            return;
        }

        setLoading(true);

        boolean emailChanged = !Objects.equals(firebaseUser.getEmail(), email);

        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("firstName", firstName);
        profileUpdates.put("username", firstName);
        profileUpdates.put("lastName", TextUtils.isEmpty(lastName) ? null : lastName);
        profileUpdates.put("cell", TextUtils.isEmpty(phone) ? null : phone);
        if (!emailChanged) {
            profileUpdates.put("email", email);
        }

        db.collection("users")
                .document(firebaseUser.getUid())
                .set(profileUpdates, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    if (emailChanged) {
                        updateEmailAndPersist(firebaseUser, firstName, email);
                    } else {
                        finishSave(firstName, email);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast(getString(R.string.edit_profile_save_failed, e.getMessage()));
                });
    }

    /**
     * Updates the user's email in Firebase Authentication and persists it in Firestore.
     *
     * @param firebaseUser The current Firebase user.
     * @param firstName The user's first name.
     * @param desiredEmail The new email address to set.
     * @return void
     */
    private void updateEmailAndPersist(FirebaseUser firebaseUser, String firstName, String desiredEmail) {
        firebaseUser.updateEmail(desiredEmail)
                .addOnSuccessListener(v -> persistEmailField(firstName, desiredEmail))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast(getString(R.string.edit_profile_save_failed, e.getMessage()));
                });
    }

    /**
     * Persists the updated email field in Firestore.
     *
     * @param firstName The user's first name.
     * @param email The new email address to set.
     * @return void
     */
    private void persistEmailField(String firstName, String email) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            setLoading(false);
            toast(getString(R.string.edit_profile_auth_missing));
            return;
        }

        Map<String, Object> emailUpdate = new HashMap<>();
        emailUpdate.put("email", email);

        db.collection("users")
                .document(firebaseUser.getUid())
                .set(emailUpdate, SetOptions.merge())
                .addOnSuccessListener(v -> finishSave(firstName, email))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast(getString(R.string.edit_profile_save_failed, e.getMessage()));
                });
    }

    /**
     * Finalizes the save operation by updating the session and notifying the user.
     *
     * @param firstName The user's first name.
     * @param email The user's email address.
     * @return void
     */
    private void finishSave(String firstName, String email) {
        setLoading(false);

        User user = UserSession.getInstance().getCurrentUser();
        if (user != null) {
            user.setUsername(firstName);
            user.setEmail(email);
            UserSession.getInstance().setCurrentUser(user);
        }

        toast(getString(R.string.edit_profile_save_success));
        NavHostFragment.findNavController(this).popBackStack();
    }

    /**
     * Sets the loading state of the UI.
     *
     * @param loading True to show loading state, false to hide.
     * @return void
     */
    private void setLoading(boolean loading) {
        progressView.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!loading);
    }

    /**
     * Retrieves trimmed text from a TextInputEditText.
     *
     * @param input The TextInputEditText to extract text from.
     * @return The trimmed text as a String.
     */
    private String textOf(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }

    /**
     * Displays a short Toast message.
     *
     * @param message The message to display.
     * @return void
     */
    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
