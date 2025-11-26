package com.example.myapplication.features.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.features.user.UNotiAdapter;
import com.example.myapplication.features.user.UNotiItem;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import androidx.navigation.fragment.NavHostFragment;
import androidx.appcompat.widget.SwitchCompat;

/**
 * Fragment responsible for displaying a list of notifications for the currently logged-in user.
 * <p>
 * This fragment retrieves user-specific notifications from the Firestore database,
 * orders them by the date they were created, and displays them using a RecyclerView
 * populated by a {@link UNotiAdapter}.
 */
public class UNotiFrag extends Fragment {

    /** RecyclerView for displaying user notifications. */
    private RecyclerView recyclerView;

    /** Adapter that binds Firestore notification data to the RecyclerView. */
    private UNotiAdapter adapter;

    FirebaseEventRepository repo = new FirebaseEventRepository();

    private boolean showPersonalNoti = false;

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater  The LayoutInflater object that can be used to inflate
     *                  any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's
     *                  UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being restored
     *                           from a previous saved state as given here.
     * @return The View for the fragment's UI, or {@code null}.
     */
    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_u_notifications, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has returned.
     * Sets up the RecyclerView, initializes the adapter, and defines user interactions for
     * accepting or declining invitations.
     *
     * @param view               The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle("USER Notifications");

        recyclerView = view.findViewById(R.id.notifications_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        SwitchCompat switchNotifications = view.findViewById(R.id.switch_notifications);

        ImageButton backButton = view.findViewById(R.id.bckButton);
        backButton.setOnClickListener(x -> {
            Navigation.findNavController(view).navigateUp();
        });

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
            UNotiItem item = snapshot.toObject(UNotiItem.class);
            if (item == null) return;

            boolean isInvitation = "lottery_win".equalsIgnoreCase(item.getType());

            if (isInvitation) {
                checkInvitationStatus(snapshot, item, uid);
            } else{
                showOtherOption(snapshot, item);
            }
        });

        recyclerView.setAdapter(adapter);

        switchNotifications.setChecked(true);
        adapter.setShowPersonalNoti(true);

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.setShowPersonalNoti(isChecked);
        });

    }

    /**
     * Called when the view previously created by {@link #onCreateView} is about to be destroyed.
     * Cleans up resources by detaching the adapter from the RecyclerView.
     */
    @Override
    public void onDestroyView() {
        recyclerView.setAdapter(null);
        super.onDestroyView();
    }

    private void checkInvitationStatus(DocumentSnapshot notificationSnapshot,
                                       UNotiItem item,
                                       String uid) {
        String eventId = item.getEventId();

        FirebaseFirestore.getInstance()
                .collection("notificationList")
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        DocumentSnapshot doc = qs.getDocuments().get(0);

                        @SuppressWarnings("unchecked")
                        java.util.List<String> invited =
                                (java.util.List<String>) doc.get("invited");
                        @SuppressWarnings("unchecked")
                        java.util.List<String> finalUsers =
                                (java.util.List<String>) doc.get("final");
                        @SuppressWarnings("unchecked")
                        java.util.List<String> cancelled =
                                (java.util.List<String>) doc.get("cancelled");

                        boolean isInvited  = invited != null && invited.contains(uid);
                        boolean isFinal    = finalUsers != null && finalUsers.contains(uid);
                        boolean isCancelled = cancelled != null && cancelled.contains(uid);

                        if (isFinal) {
                            showAlreadyAcceptedOption(notificationSnapshot, item);
                        } else if (isInvited) {
                            showInviteOption(notificationSnapshot, item);
                        } else if (isCancelled) {
                            showAlreadyDeclinedOption(notificationSnapshot, item);
                        } else {
                            showOtherOption(notificationSnapshot, item);
                        }
                    } else {
                        showOtherOption(notificationSnapshot, item);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(),
                            "Error checking invitation status",
                            Toast.LENGTH_SHORT).show();
                    showOtherOption(notificationSnapshot, item);
                });
    }

    private void showInviteOption(DocumentSnapshot snapshot, UNotiItem item){
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_invitation, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvEvent = dialogView.findViewById(R.id.tvDialogEvent);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        MaterialButton btnAccept = dialogView.findViewById(R.id.btnAccept);
        MaterialButton btnDecline = dialogView.findViewById(R.id.btnDecline);
        MaterialButton btnViewEvent = dialogView.findViewById(R.id.btnViewEvent);

        tvTitle.setText("Invitation Options");
        tvEvent.setText(item.getEvent());
        tvMessage.setText("You have been invited to this event.");

        var dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnAccept.setOnClickListener(v -> {
            acceptInvite(snapshot, item);
            dialog.dismiss();
        });

        btnDecline.setOnClickListener(v -> {
            declineInvite(snapshot, item);
            dialog.dismiss();
        });

        btnViewEvent.setOnClickListener(v -> {
            viewEvent(item);
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            );
        }
    }

    private void showAlreadyAcceptedOption(DocumentSnapshot snapshot, UNotiItem item) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_invitation, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvEvent = dialogView.findViewById(R.id.tvDialogEvent);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        MaterialButton btnAccept = dialogView.findViewById(R.id.btnAccept);
        MaterialButton btnDecline = dialogView.findViewById(R.id.btnDecline);
        MaterialButton btnViewEvent = dialogView.findViewById(R.id.btnViewEvent);

        tvTitle.setText("You have already accepted this invite");
        tvEvent.setText(item.getEvent());
        tvMessage.setText("You can still view the event details below.");

        btnAccept.setVisibility(View.GONE);
        btnDecline.setVisibility(View.GONE);

        var dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnViewEvent.setOnClickListener(v -> {
            viewEvent(item);
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            );
        }
    }

    private void showAlreadyDeclinedOption(DocumentSnapshot snapshot, UNotiItem item) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_invitation, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvEvent = dialogView.findViewById(R.id.tvDialogEvent);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        MaterialButton btnAccept = dialogView.findViewById(R.id.btnAccept);
        MaterialButton btnDecline = dialogView.findViewById(R.id.btnDecline);
        MaterialButton btnViewEvent = dialogView.findViewById(R.id.btnViewEvent);

        tvTitle.setText("You have already declined this invite");
        tvEvent.setText(item.getEvent());
        tvMessage.setText("You can still view the event details if you like.");

        btnAccept.setVisibility(View.GONE);
        btnDecline.setVisibility(View.GONE);

        var dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnViewEvent.setOnClickListener(v -> {
            viewEvent(item);
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            );
        }
    }

    private void showOtherOption(DocumentSnapshot snapshot, UNotiItem item){
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_invitation, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvEvent = dialogView.findViewById(R.id.tvDialogEvent);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        MaterialButton btnAccept = dialogView.findViewById(R.id.btnAccept);
        MaterialButton btnDecline = dialogView.findViewById(R.id.btnDecline);
        MaterialButton btnViewEvent = dialogView.findViewById(R.id.btnViewEvent);

        tvTitle.setText("Personal Notification");
        tvEvent.setText(item.getEvent());
        tvMessage.setText(item.getMessage() != null ? item.getMessage()
                : "View event details.");

        btnAccept.setVisibility(View.GONE);
        btnDecline.setVisibility(View.GONE);

        var dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnViewEvent.setOnClickListener(v -> {
            viewEvent(item);
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            );
        }
    }

    private void viewEvent(UNotiItem item){
        String eventId = item.getEventId();
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Event information is missing for this notification",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("eventId", eventId);

        NavHostFragment.findNavController(this)
                .navigate(R.id.navigation_user_event_detail, bundle);

    }
    private void acceptInvite(DocumentSnapshot snapshot, UNotiItem item){
        String notificationId = snapshot.getId();
        String eventId = item.getEventId();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        repo.acceptInvitation(notificationId, eventId, userId,
                v -> Toast.makeText(requireContext(), "Invitation accepted", Toast.LENGTH_SHORT).show(),
                e -> Toast.makeText(requireContext(), "Error accepting invitation", Toast.LENGTH_SHORT).show());
    }

    private void declineInvite(DocumentSnapshot snapshot, UNotiItem item){
        String notificationId = snapshot.getId();
        String eventId = item.getEventId();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        repo.declineInvitation(notificationId, eventId, userId,
                v -> Toast.makeText(requireContext(), "Invitation declined", Toast.LENGTH_SHORT).show(),
                e -> Toast.makeText(requireContext(), "Error declining invitation", Toast.LENGTH_SHORT).show());
    }
}
