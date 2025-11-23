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

    public WelcomeFrag() {}

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        return i.inflate(R.layout.fragment_auth_welcome, c, false);
    }

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

    private void continueWithSavedUser(User savedUser) {
        if (!isAdded()) {
            return;
        }
        toast("Could not refresh your profile. Using the last saved account.");
        finalizeLogin(savedUser);
    }

    private void setLoginLoading(boolean loading) {
        if (btnLogin == null) {
            return;
        }
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Logging you in..." : loginButtonText);
    }

    private void finalizeLogin(User user) {
        if (!isAdded()) {
            return;
        }
        UserSession.getInstance().setCurrentUser(user);
        DeviceLoginStore.rememberUser(requireContext(), user);
        navigateBasedOnRole(user);
    }

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

    private boolean isLocalUser(User user) {
        return user.getUid() != null && user.getUid().startsWith("LOCAL_");
    }

    private void toast(String message) {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
