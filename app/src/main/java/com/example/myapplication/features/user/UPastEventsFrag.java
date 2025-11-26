package com.example.myapplication.features.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class UPastEventsFrag extends Fragment {

    private RecyclerView recycler;
    private UPastEventsAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private final List<UPastEventItem> pastEvents = new ArrayList<>();

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    private String formatDate(Long millis) {
        if (millis == null) return "";
        return DATE_FORMAT.format(new Date(millis));
    }

    public UPastEventsFrag() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_u_past_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.recyclerPastEvents);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new UPastEventsAdapter();
        recycler.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            loadPastEvents();
        }
    }

    private void loadPastEvents() {
        pastEvents.clear();
        final String uid = currentUser.getUid();

        db.collection("notificationList")
                .whereArrayContains("all", uid)
                .get()
                .addOnSuccessListener(this::handlePastEventsResult);
    }

    private void handlePastEventsResult(QuerySnapshot notiSnapshot) {
        final String uid = currentUser.getUid();
        pastEvents.clear();

        if (notiSnapshot == null || notiSnapshot.isEmpty()) {
            adapter.setItems(pastEvents);
            return;
        }

        AtomicInteger remaining = new AtomicInteger(notiSnapshot.size());

        for (DocumentSnapshot notifDoc : notiSnapshot.getDocuments()) {

            String eventId = notifDoc.getString("eventId");
            if (eventId == null) {
                if (remaining.decrementAndGet() == 0) {
                    sortAndShow();
                }
                continue;
            }

            @SuppressWarnings("unchecked")
            List<String> finalUsers     = (List<String>) notifDoc.get("final");
            @SuppressWarnings("unchecked")
            List<String> invitedUsers   = (List<String>) notifDoc.get("invited");
            @SuppressWarnings("unchecked")
            List<String> cancelledUsers = (List<String>) notifDoc.get("cancelled");

            String status;
            if (finalUsers != null && finalUsers.contains(uid)) {
                status = "Accepted";
            } else if (invitedUsers != null && invitedUsers.contains(uid)) {
                status = "Invited";
            } else if (cancelledUsers != null && cancelledUsers.contains(uid)) {
                status = "Declined";
            } else {
                status = "Not Selected";
            }

            fetchEventDetails(eventId, status, remaining);
        }
    }

    private void fetchEventDetails(String eventId, String status, AtomicInteger remaining) {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventSnap -> {
                    if (eventSnap.exists()) {
                        String name = eventSnap.getString("name");
                        String priceDisplay = eventSnap.getString("priceDisplay");
                        Long endMillis = eventSnap.getLong("endTimeMillis");

                        long now = System.currentTimeMillis();

                        if (endMillis != null && endMillis <= now) {
                            String dateStr = formatDate(endMillis);

                            UPastEventItem item = new UPastEventItem(
                                    eventId,
                                    name != null ? name : "",
                                    priceDisplay != null ? priceDisplay : "",
                                    dateStr,
                                    status
                            );

                            pastEvents.add(item);
                        }
                    }

                    if (remaining.decrementAndGet() == 0) {
                        sortAndShow();
                    }
                })
                .addOnFailureListener(e -> {
                    if (remaining.decrementAndGet() == 0) {
                        sortAndShow();
                    }
                });
    }

    private void sortAndShow() {
        Collections.sort(pastEvents, (e1, e2) -> {
            try {
                Date d1 = DATE_FORMAT.parse(e1.getDate());
                Date d2 = DATE_FORMAT.parse(e2.getDate());
                if (d1 == null || d2 == null) return 0;
                return d2.compareTo(d1);
            } catch (ParseException e) {
                return 0;
            }
        });

        adapter.setItems(pastEvents);
    }
}
