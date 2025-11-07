package com.example.myapplication.features.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class OEventWaitlistFrag extends Fragment {

    private String eventId;
    private String notificationDocId;
    private String eventName = "";
    private final FirebaseEventRepository eventRepo = new FirebaseEventRepository();

    private RecyclerView waitlistRecycler;
    private TextView emptyState;
    private MaterialButton drawBtn;

    private OEventWaitlistAdapter adapter;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Random rng = new Random();

    // Data
    private final List<String> waitingUids = new ArrayList<>();
    private final Map<String, String> nameByUid = new HashMap<>();

    private long capacity = Long.MAX_VALUE;
    private long entrantsToDraw = 1;

    private int dataEpoch = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_o_event_waitlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        waitlistRecycler = view.findViewById(R.id.waitlistRecycler);
        emptyState       = view.findViewById(R.id.emptyState);
        drawBtn          = view.findViewById(R.id.btnDraw);

        waitlistRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        waitlistRecycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        adapter = new OEventWaitlistAdapter();
        waitlistRecycler.setAdapter(adapter);

        showEmpty();

        if (eventId != null) {
            loadEventMeta();
            loadWaitlist();
        }

        drawBtn.setOnClickListener(v -> drawApplicants());
    }

    private void loadEventMeta() {
        final int myEpoch = ++dataEpoch;

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (myEpoch != dataEpoch) return; // stale
                    if (doc != null && doc.exists()) {
                        Long cap    = doc.getLong("capacity");
                        Long toDraw = doc.getLong("entrantsToDraw");
                        if (cap != null)    capacity = cap;
                        if (toDraw != null) entrantsToDraw = Math.max(1, toDraw);

                        String n = doc.getString("name");
                        if (n != null) eventName = n;
                    }
                });
    }

    private void loadWaitlist() {
        final int myEpoch = ++dataEpoch;

        db.collection("notificationList")
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (myEpoch != dataEpoch) return;

                    if (query.isEmpty()) {
                        waitingUids.clear();
                        nameByUid.clear();
                        applyNames(myEpoch, new ArrayList<>());
                        notificationDocId = null;
                        return;
                    }

                    DocumentSnapshot doc = query.getDocuments().get(0);
                    notificationDocId = doc.getId();

                    @SuppressWarnings("unchecked")
                    List<String> waitingIds = (List<String>) doc.get("waiting");

                    if (waitingIds == null || waitingIds.isEmpty()) {
                        waitingUids.clear();
                        nameByUid.clear();
                        applyNames(myEpoch, new ArrayList<>());
                        return;
                    }

                    Set<String> uniqueOrdered = new LinkedHashSet<>(waitingIds);
                    waitingUids.clear();
                    waitingUids.addAll(uniqueOrdered);

                    fetchUserNames(myEpoch, new ArrayList<>(waitingUids));
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to load waitlist.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchUserNames(int epoch, List<String> userIds) {
        nameByUid.clear();

        AtomicInteger done = new AtomicInteger(0);
        int total = userIds.size();

        for (String uid : userIds) {
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(userDoc -> {
                        if (epoch != dataEpoch) return; // stale
                        String name = resolveName(userDoc, uid);
                        nameByUid.put(uid, name);
                        if (done.incrementAndGet() == total && epoch == dataEpoch) {
                            applyCurrentMapping(epoch);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (epoch != dataEpoch) return; // stale
                        nameByUid.put(uid, uid);
                        if (done.incrementAndGet() == total && epoch == dataEpoch) {
                            applyCurrentMapping(epoch);
                        }
                    });
        }
    }

    private void applyCurrentMapping(int epoch) {

        List<String> names = new ArrayList<>(waitingUids.size());
        for (String uid : waitingUids) {
            names.add(nameByUid.getOrDefault(uid, uid));
        }
        applyNames(epoch, names);
    }

    private String resolveName(DocumentSnapshot userDoc, String fallbackUid) {
        if (userDoc != null && userDoc.exists()) {
            String first = userDoc.getString("firstName");
            String last  = userDoc.getString("lastName");
            String full = (first != null ? first : "") + (last != null ? " " + last : "");
            if (!full.trim().isEmpty()) return full.trim();
        }
        return fallbackUid;
    }

    private void applyNames(int epoch, List<String> names) {
        if (epoch != dataEpoch || !isAdded()) return;
        adapter.setNames(names);
        waitlistRecycler.setVisibility(View.VISIBLE);
        emptyState.setVisibility(names.isEmpty() ? View.VISIBLE : View.GONE);
        drawBtn.setEnabled(!waitingUids.isEmpty());
    }

    private void showEmpty() {
        if (!isAdded()) return;
        waitlistRecycler.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.VISIBLE);
        drawBtn.setEnabled(false);
    }

    private void drawApplicants() {
        if (notificationDocId == null) {
            Toast.makeText(requireContext(), "Waitlist not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (waitingUids.isEmpty()) {
            Toast.makeText(requireContext(), "No one is on the waitlist.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventName == null || eventName.trim().isEmpty()) {
            db.collection("events").document(eventId).get()
                    .addOnSuccessListener(doc -> {
                        String n = (doc != null && doc.exists()) ? doc.getString("name") : null;
                        eventName = (n != null && !n.trim().isEmpty()) ? n.trim() : "Untitled Event";
                        runLotteryNow();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Could not fetch event name: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        } else {
            runLotteryNow();
        }
    }

    private void runLotteryNow() {
        int toSelect = (int) Math.max(1, entrantsToDraw);

        eventRepo.runLottery(
                eventId,
                eventName,                          // ✅ pass the name we ensured
                new ArrayList<>(waitingUids),
                toSelect,
                selectedCount -> {
                    Toast.makeText(
                            requireContext(),
                            "Invited " + selectedCount + " user(s). Notifications sent for \"" + eventName + "\".",
                            Toast.LENGTH_LONG
                    ).show();
                },
                e -> Toast.makeText(
                        requireContext(),
                        "Draw failed: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show()
        );
    }


}
