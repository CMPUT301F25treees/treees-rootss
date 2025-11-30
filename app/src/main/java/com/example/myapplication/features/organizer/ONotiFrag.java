package com.example.myapplication.features.organizer;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

/**
 * This class is responsible for managing organizer notifications and
 * certain event actions.
 */
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
        MaterialButton btnRunLottery = view.findViewById(R.id.btnRunLottery);
        btnEvent.setText(selectedEventName);

        btnEvent.setOnClickListener(v -> openEventPicker());

        btnCustomNoti.setOnClickListener(v -> {
            if (selectedEventId == null) {
                Toast.makeText(requireContext(), "Pick an event first.", Toast.LENGTH_SHORT).show();
                return;
            }
            promptAndSendCustomPush(selectedEventId);
        });

        btnRunLottery.setOnClickListener(v -> {
            if (selectedEventId == null) {
                Toast.makeText(requireContext(), "Pick an event first.", Toast.LENGTH_SHORT).show();
                return;
            }
            runLotteryForEvent(selectedEventId);
        });

        preloadOrganizerName();

        ImageButton backButton = view.findViewById(R.id.bckButton2);
        backButton.setOnClickListener(x -> {
            Navigation.findNavController(view).navigateUp();
        });

    }

    /**
     * Opens a custom picker with a list of the Users events which they can select.
     */
    private void openEventPicker() {

        repo.getAllEvents(new FirebaseEventRepository.EventListCallback() {
            @Override
            public void onEventsFetched(List<UserEvent> allEvents) {
                String uid = FirebaseAuth.getInstance().getUid();
                List<UserEvent> myEvents = new ArrayList<>();
                for (UserEvent event : allEvents) {
                    if (event != null && uid.equals(event.getOrganizerID())) {
                        myEvents.add(event);
                    }
                }

                if (myEvents.isEmpty()) {
                    Toast.makeText(requireContext(), "No events found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                View view = LayoutInflater.from(requireContext())
                        .inflate(R.layout.dialog_select_event, null);

                RecyclerView rv = view.findViewById(R.id.rvEvents);
                rv.setLayoutManager(new LinearLayoutManager(requireContext()));
                final int[] selected = {-1};

                OEventSelectAdapter adapter =
                        new OEventSelectAdapter(myEvents, -1, pos -> selected[0] = pos);

                rv.setAdapter(adapter);

                AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                        .setView(view)
                        .setPositiveButton("Select", (d, w) -> {
                            if (selected[0] >= 0) {
                                UserEvent event = myEvents.get(selected[0]);
                                selectedEventId = event.getId();
                                selectedEventName = event.getName();
                                btnEvent.setText(selectedEventName);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();

                dialog.setOnShowListener(dlg -> {
                    dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_role_switch_card);

                    int white = requireContext().getColor(android.R.color.white);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(white);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(white);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTypeface(null, android.graphics.Typeface.BOLD);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTypeface(null, android.graphics.Typeface.BOLD);
                });

                dialog.show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private enum Audience { INVITED, WAITING, ALL, CANCELLED }

    /**
     * This method prompts the user to provide a message and then select who
     * the target audience is.
     * @param eventId the event id of the Firestore Id of the specified event
     */
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

    /**
     * This method sends the custom message that the user created.
     *
     * A new document in the "notifications" collection is created
     *
     * @param eventId event ID the notification is for
     * @param message the content of the message
     * @param audience the chosen group for the notification
     */
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
                    payload.put("eventId", eventId);
                    payload.put("type", "custom");
                    payload.put("from", organizerName);
                    payload.put("message", message);
                    payload.put("uID", recipients);


                    db.collection("notifications").add(payload)
                            .addOnSuccessListener(ref -> toast("Notification sent!"))
                            .addOnFailureListener(e -> toast("Failed to send notification: " + e.getMessage()));
                })
                .addOnFailureListener(e -> toast("Error: " + e.getMessage()));
    }

    /**
     * This method displays a toast message.
     *
     * @param msg the contents of that message
     */
    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * This method safely casts an object to a list
     * @param obj the object is expectd to be an List<Strings>
     * @return a list of strings or an empty list
     */
    private List<String> castStringList(Object obj) {
        if (obj instanceof List<?>){
            List<String> out = new ArrayList<>();
            for (Object o : (List<?>) obj) if (o instanceof String) out.add((String) o);
            return out;
        }
        return new ArrayList<>();
    }

    /**
     * This method preloads teh organizer's name from the "users" collection on Firestore.
     */
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

    /**
     * This method runs the lottery for a specified event.
     *
     * Randomly will draw a given number of users from the waitlist. A confirmation is
     * required before draw is made.
     * @param eventId the event ID the draw is happening for
     */
    private void runLotteryForEvent(String eventId) {
        repo.fetchEventById(eventId, new FirebaseEventRepository.SingleEventCallback() {
            @Override
            public void onEventFetched(UserEvent event) {
                if (event.getWaitlist() == null || event.getWaitlist().isEmpty()) {
                    toast("No users in waitlist");
                    return;
                }

                int numToDraw = event.getEntrantsToDraw();
                if (numToDraw <= 0) numToDraw = event.getWaitlist().size();

                int finalNumToDraw = numToDraw;
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Run Lottery")
                        .setMessage("Draw " + finalNumToDraw + " winners from " +
                                event.getWaitlist().size() + " entrants?")
                        .setPositiveButton("Run Lottery", (d, w) -> {
                            repo.runLottery(eventId, event.getName(), event.getWaitlist(), finalNumToDraw,
                                    numWinners -> toast(numWinners + " winners selected and notified!"),
                                    e -> toast("Error: " + e.getMessage()));
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onError(Exception e) {
                toast("Error fetching event: " + e.getMessage());
            }
        });
    }
}

