// ANotiFrag.java (Admin)
package com.example.myapplication.features.admin;

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
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.features.user.UNotiAdapter;
import com.example.myapplication.features.user.UNotiItem;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * Administrative notifications/logs fragment.
 * <p>
 * Displays a list of notification items from {@code /notifications} using
 * {@link UNotiAdapter} backed by {@link FirestoreRecyclerOptions}. Provides an
 * admin dialog to delete individual notifications.
 */
public class ANotiFrag extends Fragment {

    /**
     *  The RecyclerView for displaying the notifications.
     */
    private RecyclerView recyclerView;

    /**
     *  The adapter for the RecyclerView.
     */
    private UNotiAdapter adapter;

    /**
     * Inflates the notifications layout for the admin view.
     *
     * @param inflater           the layout inflater
     * @param container          the parent view that the fragment's UI should be attached to
     * @param savedInstanceState previously saved instance state, if any
     * @return the inflated root view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_u_notifications, container, false);
    }

    /**
     * Initializes UI after the view is created: sets the title, configures the RecyclerView,
     * builds the Firestore query and options, and attaches the {@link UNotiAdapter}.
     *
     * @param view               the root view returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param savedInstanceState previously saved instance state, if any
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle("ADMIN Notifications");

        recyclerView = view.findViewById(R.id.notifications_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        SwitchCompat switchNotifications = view.findViewById(R.id.switch_notifications);
        if (switchNotifications != null) {
            switchNotifications.setVisibility(View.GONE);
        }

        ImageButton backButton = view.findViewById(R.id.bckButton);
        backButton.setOnClickListener(x -> {
            Navigation.findNavController(view).navigateUp();
        });

        Query query = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereNotEqualTo("message", "This notification was deleted by the system.")
                .orderBy("message")
                .orderBy("dateMade", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<UNotiItem> options = new FirestoreRecyclerOptions.Builder<UNotiItem>()
                .setQuery(query, UNotiItem.class)
                .setLifecycleOwner(getViewLifecycleOwner())
                .build();

        adapter = new UNotiAdapter(options, snapshot -> {
            UNotiItem item = snapshot.toObject(UNotiItem.class);

            View dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_invitation, null);

            TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
            TextView tvEvent = dialogView.findViewById(R.id.tvDialogEvent);
            TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
            MaterialButton btnAccept = dialogView.findViewById(R.id.btnAccept);
            MaterialButton btnDecline = dialogView.findViewById(R.id.btnDecline);
            MaterialButton btnViewEvent = dialogView.findViewById(R.id.btnViewEvent);

            tvTitle.setText("Admin Options");
            if (item != null) {
                String eventName = item.getEvent();   // or getEventName()
                tvEvent.setText(eventName != null ? eventName : "Notification");
            } else {
                tvEvent.setText("Notification");
            }
            tvMessage.setText("Do you want to delete this notification?");

            btnAccept.setVisibility(View.GONE);
            btnDecline.setVisibility(View.GONE);

            btnViewEvent.setText("Delete");

            var dialog = new MaterialAlertDialogBuilder(requireContext())
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            btnViewEvent.setOnClickListener(v -> {
                snapshot.getReference()
                        .update(
                                "message", "This notification was deleted by the system.",
                                "type", "custom"
                        )
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(requireContext(),
                                        "Notification marked as deleted", Toast.LENGTH_SHORT).show()
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(requireContext(),
                                        "Error deleting notification", Toast.LENGTH_SHORT).show()
                        );

                dialog.dismiss();
            });

            dialog.show();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(
                                android.graphics.Color.TRANSPARENT
                        )
                );
            }
        });
        recyclerView.setAdapter(adapter);

    }

    /**
     * Cleans up view-bound resources before the view is destroyed.
     * Detaches the adapter to avoid leaking the RecyclerView.
     */
    @Override
    public void onDestroyView() {
        recyclerView.setAdapter(null);
        super.onDestroyView();
    }
}
