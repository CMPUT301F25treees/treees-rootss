package com.example.myapplication.features.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;

public class ARemoveFrag extends Fragment {

    private String eventId;

    public ARemoveFrag() {}

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        return i.inflate(R.layout.fragment_a_remove, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);
        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        ImageView preview = v.findViewById(R.id.preview);
        MaterialButton btnImage = v.findViewById(R.id.btnRemoveImage);
        MaterialButton btnEvent = v.findViewById(R.id.btnRemoveEvent);
        MaterialButton btnOrg   = v.findViewById(R.id.btnRemoveOrganizer);

        FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                .addOnSuccessListener(d -> {
                    String url = d.getString("imageUrl");
                    if (url != null && !url.isEmpty()) Glide.with(preview).load(url).into(preview);
                });

        btnImage.setOnClickListener(x -> new AlertDialog.Builder(requireContext())
                .setTitle("Remove event image?")
                .setMessage("This will remove the image from the event.")
                .setPositiveButton("Remove", (dialog, which) ->
                        FirebaseFirestore.getInstance().collection("events").document(eventId)
                                .update("imageUrl", FieldValue.delete(), "posterUrl", FieldValue.delete())
                                .addOnSuccessListener(v1 -> {
                                    Toast.makeText(requireContext(), "Image removed", Toast.LENGTH_SHORT).show();
                                    NavHostFragment.findNavController(ARemoveFrag.this).navigateUp();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show())
                )
                .setNegativeButton("Cancel", null)
                .show());

        btnEvent.setOnClickListener(x -> new AlertDialog.Builder(requireContext())
                .setTitle("Delete event?")
                .setMessage("This will permanently delete this event and its images. This cannot be undone.")
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

    private void removeEvent() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference eventRef = db.collection("events").document(eventId);

        eventRef.collection("images").get()
                .addOnSuccessListener(q -> {
                    for (DocumentSnapshot d : q) {
                        String sp = d.getString("storagePath");
                        if (sp != null && !sp.isEmpty()) {
                            FirebaseStorage.getInstance().getReference(sp).delete();
                        }
                    }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot d : q) batch.delete(d.getReference());
                    batch.commit()
                            .addOnCompleteListener(t -> deleteEventDoc(eventRef))  // proceed regardless of success
                            .addOnFailureListener(e -> deleteEventDoc(eventRef));
                })
                .addOnFailureListener(e -> {
                    deleteEventDoc(eventRef);
                });
    }

    private void deleteEventDoc(DocumentReference eventRef) {
        eventRef.delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), "Event deleted", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(ARemoveFrag.this).navigateUp();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeOrganizerForEvent() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    String orgId = doc.getString("organizerID");
                    if (orgId == null || orgId.isEmpty()) {
                        Toast.makeText(requireContext(), "No organizer ID on event", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.collection("users").document(orgId)
                            .update("role", "User", "suspended", true)
                            .addOnSuccessListener(v ->
                                    Toast.makeText(requireContext(), "Organizer removed", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
