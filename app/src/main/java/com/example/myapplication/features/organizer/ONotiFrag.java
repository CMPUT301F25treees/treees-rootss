package com.example.myapplication.features.organizer;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.features.user.UserEvent;
import com.example.myapplication.features.user.UserEventAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ONotiFrag extends Fragment {

    private MaterialButton btnEvent, btnResendInvites, btnCustomNoti;

    private String selectedEventId = null;
    private String selectedEventName = "Please select an event!";

    private final FirebaseEventRepository repo = new FirebaseEventRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String organizerName = "Organizer";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_o_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnEvent = view.findViewById(R.id.btnEvent);
        btnResendInvites = view.findViewById(R.id.resendInvites);
        btnCustomNoti = view.findViewById(R.id.customNoti);
        btnEvent.setText(selectedEventName);

        btnEvent.setOnClickListener(v -> openEventPicker());

        btnCustomNoti.setOnClickListener(v -> {
            if (selectedEventId == null) {
                Toast.makeText(requireContext(), "Pick an event first.", Toast.LENGTH_SHORT).show();
                return;
            }
            promptAndSendCustomPush(selectedEventId);
        });
        preloadOrganizerName();
    }

    private void openEventPicker(){
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        repo.getAllEvents(new FirebaseEventRepository.EventListCallback() {
            @Override
            public void onEventsFetched(List<UserEvent> events) {
                if (events == null || events.isEmpty()) {
                    toast("No events.");
                    return;
                }

                List<UserEvent> myEvents = new ArrayList<>();
                for (UserEvent event : events) {
                    if (event != null && uid.equals(event.getOrganizerID())) {
                        myEvents.add(event);
                    }
                }
                if (myEvents.isEmpty()) { toast("No events created by you."); return; }

                CharSequence[] eventNames = new CharSequence[myEvents.size()];
                for (int i = 0; i < myEvents.size(); i++) {
                    String name = myEvents.get(i).getName();
                    eventNames[i] = name != null ? name : "(unnamed)";;
                }

                int checked = -1;
                if (selectedEventId != null) {
                    for (int i = 0; i < myEvents.size(); i++) {
                        if (selectedEventId.equals(myEvents.get(i).getId())) {
                            checked = i;
                            break;
                        }
                    }
                }
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Select Event")
                        .setSingleChoiceItems(eventNames, checked, null)
                        .setPositiveButton("Select", (d, w) -> {
                            androidx.appcompat.app.AlertDialog ad = (androidx.appcompat.app.AlertDialog) d;
                            int idx = ad.getListView().getCheckedItemPosition();
                            if (idx >= 0) {
                                UserEvent chosen = myEvents.get(idx);
                                selectedEventId = chosen.getId();
                                selectedEventName = chosen.getName() != null ? chosen.getName() : "(unnamed)";
                                btnEvent.setText(selectedEventName);
                            }
                            d.dismiss();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
            @Override public void onError(Exception e) {
                toast("Failed to fetch: " + e.getMessage());
            }
        });
    }
    private enum Audience { INVITED, WAITING, ALL, CANCELLED }
    private void promptAndSendCustomPush(String eventId) {
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Message...");


        final CharSequence[] audiences = {"Invited", "Waiting", "ALL", "CANCELLED"};
        final int[] chosen = {0};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Custom Notification")
                .setSingleChoiceItems(audiences, 0, (d, which) -> chosen[0] = which)
                .setView(input)
                .setPositiveButton("Send", (d, w) -> {
                    String msg = input.getText().toString().trim();
                    if (msg.isEmpty()) { toast("Message is empty."); return; }
                    Audience a = (chosen[0] == 0) ? Audience.INVITED
                            : (chosen[0] == 1) ? Audience.WAITING
                            : (chosen[0] == 2) ? Audience.ALL
                            : Audience.CANCELLED;
                    sendCustomPush(eventId, msg, a);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendCustomPush(String eventId, String message, Audience audience) {
        final String eventName = selectedEventName;

        db.collection("notificationList")
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs.isEmpty()) { toast("Recipient list not found."); return; }
                    var doc = qs.getDocuments().get(0);

                    List<String> recipients;
                    switch (audience) {
                        case ALL:
                            recipients = castStringList(doc.get("all"));
                            break;
                        case INVITED:
                            recipients = castStringList(doc.get("invited"));
                            break;
                        case WAITING:
                            recipients = castStringList(doc.get("waiting"));
                            break;
                        case CANCELLED:
                            recipients = castStringList(doc.get("cancelled"));
                            break;
                        default:
                            recipients = new ArrayList<>();
                    }

                    if (recipients == null || recipients.isEmpty()) { toast("No recipients found."); return; }

                    var payload = new java.util.HashMap<String, Object>();
                    payload.put("dateMade", Timestamp.now());
                    payload.put("event", eventName);
                    payload.put("from", organizerName);
                    payload.put("message", message);
                    payload.put("uID", recipients);

                    db.collection("notifications").add(payload)
                            .addOnSuccessListener(ref -> toast("Notification sent!"))
                            .addOnFailureListener(e -> toast("Failed to send notification: " + e.getMessage()));
                })
                .addOnFailureListener(e -> toast("Error: " + e.getMessage()));
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private List<String> castStringList(Object obj) {
        if (obj instanceof List<?>){
            List<String> out = new ArrayList<>();
            for (Object o : (List<?>) obj) if (o instanceof String) out.add((String) o);
            return out;
        }
        return new ArrayList<>();
    }

    private void preloadOrganizerName() {
        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String first = doc.getString("firstName");
                        String last  = doc.getString("lastName");
                        String full  = ((first != null ? first.trim() : "") + " " +
                                (last  != null ? last.trim()  : "")).trim();

                        // Optional other schemas:
                        if (full.isEmpty()) {
                            String name = doc.getString("name");        // if you store a single-name field
                            if (name != null && !name.trim().isEmpty()) full = name.trim();
                        }

                        if (!full.isEmpty()) organizerName = full;
                    }
                    // last resort: FirebaseAuth displayName
                    else if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                        organizerName = user.getDisplayName();
                    }
                })
                .addOnFailureListener(e -> {
                    if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                        organizerName = user.getDisplayName();
                    }
                });
    }
}

