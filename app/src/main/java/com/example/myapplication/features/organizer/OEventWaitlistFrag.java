package com.example.myapplication.features.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class OEventWaitlistFrag extends Fragment {
    private String eventId;
    private RecyclerView finalListRecycler;
    private TextView emptyState;
    private OFinalListAdapter adapter;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_o_event_finallist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventId = getArguments() != null ? getArguments().getString("eventId") : null;
        finalListRecycler = view.findViewById(R.id.waitlistRecycler);
        emptyState = view.findViewById(R.id.emptyState);

        finalListRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        finalListRecycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        adapter = new OFinalListAdapter();
        finalListRecycler.setAdapter(adapter);

        showEmpty();

        if (eventId != null) {
            finalList();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (eventId != null) {
            finalList(); // refresh when returning to screen
        }
    }

    private void finalList() {
        db.collection("notificationList")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        showEmpty();
                        return;
                    }
                    DocumentSnapshot doc = query.getDocuments().get(0);
                    List<String> finalIds = (List<String>) doc.get("final");
                    if (finalIds == null || finalIds.isEmpty()) {
                        showEmpty();
                        return;
                    }
                    fetchUserNames(finalIds);

        })
                .addOnFailureListener(e -> {
                    showEmpty();
        });
    }

    private void fetchUserNames(List<String> userIds) {
        List<String> names = new ArrayList<>();
        AtomicInteger done = new AtomicInteger(0);
        int total = userIds.size();

        for (String uid : userIds) {
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(userDoc -> {
                        names.add(resolveName(userDoc, uid));
                        if (done.incrementAndGet() == total) {
                            applyNames(names);
                        }
                    })
                    .addOnFailureListener(e -> {
                        names.add(uid); // fallback
                        if (done.incrementAndGet() == total) {
                            applyNames(names);
                        }
                    });
        }
    }

    private void applyNames(List<String> names) {
        if (!isAdded()) return;
        if (names.isEmpty()) {
            showEmpty();
        } else {
            adapter.setNames(names);
            finalListRecycler.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private String resolveName(DocumentSnapshot userDoc, String fallbackUid) {
        if (userDoc != null && userDoc.exists()) {
            String first = userDoc.getString("firstName");
            String last = userDoc.getString("lastName");
            String full = (first != null ? first : "") +
                    (last != null ? " " + last : "");
            if (!full.trim().isEmpty()) return full.trim();
        }
        return fallbackUid;
    }

    private void showEmpty() {
        emptyState.setVisibility(View.VISIBLE);
        finalListRecycler.setVisibility(View.GONE);
    }
}
