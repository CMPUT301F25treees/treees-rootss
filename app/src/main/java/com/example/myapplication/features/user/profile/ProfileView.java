package com.example.myapplication.features.user.profile;

public interface ProfileView {
    void showLoading(boolean isLoading);
    void showProfileData(String firstName, String lastName, String email, String phone);
    void showFirstNameError(String error);
    void showEmailError(String error);
    void showToast(String message);
    void navigateBack();
    void showPasswordDialog();
    void showReauthError(String error);
    void updateLocalSession(String firstName, String email);
}
