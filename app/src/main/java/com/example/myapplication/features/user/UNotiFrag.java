package com.example.myapplication.features.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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

        adapter = new UNotiAdapter(
                options,
                snapshot -> {
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("USER DIALOG")
                            .setItems(new CharSequence[]{"View Details"}, (dialog, which) -> {
                                if (which == 0) {
                                    Toast.makeText(requireContext(),
                                            "View details coming soon", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Close", (dialog, which) -> dialog.dismiss())
                            .show();
                },
                new UNotiAdapter.OnLotteryActionListener() {
                    @Override
                    public void onAccept(com.google.firebase.firestore.DocumentSnapshot snapshot, UNotiItem item) {
                        handleAcceptInvitation(snapshot.getId(), item);
                    }

                    @Override
                    public void onDecline(com.google.firebase.firestore.DocumentSnapshot snapshot, UNotiItem item) {
                        handleDeclineInvitation(snapshot.getId(), item);
                    }
                }
        );

        recyclerView.setAdapter(adapter);
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

    /**
     * Handles accepting a notification invitation.
     * Displays a confirmation dialog before performing the action via Firebase.
     *
     * @param notificationId The unique ID of the notification document.
     * @param item           The {@link UNotiItem} representing the invitation.
     */
    private void handleAcceptInvitation(String notificationId, UNotiItem item) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Accept Invitation")
                .setMessage("Are you sure you want to accept the invitation for \"" + item.getEvent() + "\"?")
                .setPositiveButton("Accept", (dialog, which) -> {
                    com.example.myapplication.data.firebase.FirebaseEventRepository repo =
                            new com.example.myapplication.data.firebase.FirebaseEventRepository();

                    repo.acceptInvitation(notificationId, item.getEventId(), uid,
                            aVoid -> Toast.makeText(requireContext(),
                                    "Invitation accepted!", Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(requireContext(),
                                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Handles declining a notification invitation.
     * Displays a confirmation dialog before performing the action via Firebase.
     *
     * @param notificationId The unique ID of the notification document.
     * @param item           The {@link UNotiItem} representing the invitation.
     */
    private void handleDeclineInvitation(String notificationId, UNotiItem item) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Decline Invitation")
                .setMessage("Are you sure you want to decline the invitation for \"" + item.getEvent() + "\"?")
                .setPositiveButton("Decline", (dialog, which) -> {
                    com.example.myapplication.data.firebase.FirebaseEventRepository repo =
                            new com.example.myapplication.data.firebase.FirebaseEventRepository();

                    repo.declineInvitation(notificationId, item.getEventId(), uid,
                            aVoid -> Toast.makeText(requireContext(),
                                    "Invitation declined.", Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(requireContext(),
                                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
