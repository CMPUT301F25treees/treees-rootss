package com.example.myapplication.features.profile;

/**
 * View interface for profile deletion operations.
 */
public interface DeleteProfileView {
    void showConfirmationDialog();
    void showProgress(boolean show);
    void showToast(String message);
    void navigateOnSuccess();
}
