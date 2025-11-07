package com.example.myapplication.features.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.*;

public class AUserDetailFrag extends Fragment {

    private String uid, name, email, role, avatarUrl;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        return i.inflate(R.layout.fragment_a_user_detail, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        Bundle args = getArguments() != null ? getArguments() : Bundle.EMPTY;
        uid       = args.getString("uid", "");
        name      = args.getString("name", "");
        email     = args.getString("email", "");
        role      = args.getString("role", "");
        avatarUrl = args.getString("avatarUrl", "");

        ImageView iv = v.findViewById(R.id.ivAvatar);
        TextView tvN = v.findViewById(R.id.tvName);
        TextView tvE = v.findViewById(R.id.tvEmail);
        TextView tvR = v.findViewById(R.id.tvRole);
        MaterialButton back = v.findViewById(R.id.btnBack);
        MaterialButton delete = v.findViewById(R.id.btnDeleteProfile);

        tvN.setText(name);
        tvE.setText(email);
        tvR.setText(role);
        if (!avatarUrl.isEmpty()) Glide.with(iv).load(avatarUrl).into(iv);

        back.setOnClickListener(x -> NavHostFragment.findNavController(this).navigateUp());
        delete.setOnClickListener(x -> confirmDelete());
    }

    private void confirmDelete() {
        String who = !name.isEmpty() ? name : email;
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete profile?")
                .setMessage("This will remove " + who + " from the app. "
                        + "It deletes their profile document in Firestore. "
                        + "It does not remove their sign-in account.")
                .setPositiveButton("Delete", (d, w) -> deleteProfile())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProfile() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if ("organizer".equalsIgnoreCase(role)) {
            db.collection("events").whereEqualTo("organizerID", uid).get()
                    .addOnSuccessListener(q -> {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : q) {
                            batch.update(doc.getReference(), "disabled", true);
                        }
                        batch.commit().addOnCompleteListener(t -> actuallyDeleteUserDoc(db));
                    })
                    .addOnFailureListener(e -> actuallyDeleteUserDoc(db)); // still try to delete user doc
        } else {
            actuallyDeleteUserDoc(db);
        }
    }

    private void actuallyDeleteUserDoc(FirebaseFirestore db) {
        db.collection("users").document(uid).delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), "Profile deleted", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).navigateUp();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
