package com.example.myapplication.features.user;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.features.user.UserEvent;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

/**
 * Displays event details for entrants, allows joining the waitlist,
 * and captures optional geolocation.
 */
public class UEventDetailFrag extends Fragment {

    private static final int LOCATION_REQUEST_CODE = 201;

    private TextView title, organizer, location, price, endTime, descr, waitingList;

    private MaterialButton joinWaitlistBtn;
    private boolean inWaitlist = false;

    private String eventId;

    // Whether geolocation is required for this event — set when binding event data
    private boolean geoRequired = false;

    private FusedLocationProviderClient fusedLocationClient;


    /**
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.  The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_u_event_detail, container, false);
    }


    /**
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        title = view.findViewById(R.id.EventTitle);
        waitingList = view.findViewById(R.id.WaitinglistText);
        organizer = view.findViewById(R.id.OrganizerTitle);
        location = view.findViewById(R.id.addressText);
        price = view.findViewById(R.id.price);
        endTime = view.findViewById(R.id.endTime);
        descr = view.findViewById(R.id.description);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Back button
        ImageButton backButton = view.findViewById(R.id.bckButton);
        backButton.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        if (eventId == null) {
            Toast.makeText(requireContext(), "Event not found.", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
            return;
        }

        // Load event details
        FirebaseEventRepository repo = new FirebaseEventRepository();
        repo.fetchEventById(eventId, new FirebaseEventRepository.SingleEventCallback() {
            @Override
            public void onEventFetched(UserEvent event) {
                bindEventData(event);

                // Geo requirement flag (if your UserEvent supports it)
                geoRequired = event.isGeoRequired();   // <-- Adjust if getter name differs
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Could not load event.", Toast.LENGTH_SHORT).show();
            }
        });


        // Join waitlist button
        joinWaitlistBtn = view.findViewById(R.id.joinWaitlist);
        joinWaitlistBtn.setOnClickListener(x -> {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(getContext(), "Please log in first!", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = user.getUid();

            if (inWaitlist){
                repo.leaveWaitlist(eventId, uid, v -> {
                    Toast.makeText(getContext(), "You have left the waitlist.", Toast.LENGTH_SHORT).show();

                    inWaitlist = false;
                    if (joinWaitlistBtn !=null){
                        joinWaitlistBtn.setText("Join Waitlist");
                    }
                    refreshEventDetail(eventId);
                }, e -> {
                    Toast.makeText(getContext(), "Could not leave waitlist", Toast.LENGTH_SHORT).show();
                });
            } else {
                if (geoRequired && !hasLocationPermission()) {
                    requestPermissions(
                            new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                            LOCATION_REQUEST_CODE
                    );
                    return;
                }
                captureLocationAndJoin(uid);
            }
        });
    }


    /** Bind event details to the UI */
    private void bindEventData(UserEvent event) {
        long millisLeft = event.getEndTimeMillis() - System.currentTimeMillis();
        long daysLeft = (long) Math.ceil(millisLeft / (1000.0 * 60 * 60 * 24));

        title.setText(event.getName());
        organizer.setText("Organizer: " + event.getInstructor());
        location.setText(event.getLocation());

        String priceText = event.getPriceDisplay();
        if (TextUtils.isEmpty(priceText)) {
            price.setText(getString(R.string.event_price_unavailable));
        } else {
            price.setText(getString(R.string.event_price_label, priceText));
        }

        descr.setText(event.getDescr());
        endTime.setText("Days Left: " + Math.max(daysLeft, 0));

        waitingList.setText("Currently in Waitinglist: " +
                (event.getWaitlist() != null ? event.getWaitlist().size() : 0));

        ImageView imageView = requireView().findViewById(R.id.eventImage);
        ImageView qrImageView = requireView().findViewById(R.id.qrCodeImage);

        if (event.getImageUrl() != null)
            Glide.with(this).load(event.getImageUrl()).into(imageView);

        if (event.getQrData() != null)
            Glide.with(this).load(event.getQrData()).into(qrImageView);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user!=null) ? user.getUid() : null;

        if( uid != null && event.getWaitlist() != null && event.getWaitlist().contains(uid)){
            inWaitlist = true;
            joinWaitlistBtn.setText("Leave Waitlist");
        } else {
            inWaitlist = false;
            joinWaitlistBtn.setText("Join Waitlist");
        }
    }


    /**
     * @param eventId
     */
    private void refreshEventDetail(String eventId) {
        FirebaseEventRepository repo = new FirebaseEventRepository();
        repo.fetchEventById(eventId, new FirebaseEventRepository.SingleEventCallback() {
            @Override
            public void onEventFetched(UserEvent event) {
                bindEventData(event);
            }

            @Override
            public void onError(Exception e) { }
        });
    }


    /**
     * @return
     */
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * @param uid
     */
    private void captureLocationAndJoin(String uid) {

        FirebaseEventRepository repo = new FirebaseEventRepository();

        // If location NOT required → join immediately
        if (!geoRequired || !hasLocationPermission()) {
            joinWithLocation(repo, uid, null, null);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        joinWithLocation(repo, uid,
                                location.getLatitude(),
                                location.getLongitude());
                    } else {
                        Toast.makeText(
                                getContext(),
                                "Could not get location. Try again outdoors.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Location failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }


    /**
     * @param repo
     * @param uid
     * @param lat
     * @param lng
     */
    private void joinWithLocation(FirebaseEventRepository repo,
                                  String uid,
                                  @Nullable Double lat,
                                  @Nullable Double lng) {

        repo.joinWaitlist(eventId, uid, lat, lng, a -> {

            repo.fetchEventById(eventId, new FirebaseEventRepository.SingleEventCallback() {
                @Override
                public void onEventFetched(UserEvent event) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("You have joined the waitlist!")
                            .setMessage("You have been added successfully.")
                            .setPositiveButton("Okay", (dialog, i) -> dialog.dismiss())
                            .show();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(),
                            "Unable to reload event.",
                            Toast.LENGTH_SHORT).show();
                }
            });

            refreshEventDetail(eventId);

        }, e -> Toast.makeText(getContext(),
                "Could not join waitlist.",
                Toast.LENGTH_SHORT).show());
    }


    /**
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *                     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null)
                    captureLocationAndJoin(user.getUid());

            } else {
                Toast.makeText(requireContext(),
                        "Location permission denied.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
