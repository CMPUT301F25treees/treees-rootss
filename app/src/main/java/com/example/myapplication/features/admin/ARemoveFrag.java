package com.example.myapplication.features.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.*;
import androidx.navigation.fragment.NavHostFragment;

public class ARemoveFrag extends Fragment {
    private String eventId;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        return i.inflate(R.layout.fragment_a_remove, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        ImageView preview = v.findViewById(R.id.preview);
        MaterialButton btnImage = v.findViewById(R.id.btnRemoveImage);
        MaterialButton btnEvent = v.findViewById(R.id.btnRemoveEvent);
        MaterialButton btnOrg   = v.findViewById(R.id.btnRemoveOrganizer);

        FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                .addOnSuccessListener(d -> {
                    String url = d.getString("imageUrl");
                    if (url != null) Glide.with(preview).load(url).into(preview);
                });

        // US 03.06.01 — remove event image (detach from doc)
        btnImage.setOnClickListener(x -> new AlertDialog.Builder(requireContext())
                .setTitle("Remove event image?")
                .setMessage("This will remove the image from the event.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("events")
                            .document(eventId)
                            .update("imageUrl", FieldValue.delete(), "posterUrl", FieldValue.delete())
                            .addOnSuccessListener(v1 -> {
                                Toast.makeText(requireContext(), "Image removed", Toast.LENGTH_SHORT).show();
                                NavHostFragment.findNavController(ARemoveFrag.this).popBackStack(); // or .navigateUp()
                            })
                            .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed: "+e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show());

        // These two are for future stories – disabled in layout
        btnEvent.setOnClickListener(x -> Toast.makeText(requireContext(),"Remove Event coming next",Toast.LENGTH_SHORT).show());
        btnOrg.setOnClickListener(x -> Toast.makeText(requireContext(),"Remove Organizer coming next",Toast.LENGTH_SHORT).show());
    }
}
