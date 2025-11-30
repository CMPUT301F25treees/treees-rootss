package com.example.myapplication.features.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;

import com.example.myapplication.core.ImageUtils;
import com.example.myapplication.data.repo.ImageRepository;
import com.example.myapplication.data.firebase.FirebaseUserRepository;
import com.example.myapplication.features.profile.DeleteProfileController;
import com.example.myapplication.features.profile.DeleteProfileView;

/**
 * Admin remove-options fragment for a selected event.
 * <p>
 * Provides destructive admin operations for an event: removing the
 * preview image, deleting the event document (and associated image
 * metadata), and demoting the organizer. This fragment is a view that
 * delegates Firestore and Storage operations to repository-style code.
 * <p>
 */
public class ARemoveFrag extends Fragment implements DeleteProfileView {

    /**
     *  Event ID of the event to remove.
     */
    private String eventId;

    /**
     *  Whether the event has a preview image.
     */
    private boolean hasPreview = false;

    /**
     *  Event name.
     */
    private String eventName = "";

    /**
     *  Event organizer ID.
     */
    private String organizerId = "";
    private DeleteProfileController deleteController;

    /**
     * Default no-argument constructor.
     */
    public ARemoveFrag() {}

    /**
     * Inflates the remove-options layout.
     *
     * @param i  layout inflater
     * @param c  parent container
     * @param b  saved instance state, if any
     * @return   the inflated root view for this fragment
     */
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        return i.inflate(R.layout.fragment_a_remove, c, false);
    }

    /**
     * Initializes UI and wires destructive actions after the view is created.
     * Loads event data (name, organizer, preview image) to drive button state/labels.
     *
     * @param v root view returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param b saved instance state, if any
     */
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);
        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        ImageView preview = v.findViewById(R.id.preview);
        MaterialButton btnImage = v.findViewById(R.id.btnRemoveImage);
        MaterialButton btnEvent = v.findViewById(R.id.btnRemoveEvent);
        MaterialButton btnOrg   = v.findViewById(R.id.btnRemoveOrganizer);

        btnImage.setEnabled(false);
        btnImage.setAlpha(0.55f);

        deleteController = new DeleteProfileController(this, new FirebaseUserRepository());

        FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                .addOnSuccessListener(d -> {
                    eventName   = safe(d.getString("name"));
                    organizerId = safe(d.getString("organizerID"));
                    String url  = d.getString("imageUrl");

                    if (!eventName.isEmpty()) {
                        btnEvent.setText("Remove Event: " + eventName);
                    }

                    if (!organizerId.isEmpty()) {
                        FirebaseFirestore.getInstance().collection("users").document(organizerId).get()
                                .addOnSuccessListener(u -> {
                                    String first = safe(u.getString("firstName"));
                                    String last  = safe(u.getString("lastName"));
                                    String full  = (first + " " + last).trim();
                                    if (full.isEmpty()) full = safe(u.getString("name"));
                                    if (!full.isEmpty()) btnOrg.setText("Remove Organizer: " + full);
                                });
                    }

                    hasPreview = url != null && !url.isEmpty();
                    if (hasPreview) {
                        Glide.with(preview).load(url).into(preview);
                        btnImage.setEnabled(true);
                        btnImage.setAlpha(1f);
                    }
                });

        btnImage.setOnClickListener(x -> {
            if (!hasPreview) {
                Toast.makeText(requireContext(), "No preview image to remove.", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle("Remove event image?")
                    .setMessage("This will remove the preview image from this event and replace it with a default.")
                    .setPositiveButton("Remove", (dialog, which) -> {
                        Uri defaultPosterUri = ImageUtils.createDefaultPosterUri(requireContext());
                        if (defaultPosterUri != null) {
                            ImageRepository imageRepository = new ImageRepository();
                            imageRepository.uploadImage(defaultPosterUri, new ImageRepository.UploadCallback() {
                                @Override
                                public void onSuccess(String secureUrl) {
                                    FirebaseFirestore.getInstance().collection("events").document(eventId)
                                            .update(
                                                    "imageUrl", secureUrl,
                                                    "posterUrl", secureUrl
                                            )
                                            .addOnSuccessListener(v1 -> {
                                                Toast.makeText(requireContext(), "Image replaced with default", Toast.LENGTH_SHORT).show();
                                                NavHostFragment.findNavController(ARemoveFrag.this).navigateUp();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(requireContext(), "Failed to update with default image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                }

                                @Override
                                public void onError(String e) {
                                    Toast.makeText(requireContext(), "Failed to upload default image: " + e, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(requireContext(), "Failed to create default image URI", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnEvent.setOnClickListener(x -> new AlertDialog.Builder(requireContext())
                .setTitle("Delete event?")
                .setMessage("This will permanently delete this event and its images.")
                .setPositiveButton("Delete", (d1, w1) -> removeEvent())
                .setNegativeButton("Cancel", null)
                .show());

        btnOrg.setOnClickListener(x -> new AlertDialog.Builder(requireContext())
                .setTitle("Remove organizer?")
                .setMessage("This will revoke organizer permissions for this event's organizer.")
                .setPositiveButton("Remove", (d2, w2) -> removeOrganizerForEvent())
                .setNegativeButton("Cancel", null)
                .show());
    }

    /**
     * Deletes the event’s image documents under {@code /events/{eventId}/images}, attempts to
     * delete any referenced storage objects, and then proceeds to delete the event document.
     */
    private void removeEvent() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference eventRef = db.collection("events").document(eventId);

        eventRef.collection("images").orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(q -> {
                    for (DocumentSnapshot d : q) {
                        String sp = d.getString("storagePath");
                        if (sp != null && !sp.isEmpty()) FirebaseStorage.getInstance().getReference(sp).delete();
                    }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot d : q) batch.delete(d.getReference());
                    batch.commit()
                            .addOnCompleteListener(t -> deleteEventDoc(eventRef))
                            .addOnFailureListener(e -> deleteEventDoc(eventRef));
                })
                .addOnFailureListener(e -> deleteEventDoc(eventRef));
    }

    /**
     * Finalizes the event deletion by removing the event document.
     * Navigates back on success and shows a toast on failure.
     *
     * @param eventRef reference to {@code /events/{eventId}}
     */
    private void deleteEventDoc(DocumentReference eventRef) {
        eventRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(),
                            "Event removed", Toast.LENGTH_SHORT).show();
                    navigateToAdminHome();
                })
                .addOnFailureListener(e -> {
                    String msg = "Failed to delete event";
                    if (e != null && e.getMessage() != null) {
                        msg += ": " + e.getMessage();
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Navigates back to the admin home screen after a destructive action.
     * <p>
     * This pops the back stack up to (but not including) the admin home
     * destination, leaving the admin home as the visible screen.
     */
    private void navigateToAdminHome() {
        NavController navController = NavHostFragment.findNavController(this);
        navController.popBackStack(R.id.navigation_admin_home, false);
    }

    /**
     * Handles the "Remove Organizer" button click.
     * Delegates to {@link DeleteProfileController} to show a confirmation
     * dialog and, on confirmation, delete the organizer's profile and
     * all of their events.
     */
    private void onRemoveOrganizerClicked() {
        if (organizerId == null || organizerId.isEmpty()) {
            Toast.makeText(requireContext(), "No organizer ID on event", Toast.LENGTH_SHORT).show();
            return;
        }
        if (deleteController != null) {
            deleteController.onDeleteProfileClicked();
        }
    }

    /**
     * Deletes the organizer's profile and all events they created via
     * {@link DeleteProfileController}. This is invoked after the user
     * confirms deletion in {@link #showConfirmationDialog()}.
     */
    private void removeOrganizerForEvent() {
        if (organizerId == null || organizerId.isEmpty()) {
            Toast.makeText(requireContext(), "No organizer ID on event", Toast.LENGTH_SHORT).show();
            return;
        }
        if (deleteController != null) {
            deleteController.onAdminDeleteConfirmed(organizerId, "User");
        }
    }

    @Override
    public void showConfirmationDialog() {
        String who = eventName.isEmpty()
                ? "this organizer"
                : "the organizer of " + eventName;
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete organizer profile?")
                .setMessage("This will delete " + who + " from the app. "
                        + "It deletes their profile and all events they created, "
                        + "and prevents them from using this app again.")
                .setPositiveButton("Delete", (d, w) -> removeOrganizerForEvent())
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void showProgress(boolean show) {
    }

    @Override
    public void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void navigateOnSuccess() {
        navigateToAdminHome();
    }

    /**
     * Null-safe string helper.
     *
     * @param s input string (may be {@code null})
     * @return empty string if {@code s} is null; otherwise {@code s}
     */
    private static String safe(String s){ return s == null ? "" : s; }
}
