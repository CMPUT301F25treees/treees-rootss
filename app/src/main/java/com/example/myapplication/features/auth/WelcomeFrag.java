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
import com.example.myapplication.data.model.User;
import com.google.android.material.button.MaterialButton;

/**
 * This class is the entry point of our App flow. The user is shown two options:
 * Login or Register. The user gets navigated to their respective screens based on
 * their choice.
 */
public class WelcomeFrag extends Fragment implements AuthenticationController.AuthenticationCallback {
    private AuthenticationController controller;
    private MaterialButton btnLogin;
    private CharSequence loginButtonText;

    /** Default constructor
     * @param: None
     * @return: void
     * */
    public WelcomeFrag() {}

    /**
     * This method inflates the layout for the fragment.
     *
     * @param i LayoutInflater object that can be used to inflate any views in the fragment
     * @param c If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param b If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI, or null.
     */
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        return i.inflate(R.layout.fragment_auth_welcome, c, false);
    }

    /**
     * This method gets called after the view has been created. Initializes AuthenticationController
     * and sets up button click listeners.
     *
     * @param v The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param b If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        controller = new AuthenticationController(requireContext(), this);

        btnLogin = v.findViewById(R.id.btnLogin);
        loginButtonText = btnLogin.getText();
        MaterialButton btnRegister = v.findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(x -> controller.attemptAutoLogin());
        btnRegister.setOnClickListener(x ->
                NavHostFragment.findNavController(this).navigate(R.id.navigation_register));
    }

    // AuthenticationCallback implementation

    @Override
    public void onLoginSuccess(User user) {
        navigateBasedOnRole(user);
    }

    @Override
    public void onLoginFailure(String message) {
        toast(message);
        NavHostFragment.findNavController(this).navigate(R.id.navigation_login);
    }

    @Override
    public void onNoSavedUser() {
        NavHostFragment.findNavController(this).navigate(R.id.navigation_login);
    }

    @Override
    public void onLoading(boolean isLoading) {
        setLoginLoading(isLoading);
    }

    /**
     * Sets the login button state to loading or normal.
     *
     * @param loading true to show loading state, false to show normal state.
     * @return: void
     */
    private void setLoginLoading(boolean loading) {
        if (btnLogin == null) {
            return;
        }
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Logging you in..." : loginButtonText);
    }

    /**
     * Navigates the user to the appropriate home screen based on their role.
     *
     * @param user The user whose role determines the navigation destination.
     * @return: void
     */
    private void navigateBasedOnRole(User user) {
        if (!isAdded()) {
            return;
        }
        int destination = R.id.navigation_user_home;
        if ("admin".equalsIgnoreCase(user.getRole())) {
            destination = R.id.navigation_admin_home;
        } else if ("organizer".equalsIgnoreCase(user.getRole())) {
            destination = R.id.navigation_organizer_home;
        }
        NavHostFragment.findNavController(this).navigate(destination);
    }

    /**
     * Displays a toast message.
     *
     * @param message The message to display.
     * @return: void
     */
    private void toast(String message) {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}