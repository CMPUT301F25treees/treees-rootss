// ANotiFrag.java (Admin)
package com.example.myapplication.features.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    private RecyclerView recyclerView;
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

        Button backButton = view.findViewById(R.id.bckButton);
        backButton.setOnClickListener(x -> {
            Navigation.findNavController(view).navigateUp();
        });

        Query query = FirebaseFirestore.getInstance()
                .collection("notifications");

        FirestoreRecyclerOptions<UNotiItem> options = new FirestoreRecyclerOptions.Builder<UNotiItem>()
                .setQuery(query, UNotiItem.class)
                .setLifecycleOwner(getViewLifecycleOwner())
                .build();

        adapter = new UNotiAdapter(options, snapshot -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("ADMIN DIALOG")
                    .setItems(new CharSequence[]{"Delete"}, (dialog, which) -> {
                        if (which == 0) {
                            snapshot.getReference().delete()
                                    .addOnSuccessListener(aVoid ->
                                            Toast.makeText(requireContext(), "Notification deleted", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(requireContext(), "Error deleting notification", Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
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
