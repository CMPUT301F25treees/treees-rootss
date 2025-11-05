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
    private String selectedEventName = "All Events";

    private final FirebaseEventRepository repo = new FirebaseEventRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();


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
    }

    private void openEventPicker(){
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        repo.getAllEvents(new FirebaseEventRepository.EventListCallback() {
            @Override
            public void onEventsFetched(List<UserEvent> events) {
                if (events != null) { toast("No events."); return; }

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

    private void promptAndSendCustomPush(String eventId) {
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Message to...");

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Custom Notification")
                .setView(input)
                .setPositiveButton("Send", (d, w) -> {
                    String msg = input.getText().toString().trim();
                    if (msg.isEmpty()) { toast("Message is empty."); return; }
                    sendCustomPush(eventId, msg);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void sendCustomPush(String eventId, String message) {
        String eventName= selectedEventName;
        final String myName = getMyDisplayName();

        db.collection("notificationList").document(eventId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                toast("Recipient list not found.");
                return;
            }

            List<String> all = castStringList(doc.get("all"));
            List<String> invited = castStringList(doc.get("invited"));
            List<String> recipients = !all.isEmpty() ? all : invited;

            if (recipients.isEmpty()) {
                toast("No recipients found.");
                return;
            }

            var payload = new java.util.HashMap<String, Object>();
            payload.put("dateMade", Timestamp.now());
            payload.put("event", eventName);
            payload.put("from", myName);
            payload.put("message", message);
            payload.put("uID", recipients);

            db.collection("notifications").add(payload)
                    .addOnSuccessListener(ref -> toast("Notification sent!"))
                    .addOnFailureListener(e -> toast("Failed to send notification: " + e.getMessage()));
        });
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

    private String getMyDisplayName() {
        var user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return "Organizer";
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) return user.getDisplayName();
        if (user.getEmail() != null && !user.getEmail().isEmpty()) return user.getEmail();
        return user.getUid();
    }

}

