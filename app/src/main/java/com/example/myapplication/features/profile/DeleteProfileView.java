package com.example.myapplication.features.profile;

public interface DeleteProfileView {
    void showConfirmationDialog();
    void showProgress(boolean show);
    void showToast(String message);
    void navigateOnSuccess();
}
