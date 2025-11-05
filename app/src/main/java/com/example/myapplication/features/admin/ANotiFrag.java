// ANotiFrag.java (Admin)
package com.example.myapplication.features.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.features.user.UNotiAdapter;
import com.example.myapplication.features.user.UNotiItem;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ANotiFrag extends Fragment {

    private RecyclerView recyclerView;
    private UNotiAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_u_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle("ADMIN Notifications");


        recyclerView = view.findViewById(R.id.notifications_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        Query query = FirebaseFirestore.getInstance()
                .collection("notifications");


        FirestoreRecyclerOptions<UNotiItem> options = new FirestoreRecyclerOptions.Builder<UNotiItem>()
                .setQuery(query, UNotiItem.class)
                .setLifecycleOwner(getViewLifecycleOwner())
                .build();

        adapter = new UNotiAdapter(options, snapshot -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("ADMIN DIALOG")
                    .setItems(new CharSequence[]{"Delete"}, (dialog, which) -> {
                        if (which == 0) {
                            snapshot.getReference().delete()
                                    .addOnSuccessListener(aVoid ->
                                            Toast.makeText(requireContext(), "Notification deleted", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(requireContext(), "Error deleting notification", Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });
        recyclerView.setAdapter(adapter);

    }

    @Override
    public void onDestroyView() {
        recyclerView.setAdapter(null);
        super.onDestroyView();
    }
}
