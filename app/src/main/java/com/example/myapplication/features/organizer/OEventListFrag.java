package com.example.myapplication.features.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class OEventListFrag extends Fragment {

    private static final String FIELD_WAITING = "waiting";
    private static final String FIELD_FINAL   = "final";   // change if your field is named differently

    private enum ListMode { WAITING, FINAL }

    private ListMode currentMode = ListMode.WAITING;

    private String eventId;
    private String notificationDocId;
    private String eventName = "";
    private final FirebaseEventRepository eventRepo = new FirebaseEventRepository();

    private RecyclerView waitlistRecycler;
    private TextView emptyState;
    private MaterialButton drawBtn;
    private MaterialButton btnEvent;

    private OEventListAdapter adapter;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Random rng = new Random();

    private final List<String> currentUids = new ArrayList<>();
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
        btnEvent         = view.findViewById(R.id.btnEvent);

        waitlistRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        waitlistRecycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        adapter = new OEventListAdapter();
        waitlistRecycler.setAdapter(adapter);

        // Initial UI state
        updateListSelectorButton();
        showEmpty();

        if (eventId != null) {
            loadEventMeta();
            loadListForCurrentMode();
        }

        drawBtn.setOnClickListener(v -> drawApplicants());

        btnEvent.setOnClickListener(v -> showListChooser());
    }

    private void showListChooser() {
        PopupMenu menu = new PopupMenu(requireContext(), btnEvent);
        menu.getMenu().add(0, 1, 0, "Waiting list");
        menu.getMenu().add(0, 2, 1, "Final list");

        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            ListMode chosen = (id == 1) ? ListMode.WAITING : ListMode.FINAL;
            if (chosen != currentMode) {
                currentMode = chosen;
                updateListSelectorButton();
                loadListForCurrentMode();
            }
            return true;
        });

        menu.show();
    }

    private void updateListSelectorButton() {
        if (btnEvent == null) return;
        String label = (currentMode == ListMode.WAITING) ? "Waiting list" : "Final list";
        btnEvent.setText(label);

        drawBtn.setVisibility(currentMode == ListMode.WAITING ? View.VISIBLE : View.GONE);
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

    private void loadListForCurrentMode() {
        final int myEpoch = ++dataEpoch;

        db.collection("notificationList")
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (myEpoch != dataEpoch) return;

                    if (query.isEmpty()) {
                        currentUids.clear();
                        nameByUid.clear();
                        applyNames(myEpoch, new ArrayList<>());
                        notificationDocId = null;
                        return;
                    }

                    DocumentSnapshot doc = query.getDocuments().get(0);
                    notificationDocId = doc.getId();

                    String field = (currentMode == ListMode.WAITING) ? FIELD_WAITING : FIELD_FINAL;

                    @SuppressWarnings("unchecked")
                    List<String> ids = (List<String>) doc.get(field);

                    if (ids == null || ids.isEmpty()) {
                        currentUids.clear();
                        nameByUid.clear();
                        applyNames(myEpoch, new ArrayList<>());
                        return;
                    }

                    // keep insertion order but drop duplicates
                    Set<String> uniqueOrdered = new LinkedHashSet<>(ids);
                    currentUids.clear();
                    currentUids.addAll(uniqueOrdered);

                    fetchUserNames(myEpoch, new ArrayList<>(currentUids));
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to load list.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchUserNames(int epoch, List<String> userIds) {
        nameByUid.clear();

        if (userIds.isEmpty()) {
            applyNames(epoch, new ArrayList<>());
            return;
        }

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
        List<String> names = new ArrayList<>(currentUids.size());
        for (String uid : currentUids) {
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

        boolean empty = names.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        emptyState.setText(
                (currentMode == ListMode.WAITING)
                        ? "No one on the waitlist."
                        : "No one on the final list."
        );

        // Draw button only enabled when showing non-empty waiting list
        boolean enableDraw = (currentMode == ListMode.WAITING) && !currentUids.isEmpty();
        drawBtn.setEnabled(enableDraw);
    }

    private void showEmpty() {
        if (!isAdded()) return;
        waitlistRecycler.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.VISIBLE);
        emptyState.setText(
                (currentMode == ListMode.WAITING)
                        ? "No one on the waitlist."
                        : "No one on the final list."
        );
        drawBtn.setEnabled(false);
    }

    private void drawApplicants() {
        if (currentMode != ListMode.WAITING) {
            Toast.makeText(requireContext(), "Switch to the Waiting list to draw.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (notificationDocId == null) {
            Toast.makeText(requireContext(), "Waitlist not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUids.isEmpty()) {
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
                eventName,
                new ArrayList<>(currentUids), // pulling from waiting list (enforced above)
                toSelect,
                selectedCount -> Toast.makeText(
                        requireContext(),
                        "Invited " + selectedCount + " user(s). Notifications sent for \"" + eventName + "\".",
                        Toast.LENGTH_LONG
                ).show(),
                e -> Toast.makeText(
                        requireContext(),
                        "Draw failed: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show()
        );
    }
}
