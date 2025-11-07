package com.example.myapplication.features.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * This activity is responsible for registering new users.
 *
 * Collects a users personal information: First Name, Last Name, Email, Phone, and Password
 *
 * Current Functionality: Validates the users inputs, creates a user account, and submits
 * user information to FireStore database.
 */

public class RegisterFrag extends Fragment {

    private TextInputEditText inputFirstName, inputLastName, inputEmail, inputPhone, inputPassword;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    /**
     * This method gets called when the activity is created. Initializes the input fields and
     * register button listener.
     *
     * FirebaseAuth and FireStore get initialized.
     *
     *
     * @param savedInstanceState if there is any previous state of this activity
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth_registration, container, false);
    }

    /**
     * Inflates the layout for the registration.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inputFirstName = view.findViewById(R.id.first_name_input);
        inputLastName = view.findViewById(R.id.last_name_input);
        inputEmail = view.findViewById(R.id.email_input);
        inputPhone = view.findViewById(R.id.phone_input);
        inputPassword = view.findViewById(R.id.password_input);

        MaterialButton registerButton = view.findViewById(R.id.registerButton);
        registerButton.setOnClickListener(v -> handleRegister());
    }

    /**
     * This method is called when the user clicks the register button.
     * User entries will first get validated, make sure that the required fields are filled.
     * User is then created with FirebaseAuth and then user information is added to FireStore database.
     */
    private void handleRegister(){
        String firstName = inputFirstName.getText().toString().trim();
        String lastName = inputLastName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String phone = inputPhone.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        
        if (firstName.isEmpty()){
            Toast.makeText(requireContext(), "First Name is Required", Toast.LENGTH_SHORT).show();
        } else if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Email is Required", Toast.LENGTH_SHORT).show();
        } else if (password.isEmpty()) {
            Toast.makeText(requireContext(), "Password is Required", Toast.LENGTH_SHORT).show();
        } else if (password.length() < 6) {
            Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
        } else {
            // Show progress indication
            Toast.makeText(requireContext(), "Registering... Please wait", Toast.LENGTH_SHORT).show();

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firbaseUser = auth.getCurrentUser();
                            String uid = firbaseUser.getUid();

                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("firstName", firstName);
                            userMap.put("lastName", lastName.isEmpty() ? null : lastName);
                            userMap.put("email", email);
                            userMap.put("cell", phone.isEmpty() ? null : phone);
                            userMap.put("role", "User");
                            userMap.put("wantNoti", true);


                            db.collection("users").document(uid)
                                    .set(userMap)
                                    .addOnSuccessListener(x -> {
                                        Toast.makeText(requireContext(), "Registration Successful", Toast.LENGTH_SHORT).show();
                                        NavHostFragment.findNavController(this).navigate(R.id.navigation_login);
                                    })
                                    .addOnFailureListener( e -> {
                                        Toast.makeText(requireContext(), "Error saving user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                     });
                        } else {
                            String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";

                            // Provide specific guidance for network errors
                            if (errorMessage.contains("network error") || errorMessage.contains("timeout") ||
                                errorMessage.contains("unreachable")) {
                                Toast.makeText(requireContext(),
                                    "Network error: Please check your internet connection and try again. If using VPN, try disabling it.",
                                    Toast.LENGTH_LONG).show();
                            } else if (errorMessage.contains("email address is already in use")) {
                                Toast.makeText(requireContext(),
                                    "This email is already registered. Try logging in instead.",
                                    Toast.LENGTH_LONG).show();
                            } else if (errorMessage.contains("weak-password")) {
                                Toast.makeText(requireContext(),
                                    "Password is too weak. Use at least 6 characters.",
                                    Toast.LENGTH_LONG).show();
                            } else if (errorMessage.contains("invalid-email")) {
                                Toast.makeText(requireContext(),
                                    "Invalid email format. Please check your email address.",
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(requireContext(),
                                    "Registration failed: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }

    }
}
