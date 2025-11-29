package com.example.myapplication.features.organizer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.ExportHelper;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
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

/**
 * Fragment that allows an event organizer to view and manage participants
 * on both the waiting list and final list for a specific event.
 * <p>
 * Organizers can view current participants, draw random entrants
 * from the waiting list to the final list (lottery system),
 * and switch between lists using a popup selector.
 */
public class OEventListFrag extends Fragment {

    /** Firestore field name representing the waiting list. */
    private static final String FIELD_WAITING = "waiting";

    /** Firestore field name representing the finalized list. */
    private static final String FIELD_FINAL = "final";

    /**
     * Firestore field name representing the invited list
     */
    private static final String FIELD_INVITED = "invited";

    /**
     * Firestore field name representing the canceled list
     */
    private static final String FIELD_CANCELLED = "canceled";

    /**
     * Represents the current list being displayed:
     * either the waiting list or the final list.
     */
    private enum ListMode { WAITING, INVITED, CANCELED, FINAL }

    /** Current view mode of the fragment (WAITING or FINAL). */
    private ListMode currentMode = ListMode.WAITING;

    /** Event document ID retrieved from fragment arguments. */
    private String eventId;

    /** Notification document ID used for waitlist operations. */
    private String notificationDocId;

    /** Name of the event currently being viewed. */
    private String eventName = "";

    /** Repository for Firebase event operations. */
    private final FirebaseEventRepository eventRepo = new FirebaseEventRepository();

    /** RecyclerView displaying participant names. */
    private RecyclerView waitlistRecycler;

    /** Text view shown when there are no entries to display. */
    private TextView emptyState;

    /** Button used to trigger a draw from the waiting list. */
    private MaterialButton drawBtn;


    /** Adapter backing the RecyclerView of participant names. */
    private OEventListAdapter adapter;

    /** Firestore database instance. */
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Random generator used for lottery selection (handled in repository). */
    private final Random rng = new Random();

    /** Ordered list of participant UIDs currently displayed. */
    private final List<String> currentUids = new ArrayList<>();

    /** Map of UID → Display Name for participants. */
    private final Map<String, String> nameByUid = new HashMap<>();

    /** Maximum allowed participants in the event. */
    private long capacity = Long.MAX_VALUE;

    /** Number of entrants to draw during a lottery. */
    private long entrantsToDraw = 1;

    /** Epoch counter to handle asynchronous data consistency. */
    private int dataEpoch = 0;

    /**
     * This si a list of names that is visible in the view. This is added to make
     * exporting much easier.
     */
    private final List<String> displayedNames = new ArrayList<>();

    /**
     * This is a helper that handles the CSV exporting.
     */
    private ExportHelper exportHelper;

    /**
     * Inflates the layout for the organizer event list fragment.
     *
     * @param inflater  LayoutInflater to inflate the layout XML.
     * @param container Optional parent container.
     * @param savedInstanceState Previously saved fragment state, if any.
     * @return The inflated fragment view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_o_event_waitlist, container, false);
    }

    /**
     * Called after the view has been created. Initializes UI components,
     * loads event metadata, and sets up click listeners.
     *
     * @param view The root view of the fragment.
     * @param savedInstanceState The saved instance state, if any.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        exportHelper = new ExportHelper(this);

        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        waitlistRecycler = view.findViewById(R.id.waitlistRecycler);
        emptyState = view.findViewById(R.id.emptyState);
        drawBtn = view.findViewById(R.id.btnDraw);
        MaterialButtonToggleGroup statusToggleGroup = view.findViewById(R.id.statusToggleGroup);
        MaterialButton exportButton = view.findViewById(R.id.btnExport);

        statusToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == R.id.tab_waitlist) {
                currentMode = ListMode.WAITING;
            } else if (checkedId == R.id.tab_invited) {
                currentMode = ListMode.INVITED;
            } else if (checkedId == R.id.tab_canceled) {
                currentMode = ListMode.CANCELED;
            } else if (checkedId == R.id.tab_final) {
                currentMode = ListMode.FINAL;
            }

            loadListForCurrentMode();
        });

        statusToggleGroup.check(R.id.tab_waitlist);

        ImageButton backButton = view.findViewById(R.id.bckButton);
        backButton.setOnClickListener(x -> {
            Navigation.findNavController(view).navigateUp();
        });


        waitlistRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        waitlistRecycler.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        );

        adapter = new OEventListAdapter();
        waitlistRecycler.setAdapter(adapter);

        adapter.setOnItemLongClickListener(position -> {
            if (position < 0 || position >= currentUids.size()) {
                return;
            }

            String uid = currentUids.get(position);
            String name = nameByUid.getOrDefault(uid, uid);

            if (currentMode == ListMode.WAITING) {
                showRemoveFromWaitlistDialog(uid, name, position);
            } else if (currentMode == ListMode.INVITED) {
                showRemoveFromInvitedDialog(uid, name, position);
            } else {
                Toast.makeText(
                        requireContext(),
                        "You can only remove users from the waitlist or invited list.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        showEmpty();

        if (eventId != null) {
            loadEventMeta();
            loadListForCurrentMode();
        }

        exportButton.setOnClickListener(v -> onExportClicked());
        drawBtn.setOnClickListener(v -> drawApplicants());

    }

    /**
     * Loads event metadata (capacity, entrants to draw, and name)
     * from Firestore for the current event.
     */
    private void loadEventMeta() {
        final int myEpoch = ++dataEpoch;

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (myEpoch != dataEpoch) return;
                    if (doc != null && doc.exists()) {
                        Long cap = doc.getLong("capacity");
                        Long toDraw = doc.getLong("entrantsToDraw");
                        if (cap != null) capacity = cap;
                        if (toDraw != null) entrantsToDraw = Math.max(1, toDraw);
                        String n = doc.getString("name");
                        if (n != null) eventName = n;
                    }
                });
    }

    /**
     * Loads and displays the list of user IDs corresponding to
     * the current mode (waiting, invited, canceled, or final list) for this event.
     */
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

                    String field = FIELD_WAITING;
                    if(currentMode == ListMode.WAITING){
                        field = FIELD_WAITING;
                    } else if (currentMode == ListMode.INVITED){
                        field = FIELD_INVITED;
                    } else if (currentMode == ListMode.CANCELED){
                        field = FIELD_CANCELLED;
                    } else if (currentMode == ListMode.FINAL){
                        field = FIELD_FINAL;
                    }

                    List<String> ids = (List<String>) doc.get(field);

                    if (ids == null || ids.isEmpty()) {
                        currentUids.clear();
                        nameByUid.clear();
                        applyNames(myEpoch, new ArrayList<>());
                        return;
                    }

                    // Keep order and remove duplicates
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

    /**
     * Fetches the display names of users based on their UIDs from Firestore.
     *
     * @param epoch   Epoch identifier to prevent outdated updates.
     * @param userIds List of user IDs to fetch names for.
     */
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
                        if (epoch != dataEpoch) return;
                        String name = resolveName(userDoc, uid);
                        nameByUid.put(uid, name);
                        if (done.incrementAndGet() == total && epoch == dataEpoch) {
                            applyCurrentMapping(epoch);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (epoch != dataEpoch) return;
                        nameByUid.put(uid, uid);
                        if (done.incrementAndGet() == total && epoch == dataEpoch) {
                            applyCurrentMapping(epoch);
                        }
                    });
        }
    }

    /**
     * Applies the mapping of UIDs to display names and updates the RecyclerView.
     *
     * @param epoch Current data epoch.
     */
    private void applyCurrentMapping(int epoch) {
        List<String> names = new ArrayList<>(currentUids.size());
        for (String uid : currentUids) {
            names.add(nameByUid.getOrDefault(uid, uid));
        }
        applyNames(epoch, names);
    }

    /**
     * Resolves a readable display name for a user.
     *
     * @param userDoc     Firestore document of the user.
     * @param fallbackUid UID to use if the name cannot be determined.
     * @return The user's full name or UID as a fallback.
     */
    private String resolveName(DocumentSnapshot userDoc, String fallbackUid) {
        if (userDoc != null && userDoc.exists()) {
            String first = userDoc.getString("firstName");
            String last = userDoc.getString("lastName");
            String full = (first != null ? first : "") + (last != null ? " " + last : "");
            if (!full.trim().isEmpty()) return full.trim();
        }
        return fallbackUid;
    }

    /**
     * Applies a list of user names to the adapter and updates
     * UI elements such as empty states and draw button visibility.
     *
     * @param epoch Current data epoch for synchronization.
     * @param names List of participant names to display.
     */
    private void applyNames(int epoch, List<String> names) {
        if (epoch != dataEpoch || !isAdded()) return;

        displayedNames.clear();
        displayedNames.addAll(names);

        adapter.setNames(names);
        waitlistRecycler.setVisibility(View.VISIBLE);

        boolean empty = names.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);

        String emptyText;
        switch (currentMode) {
            case WAITING:
                emptyText = "No one on the waitlist.";
                break;
            case INVITED:
                emptyText = "No one on the invited list.";
                break;
            case CANCELED:
                emptyText = "No one on the canceled list.";
                break;
            case FINAL:
            default:
                emptyText = "No one on the final list.";
                break;
        }
        emptyState.setText(emptyText);

        boolean enableDraw = (currentMode == ListMode.WAITING) && !currentUids.isEmpty();
        drawBtn.setEnabled(enableDraw);
    }

    /**
     * Displays an empty state message when there are no users in the list.
     */
    private void showEmpty() {
        if (!isAdded()) return;
        waitlistRecycler.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.VISIBLE);

        String emptyText;
        switch (currentMode) {
            case WAITING:
                emptyText = "No one in the waitlist.";
                break;
            case INVITED:
                emptyText = "No one in the invited list.";
                break;
            case CANCELED:
                emptyText = "No one in the canceled list.";
                break;
            case FINAL:
            default:
                emptyText = "No one in the final list.";
                break;
        }
        emptyState.setText(emptyText);
        drawBtn.setEnabled(false);
    }

    /**
     * Initiates the drawing process to randomly select participants
     * from the waiting list for final invitations.
     */
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
                            Toast.makeText(requireContext(),
                                    "Could not fetch event name: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show()
                    );
        } else {
            runLotteryNow();
        }
    }

    /**
     * Executes the lottery draw by delegating to {@link FirebaseEventRepository#runLottery}
     * and displays the result to the organizer. The list is also requeried so that the view
     * stays updated.
     */
    private void runLotteryNow() {
        int toSelect = (int) Math.max(1, entrantsToDraw);

        eventRepo.runLottery(
                eventId,
                eventName,
                new ArrayList<>(currentUids),
                toSelect,
                selectedCount -> {
                    if (!isAdded()) return;

                    Toast.makeText(
                            requireContext(),
                            "Invited " + selectedCount + " user(s). Notifications sent for \"" + eventName + "\".",
                            Toast.LENGTH_LONG
                    ).show();
                    loadListForCurrentMode();
                },
                e -> {
                    if (!isAdded()) return;

                    Toast.makeText(
                            requireContext(),
                            "Draw failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }
        );
    }

    /**
     * Handles the exporting after export button is clicked
     *
     * The current list mode is determined and then using the ExportHelper methods
     * a CSV is created and saved to the devices file storage.
     */
    private void onExportClicked() {
            String modeLabel;
            switch (currentMode) {
                case WAITING:
                    modeLabel = "waiting_list";
                    break;
                case INVITED:
                    modeLabel = "invited_list";
                    break;
                case CANCELED:
                    modeLabel = "canceled_list";
                    break;
                case FINAL:
                default:
                    modeLabel = "final_list";
                    break;
            }

            exportHelper.exportNamesCsv(eventName, modeLabel, displayedNames);
    }

    /**
     * This method shows the user a confirmation dialog to confirm the removal of an entrant
     * from the waitlist
     *
     * @param uid the uid of the user being removed
     * @param name the name of the user
     * @param position the adapter position of the user
     */
    private void showRemoveFromWaitlistDialog(String uid, String name, int position){
        if(!isAdded()){
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Remove entrant from waitlist")
                .setMessage("Are you sure you want to remove " + name )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove",  (dialog, which) ->
                    removeFromWaitlist(uid, position)).show();
    }

    /**
     * This method shows a confirmation dialog to remove an entrant from the invited list.
     *
     * @param uid      the uid of the user being removed
     * @param name     the name of the user
     * @param position the adapter position of the user
     */
    private void showRemoveFromInvitedDialog(String uid, String name, int position) {
        if (!isAdded()) {
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Remove entrant from invited list")
                .setMessage("Are you sure you want to remove " + name + " from the invited list?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (dialog, which) ->
                        removeFromInvited(uid, position))
                .show();
    }

    /**
     * This is a helper method that uses the FirebaseEventRepository to remove a user from the
     * event waitlist and updates the local lists for the UI on operation succession
     *
     * @param uid the uid of the user being removed
     * @param position the adapter position of the suer in the list.
     */
    private void removeFromWaitlist(String uid, int position){
        if(eventId == null){
            Toast.makeText(requireContext(), "Event not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        eventRepo.leaveWaitlist(eventId, uid, unused -> {
            if(position >= 0 && position < currentUids.size()) {
                currentUids.remove(position);
            } if (position >= 0 && position < displayedNames.size()){
                displayedNames.remove(position);
            }

            adapter.removeAt(position);

            if(currentUids.isEmpty()){
                showEmpty();
            }

            Toast.makeText(requireContext(), "Removed from waitlist.", Toast.LENGTH_SHORT).show();

        }, e -> Toast.makeText(requireContext(), "Failed to remove.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Helper method that uses the FirebaseEventRepository to remove a user from the
     * invited list and updates the local lists and UI on success.
     *
     * @param uid      the UID of the user being removed
     * @param position the adapter position of the user in the list
     */
    private void removeFromInvited(String uid, int position) {
        if (eventId == null) {
            Toast.makeText(requireContext(), "Event not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        eventRepo.leaveInvitedList(eventId, uid, unused -> {
            if (position >= 0 && position < currentUids.size()) {
                currentUids.remove(position);
            }
            if (position >= 0 && position < displayedNames.size()) {
                displayedNames.remove(position);
            }

            adapter.removeAt(position);

            if (currentUids.isEmpty()) {
                showEmpty();
            }

            Toast.makeText(requireContext(), "Removed from invited list.", Toast.LENGTH_SHORT).show();
        }, e -> Toast.makeText(requireContext(), "Failed to remove.", Toast.LENGTH_SHORT).show());
    }
}
