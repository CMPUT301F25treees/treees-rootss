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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

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
        MaterialButton btnEvent = v.findViewById(R.id.btnRemoveEvent);       // future story
        MaterialButton btnOrg   = v.findViewById(R.id.btnRemoveOrganizer);

        // Load current image
        FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                .addOnSuccessListener(d -> {
                    String url = d.getString("imageUrl");
                    if (url != null && !url.isEmpty()) Glide.with(preview).load(url).into(preview);
                });

        // Remove Previewed Image -> clear imageUrl/posterUrl fields
        btnImage.setOnClickListener(x -> new AlertDialog.Builder(requireContext())
                .setTitle("Remove event image?")
                .setMessage("This will remove the image from the event.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("events").document(eventId)
                            .update("imageUrl", FieldValue.delete(), "posterUrl", FieldValue.delete())
                            .addOnSuccessListener(v1 -> {
                                Toast.makeText(requireContext(), "Image removed", Toast.LENGTH_SHORT).show();
                                NavHostFragment.findNavController(ARemoveFrag.this).navigateUp();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show());

        // Remove Organizer -> demote organizerID user to "User" + suspended=true
        btnOrg.setEnabled(true);
        btnOrg.setOnClickListener(x -> new AlertDialog.Builder(requireContext())
                .setTitle("Remove organizer?")
                .setMessage("This will revoke organizer permissions for this event's organizer.")
                .setPositiveButton("Remove", (d1, w1) -> removeOrganizerForEvent())
                .setNegativeButton("Cancel", null)
                .show());
    }

    private void removeOrganizerForEvent() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    String orgId = doc.getString("organizerID"); // from your schema
                    if (orgId == null || orgId.isEmpty()) {
                        Toast.makeText(requireContext(), "No organizer ID on event", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.collection("users").document(orgId)
                            .update("role", "User", "suspended", true)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(requireContext(), "Organizer removed", Toast.LENGTH_SHORT).show();
                                NavHostFragment.findNavController(ARemoveFrag.this).navigateUp();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
