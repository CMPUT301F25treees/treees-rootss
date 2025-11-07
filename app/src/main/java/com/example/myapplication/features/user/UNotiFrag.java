// UNotiFrag.java (User)
package com.example.myapplication.features.user;

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
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * This class displays the list of notifications for the logged in user.
 *
 * Will retrieve all notifications to the logged in user.
 */
public class UNotiFrag extends Fragment {

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
        requireActivity().setTitle("USER Notifications");


        recyclerView = view.findViewById(R.id.notifications_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        Query query = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereArrayContains("uID", uid)
                .orderBy("dateMade", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<UNotiItem> options = new FirestoreRecyclerOptions.Builder<UNotiItem>()
                .setQuery(query, UNotiItem.class)
                .setLifecycleOwner(getViewLifecycleOwner())
                .build();

        adapter = new UNotiAdapter(options, snapshot -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("USER DIALOG")
                    .setItems(new CharSequence[]{"View Details"}, (dialog, which) -> {
                        if (which == 0) {
                            Toast.makeText(requireContext(), "View details coming soon", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Close", (dialog, which) -> dialog.dismiss())
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
