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
import com.example.myapplication.core.DeviceLoginStore;
import com.example.myapplication.core.UserSession;
import com.example.myapplication.data.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * This class is the entry point of our App flow. The user is shown two options:
 * Login or Register. The user gets navigated to their respective screens based on
 * their choice.
 */
public class WelcomeFrag extends Fragment {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private MaterialButton btnLogin;
    private CharSequence loginButtonText;
    private boolean isAutoLoggingIn = false;

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
     * This method gets called after the view has been created. Initializes FirebaseAuth and
     * Firestore instances, and sets up button click listeners.
     *
     * @param v The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param b If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnLogin = v.findViewById(R.id.btnLogin);
        loginButtonText = btnLogin.getText();
        MaterialButton btnRegister = v.findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(x -> handleLoginButton());
        btnRegister.setOnClickListener(x ->
                NavHostFragment.findNavController(this).navigate(R.id.navigation_register));
    }

    /**
     * Handles the login button click event. Attempts to auto-login the user if a remembered
     * user exists. If auto-login fails, redirects to manual login.
     * @param: None
     * @return: void
     */
    private void handleLoginButton() {
        if (isAutoLoggingIn) {
            return;
        }

        User rememberedUser = DeviceLoginStore.getRememberedUser(requireContext());
        if (rememberedUser == null) {
            NavHostFragment.findNavController(this).navigate(R.id.navigation_login);
            return;
        }

        if (isLocalUser(rememberedUser)) {
            finalizeLogin(rememberedUser);
            return;
        }

        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null || firebaseUser.getUid() == null
                || !firebaseUser.getUid().equals(rememberedUser.getUid())) {
            redirectToManualLogin();
            return;
        }

        isAutoLoggingIn = true;
        setLoginLoading(true);

        db.collection("users").document(firebaseUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    isAutoLoggingIn = false;
                    setLoginLoading(false);

                    if (!task.isSuccessful()) {
                        continueWithSavedUser(rememberedUser);
                        return;
                    }

                    DocumentSnapshot d = task.getResult();
                    if (d == null) {
                        continueWithSavedUser(rememberedUser);
                        return;
                    }

                    User user = new User();
                    user.setUid(firebaseUser.getUid());
                    user.setEmail(firebaseUser.getEmail());

                    String role = null;
                    Object roleValue = d.get("role");
                    if (roleValue != null) {
                        role = roleValue.toString();
                    }
                    user.setRole(role != null ? role : "user");

                    if (d.contains("username")) {
                        Object username = d.get("username");
                        if (username != null) {
                            user.setUsername(username.toString());
                        }
                    }

                    UserSession.getInstance().setCurrentUser(user);
                    DeviceLoginStore.rememberUser(requireContext(), user);
                    navigateBasedOnRole(user);
                });
    }

    /**
     * Redirects the user to the manual login screen after clearing session data.
     * @param: None
     * @return: void
     */
    private void redirectToManualLogin() {
        isAutoLoggingIn = false;
        setLoginLoading(false);

        if (getContext() != null) {
            DeviceLoginStore.markLoggedOut(getContext());
        }
        UserSession.getInstance().clearSession();
        FirebaseAuth.getInstance().signOut();

        if (!isAdded()) {
            return;
        }

        toast("Unable to restore your session. Please login.");
        NavHostFragment.findNavController(this).navigate(R.id.navigation_login);
    }

    /**
     * Continues the login process using the saved user data when profile refresh fails.
     *
     * @param savedUser The user data saved on the device.
     * @return: void
     */
    private void continueWithSavedUser(User savedUser) {
        if (!isAdded()) {
            return;
        }
        toast("Could not refresh your profile. Using the last saved account.");
        finalizeLogin(savedUser);
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
     * Finalizes the login process by setting the current user in session,
     * remembering the user on the device, and navigating based on user role.
     *
     * @param user The user to finalize login for.
     * @return: void
     */
    private void finalizeLogin(User user) {
        if (!isAdded()) {
            return;
        }
        UserSession.getInstance().setCurrentUser(user);
        DeviceLoginStore.rememberUser(requireContext(), user);
        navigateBasedOnRole(user);
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
     * Checks if the user is a local (dummy) user.
     *
     * @param user The user to check.
     * @return true if the user is local, false otherwise.
     */
    private boolean isLocalUser(User user) {
        return user.getUid() != null && user.getUid().startsWith("LOCAL_");
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
