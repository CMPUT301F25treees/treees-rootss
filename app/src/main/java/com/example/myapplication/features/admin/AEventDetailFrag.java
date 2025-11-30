package com.example.myapplication.features.admin;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Admin event detail fragment that displays information about a single event.
 * <p>
 * This fragment acts as the view for an event selected from the admin
 * home screen. It reuses the user event detail layout and replaces
 * the primary call-to-action with a "Remove" button that routes to
 * the admin remove-options screen.
 * <p>
 */
public class AEventDetailFrag extends Fragment {

    private String eventId;

    /**
     * Default no-argument constructor.
     */
    public AEventDetailFrag() {}

    /**
     * Inflates the user event detail layout to maintain consistent look-and-feel.
     *
     * @param inflater  the {@link LayoutInflater} used to inflate views
     * @param container the parent view that the fragment's UI should attach to
     * @param savedInstanceState previously saved state, if any
     * @return the inflated root {@link View} for this fragment
     */
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_u_event_detail, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView}. Wires UI elements, sets up the
     * admin “Remove” action, back navigation, and loads event data from Firestore.
     *
     * @param v the root view returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param b previously saved state, or {@code null}
     */
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);
        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        final TextView tvTitle     = v.findViewById(R.id.EventTitle);
        final TextView tvOrganizer = v.findViewById(R.id.OrganizerTitle);
        final TextView tvLocation  = v.findViewById(R.id.addressText);
        final TextView tvPrice     = v.findViewById(R.id.price);
        final TextView tvEndTime   = v.findViewById(R.id.endTime);
        final TextView tvDescr     = v.findViewById(R.id.description);
        final TextView tvWaiting   = v.findViewById(R.id.WaitinglistText);
        final ImageView ivHeader   = v.findViewById(R.id.eventImage);

        final MaterialButton btnRemove = v.findViewById(R.id.joinWaitlist);
        btnRemove.setText("Remove");
        btnRemove.setAllCaps(false);
        btnRemove.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.dodo_maroon)));
        btnRemove.setOnClickListener(x -> {
            Bundle args = new Bundle();
            args.putString("eventId", eventId);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.navigation_admin_remove_options, args);
        });

        ImageButton backButton = v.findViewById(R.id.bckButton);
        backButton.setOnClickListener(x -> {
            Navigation.findNavController(v).navigateUp();
        });


        FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                .addOnSuccessListener(d -> bindEventDoc(d, tvTitle, tvOrganizer, tvLocation,
                        tvPrice, tvEndTime, tvDescr, tvWaiting, ivHeader))
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Binds fields from the provided event {@link DocumentSnapshot} into the UI controls.
     *
     * @param d          the Firestore snapshot for {@code /events/{eventId}}
     * @param tvTitle    TextView for the event title
     * @param tvOrganizer TextView for the organizer label/value
     * @param tvLocation TextView for the location/address
     * @param tvPrice    TextView for the price
     * @param tvEndTime  TextView for “Days Left”/end time display
     * @param tvDescr    TextView for the event description
     * @param tvWaiting  TextView showing the waiting list count
     * @param ivHeader   ImageView for the header/preview image
     */
    private void bindEventDoc(DocumentSnapshot d,
                              TextView tvTitle, TextView tvOrganizer, TextView tvLocation,
                              TextView tvPrice, TextView tvEndTime, TextView tvDescr,
                              TextView tvWaiting, ImageView ivHeader) {

        if (!d.exists()) return;

        String name       = d.getString("name");
        String descr      = d.getString("descr");
        String location   = d.getString("location");
        String instructor = d.getString("instructor");
        String imageUrl   = d.getString("imageUrl");
        Number price      = d.get("price") instanceof Number ? (Number) d.get("price") : null;
        Number endMillis  = d.get("endTimeMillis") instanceof Number ? (Number) d.get("endTimeMillis") : null;
        List<?> waitlist  = (List<?>) d.get("waitlist");

        tvTitle.setText(name != null ? name : "");
        tvDescr.setText(descr != null ? descr : "");
        tvLocation.setText(location != null ? location : "");
        tvOrganizer.setText(instructor != null ? ("Organizer: " + instructor) : "Organizer");

        if (price != null) {
            tvPrice.setText(String.format("$%.2f", price.doubleValue()));
        }

        if (endMillis != null) {
            long diff = endMillis.longValue() - System.currentTimeMillis();
            long daysLeft = (long) Math.ceil(diff / (1000.0 * 60 * 60 * 24));
            tvEndTime.setText("Days Left: " + Math.max(0, daysLeft));
        }

        tvWaiting.setText("Currently in Waiting list: " + (waitlist != null ? waitlist.size() : 0));

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(ivHeader).load(imageUrl).into(ivHeader);
        }
    }
}
