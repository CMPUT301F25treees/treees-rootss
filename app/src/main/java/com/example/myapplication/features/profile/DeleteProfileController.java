package com.example.myapplication.features.profile;

import com.example.myapplication.data.repo.UserRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DeleteProfileController {

    private final DeleteProfileView view;
    private final UserRepository userRepository;

    public DeleteProfileController(DeleteProfileView view, UserRepository userRepository) {
        this.view = view;
        this.userRepository = userRepository;
    }

    public void onDeleteProfileClicked() {
        view.showConfirmationDialog();
    }

    public void onDeleteConfirmed() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            view.showToast("Authentication missing. Please log in again.");
            return;
        }

        view.showProgress(true);
        String uid = firebaseUser.getUid();

        OnFailureListener onFailure = e -> {
            view.showProgress(false);
            view.showToast("Failed: " + (e != null ? e.getMessage() : "Unknown error"));
        };

        userRepository.deleteEventsForUser(uid, () -> {
            userRepository.deleteUserDocument(uid, () -> {
                userRepository.deleteAuthUser(firebaseUser, () -> {
                    view.showProgress(false);
                    view.showToast("Profile deleted successfully");
                    view.navigateOnSuccess();
                }, onFailure);
            }, onFailure);
        }, onFailure);
    }

    public void onAdminDeleteConfirmed(String uid, String role) {
        view.showProgress(true);

        OnFailureListener onFailure = e -> {
            view.showProgress(false);
            view.showToast("Failed: " + (e != null ? e.getMessage() : "Unknown error"));
        };

        Runnable deleteDoc = () -> userRepository.deleteUserDocument(uid, () -> {
            view.showProgress(false);
            view.showToast("Profile deleted");
            view.navigateOnSuccess();
        }, onFailure);

        if ("organizer".equalsIgnoreCase(role)) {
            userRepository.disableEventsForUser(uid, deleteDoc, onFailure);
        } else {
            deleteDoc.run();
        }
    }
}
