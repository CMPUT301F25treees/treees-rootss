package com.example.myapplication.features.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.*;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.myapplication.R;
import com.example.myapplication.data.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * This class handles user authentication using Firebase Auth.
 *
 * User credentials are first validated and then signed in. On successful sign in
 * the users role will be returned which will help dictate which screen the user gets
 * navigated to.
 */
public class LoginFrag extends Fragment implements AuthenticationController.AuthenticationCallback {

    private AuthenticationController controller;
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private ProgressBar progress;

    /**
     * Default constructor
     * @param: None
     * @return: void
     */
    public LoginFrag() {}

    /**
     * This method inflates the layout for the fragment.
     *
     * @param inf LayoutInflater object that can be used to inflate any views in the fragment
     * @param c If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param b If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_auth_login, c, false);
    }

    /**
     * This method is called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     *
     * Initializes AuthenticationController. Also initializes input fields
     * and login button listener.
     *
     * @param v The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param b If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        controller = new AuthenticationController(requireContext(), this);

        tilEmail = v.findViewById(R.id.tilEmail);
        tilPassword = v.findViewById(R.id.tilPassword);
        etEmail = v.findViewById(R.id.etEmail);
        etPassword = v.findViewById(R.id.etPassword);
        progress = v.findViewById(R.id.progress);
        MaterialButton btnLogin = v.findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(x -> signIn());
    }

    /**
     * This method displays teh loading circle, as authentication is happening.
     *
     * @param on if True circle is shown, if False circle is hidden.
     */
    private void setLoading(boolean on){ progress.setVisibility(on?View.VISIBLE:View.GONE); }

    /**
     * This method is what handles all the sign-in functionality.
     *
     * The user inputs get validated and then login is attempted. If successful
     * the users role is returned from Firestore and they get navigated to their
     * respective screen.
     */
    private void signIn() {
        String email = text(etEmail), pass = text(etPassword);

        tilEmail.setError(null);
        tilPassword.setError(null);

        // Basic UI validation
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { tilEmail.setError("Enter a valid email"); return; }
        if (TextUtils.isEmpty(pass)) { tilPassword.setError("Enter your password"); return; }

        controller.login(email, pass);
    }

    // AuthenticationCallback implementation

    @Override
    public void onLoginSuccess(User user) {
        navigateBasedOnRole(user);
    }

    @Override
    public void onLoginFailure(String message) {
        toast(message);
    }

    @Override
    public void onNoSavedUser() {
        // Not applicable for manual login
    }

    @Override
    public void onLoading(boolean isLoading) {
        setLoading(isLoading);
    }

    /**
     * Navigates the user to the appropriate home screen based on their role.
     *
     * @param user The user whose role determines the navigation destination.
     * @return: void
     */
    private void navigateBasedOnRole(User user) {
        int destination = R.id.navigation_user_home;
        if ("admin".equalsIgnoreCase(user.getRole())) {
            destination = R.id.navigation_admin_home;
        } else if ("organizer".equalsIgnoreCase(user.getRole())) {
            destination = R.id.navigation_organizer_home;
        }
        NavHostFragment.findNavController(this).navigate(destination);
    }

    /**
     * A helper method that gets the trimmed text from user input.
     *
     * @param e the input field
     * @return trimmed text or an empty string when null
     */
    private static String text(TextInputEditText e){ return e.getText()==null ? "" : e.getText().toString().trim(); }

    /**
     * Displays toast message.
     *
     * @param m message that gets displayed.
     */
    private void toast(String m){ Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show(); }
}