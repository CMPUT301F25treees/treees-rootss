package com.example.myapplication.features.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
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

/**
 * Fragment that displays a list of past events for the currently logged-in user.
 * <p>
 * Past events are derived from entries in the {@code notificationList} collection
 * and event details from the {@code events} collection. Only events whose end
 * time has already passed are shown, sorted by most recent date first.
 */
public class UPastEventsFrag extends Fragment {

    /**
     * RecyclerView used to display the list of past events.
     */
    private RecyclerView recycler;

    /**
     * Adapter responsible for binding {@link UPastEventItem} instances
     * to the past events RecyclerView.
     */
    private UPastEventsAdapter adapter;

    /**
     * Firestore instance used for reading notification and event data.
     */
    private FirebaseFirestore db;

    /**
     * The currently authenticated Firebase user, or {@code null} if not signed in.
     */
    private FirebaseUser currentUser;

    /**
     * In-memory list of past event items to be displayed to the user.
     */
    private final List<UPastEventItem> pastEvents = new ArrayList<>();

    /**
     * Date formatter used to present event end dates in {@code MM/dd/yyyy} format.
     */
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    /**
     * Formats a timestamp in milliseconds into a displayable date string.
     *
     * @param millis the timestamp in milliseconds; may be {@code null}
     * @return formatted date string or an empty string if {@code millis} is {@code null}
     */
    private String formatDate(Long millis) {
        if (millis == null) return "";
        return DATE_FORMAT.format(new Date(millis));
    }

    /**
     * Default empty constructor required for Fragment instantiation.
     */
    public UPastEventsFrag() {}

    /**
     * Inflates the layout for the past events screen.
     *
     * @param inflater           the LayoutInflater used to inflate the view
     * @param container          optional parent view to attach to
     * @param savedInstanceState previously saved state, if any
     * @return the inflated root view for this fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_u_past_events, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * <p>
     * Sets up the RecyclerView and its adapter, initializes Firebase instances,
     * triggers loading of past events for the current user, and wires the back button.
     *
     * @param view               the root view returned by {@link #onCreateView}
     * @param savedInstanceState previously saved instance state, if any
     */
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

        ImageButton backButton = view.findViewById(R.id.bckButton);
        backButton.setOnClickListener(x -> {
            Navigation.findNavController(view).navigateUp();
        });

    }

    /**
     * Initiates a Firestore query to load all past events for the current user.
     * <p>
     * The query targets the {@code notificationList} collection and filters
     * entries where the user's UID is present in the {@code all} array field.
     * Results are forwarded to {@link #handlePastEventsResult(QuerySnapshot)}.
     */
    private void loadPastEvents() {
        pastEvents.clear();
        final String uid = currentUser.getUid();

        db.collection("notificationList")
                .whereArrayContains("all", uid)
                .get()
                .addOnSuccessListener(this::handlePastEventsResult);
    }

    /**
     * Processes the notification list query result to determine the user's status
     * for each event and then fetches detailed event information.
     *
     * @param notiSnapshot the query snapshot of notification list documents
     */
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

    /**
     * Fetches detailed information for a single event and adds it to the list of past events
     * if the event has already ended.
     * <p>
     * Once all outstanding event detail requests have completed (tracked via
     * {@link AtomicInteger} {@code remaining}), triggers {@link #sortAndShow()}.
     *
     * @param eventId   the ID of the event document to fetch
     * @param status    the user's status for this event (e.g., Accepted, Invited)
     * @param remaining counter of how many notification documents still need processing
     */
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

    /**
     * Sorts the collected past events by date in descending order (most recent first)
     * and updates the adapter with the sorted list.
     */
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
