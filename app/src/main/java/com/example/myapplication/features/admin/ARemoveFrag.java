package com.example.myapplication.features.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;

/**
 * Remove Options fragment for administrators.
 * <p>
 * Provides three destructive actions for a selected event:
 * <ul>
 *   <li>Remove preview image (clears {@code imageUrl}/{@code posterUrl})</li>
 *   <li>Remove event (deletes {@code /events/{eventId}} and its {@code /images} subcollection,
 *       with best-effort deletion of any referenced storage objects)</li>
 *   <li>Remove organizer (demotes to {@code role="User"} and sets {@code suspended=true})</li>
 * </ul>
 */
public class ARemoveFrag extends Fragment {

    private String eventId;
    private boolean hasPreview = false;
    private String eventName = "";
    private String organizerId = "";

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
                    .setMessage("This will remove the preview image from this event.")
                    .setPositiveButton("Remove", (dialog, which) ->
                            FirebaseFirestore.getInstance().collection("events").document(eventId)
                                    .update(
                                            "imageUrl", FieldValue.delete(),
                                            "posterUrl", FieldValue.delete()
                                    )
                                    .addOnSuccessListener(v1 -> {
                                        Toast.makeText(requireContext(), "Image removed", Toast.LENGTH_SHORT).show();
                                        NavHostFragment.findNavController(ARemoveFrag.this).navigateUp();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show())
                    )
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
                    // Try to delete storage files if storagePath exists
                    for (DocumentSnapshot d : q) {
                        String sp = d.getString("storagePath");
                        if (sp != null && !sp.isEmpty()) FirebaseStorage.getInstance().getReference(sp).delete();
                    }
                    // Batch delete image docs
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
                .addOnSuccessListener(v ->
                {
                    Toast.makeText(requireContext(), "Event deleted", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(ARemoveFrag.this).navigateUp();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Demotes the event’s organizer to {@code role="User"} and sets {@code suspended=true}.
     * Shows a message if the organizer id is unavailable or on failure.
     */
    private void removeOrganizerForEvent() {
        if (organizerId.isEmpty()) {
            Toast.makeText(requireContext(), "No organizer ID on event", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseFirestore.getInstance().collection("users").document(organizerId)
                .update("role", "User", "suspended", true)
                .addOnSuccessListener(v ->
                        Toast.makeText(requireContext(), "Organizer removed", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Null-safe string helper.
     *
     * @param s input string (may be {@code null})
     * @return empty string if {@code s} is null; otherwise {@code s}
     */
    private static String safe(String s){ return s == null ? "" : s; }
}
