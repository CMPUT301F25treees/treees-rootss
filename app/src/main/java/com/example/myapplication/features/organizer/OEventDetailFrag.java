package com.example.myapplication.features.organizer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.features.user.UserEvent;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class if for displaying the detailed information about an event for a user,
 * when they select an event from the OHomeFrag.
 *
 * It retrieves event data from the Firestore and then updates the view with that info.
 * This view also allows the user to navigate to different screens where they can edit
 * edit the event, or view the waiting list.
 */
public class OEventDetailFrag extends Fragment {
    private TextView title;
    private TextView organizer;
    private TextView location;
    private TextView price;
    private TextView startDate;
    private TextView descr;
    private TextView waitingList;
    private ImageView eventImage;
    private ImageView qrCodeImage;
    private String eventId;
    private final FirebaseEventRepository eventRepository = new FirebaseEventRepository();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    /**
     * This method inflates the layout for the fragment.
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return The View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        return  inflater.inflate(R.layout.fragment_o_event_details, container, false);
    }

    /**
     * This method gets called after the view has been created. Initializes FirebaseAuth and
     * Firestore instances, and sets up button click listeners.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return void
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){

        super.onViewCreated(view, savedInstanceState);

        title = view.findViewById(R.id.EventTitle);
        waitingList = view.findViewById(R.id.WaitinglistText);
        organizer = view.findViewById(R.id.OrganizerTitle);
        location = view.findViewById(R.id.addressText);
        price = view.findViewById(R.id.price);
        startDate = view.findViewById(R.id.startDateText);
        descr = view.findViewById(R.id.description);
        eventImage = view.findViewById(R.id.eventImage);
        qrCodeImage = view.findViewById(R.id.qrCodeImage);

        ImageButton backButton = view.findViewById(R.id.bckButton);
        backButton.setOnClickListener(x -> {
            Navigation.findNavController(view).navigateUp();
        });

        eventId = getArguments() != null ? getArguments().getString("eventId") : null;
        if (eventId == null) {
            Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
            return;
        }

        Button viewWaitlistButton = view.findViewById(R.id.viewWaitlist);
        viewWaitlistButton.setOnClickListener(btn -> {
                Bundle args = new Bundle();
                args.putString("eventId", eventId);
                Navigation.findNavController(view)
                        .navigate(R.id.navigation_organizer_waitlist, args);
        });

        Button editEventButton = view.findViewById(R.id.editEvent);
        editEventButton.setOnClickListener(button -> {
            Bundle args = new Bundle();
            args.putString("eventId", eventId);
            Navigation.findNavController(view)
                    .navigate(R.id.action_navigation_organizer_event_detail_to_navigation_organizer_event_edit, args);
        });

        refreshEventDetail(eventId);

        Button viewMapButton = view.findViewById(R.id.viewEntrantMap);
        viewMapButton.setOnClickListener(btn -> {
            Bundle args = new Bundle();
            args.putString("eventId", eventId);
            Navigation.findNavController(view)
                    .navigate(R.id.action_navigation_organizer_event_detail_to_navigation_organizer_entrant_map, args);
        });

    }

    /**
     * Changes all the UI elements of the xml to the specific event data.
     *
     * @param event The UserEvent object that holds all the details.
     * @return void
     */
    private void bindEventData(UserEvent event) {
        if (event == null) {
            Toast.makeText(requireContext(), "Unable to load event", Toast.LENGTH_SHORT).show();
            return;
        }
        title.setText(event.getName());
        location.setText(event.getLocation());
        updatePrice(event.getPriceDisplay());
        descr.setText(event.getDescr());
        updateStartDate(event.getStartTimeMillis());
        updateWaitingListCount(event);
        loadOrganizerInfo(event);
        loadEventImage(event);

        // QR Image View
        if (qrCodeImage != null) {
            String qrUrl = event.getQrData();
            if (!TextUtils.isEmpty(qrUrl)) {
                Glide.with(this)
                        .load(qrUrl)
                        .into(qrCodeImage);
            } else {
                qrCodeImage.setImageDrawable(null);
            }
        }
    }

    /**
     * Helper method that fetches the details From Firestore
     *
     * @param eventId event Id of the event to retrieve
     * @return void
     */
    private void refreshEventDetail(String eventId) {
        eventRepository.fetchEventById(eventId, new FirebaseEventRepository.SingleEventCallback() {
            @Override
            public void onEventFetched(UserEvent event) {
                if(!isAdded() || getView() == null){
                    return;
                }
                bindEventData(event);
            }

            @Override
            public void onError(Exception e) {
                if(!isAdded()){
                    return;
                }
                Toast.makeText(requireContext(), "Failed to load event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * This method makes sure that event details are refreshed.
     * @return void
     */
    @Override
    public void onResume() {
        super.onResume();
        if (eventId != null) {
            refreshEventDetail(eventId);
        }
    }

    /**
     * Updates displayed event price or null
     * @param priceText event price
     * @return void
     */
    private void updatePrice(String priceText) {
        if (TextUtils.isEmpty(priceText)) {
            price.setText(getString(R.string.price_unavailable));
        } else {
            price.setText(priceText);
        }
    }

    /**
     * Updates displayed date or null
     *
     * @param startMillis event start date
     * @return void
     */
    private void updateStartDate(long startMillis) {
        if (startMillis > 0) {
            String formatted = dateFormat.format(new Date(startMillis));
            startDate.setText(getString(R.string.start_date_value, formatted));
        } else {
            startDate.setText(getString(R.string.start_date_placeholder));
        }
    }

    /**
     * Updates waitling list count.
     *
     * @param event the event contianing the waitinglist.
     * @return void
     */
    private void updateWaitingListCount(UserEvent event) {
        int count = event.getWaitlist() != null ? event.getWaitlist().size() : 0;
        waitingList.setText(getString(R.string.waitinglist_text) + " " + count);
    }

    /**
     * This method retrieves and displays the organizers name and rating.
     *
     * @param event The specified event the name if from
     * @return void
     */
    private void loadOrganizerInfo(UserEvent event) {
        String fallback = extractFirstName(event.getInstructor());
        setOrganizerLabel(fallback);

        String organizerId = event.getOrganizerID();
        if (TextUtils.isEmpty(organizerId)) {
            return;
        }

        firestore.collection("users")
                .document(organizerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) {
                        return;
                    }
                    String firstName = doc.getString("firstName");
                    if (TextUtils.isEmpty(firstName)) {
                        firstName = extractFirstName(doc.getString("name"));
                    }
                    if (TextUtils.isEmpty(firstName)) {
                        firstName = extractFirstName(doc.getString("username"));
                    }
                    if (!TextUtils.isEmpty(firstName)) {
                        setOrganizerLabel(firstName);
                    }

                    Double rating = doc.getDouble("rating");
                    if (rating == null) rating = 0.0;
                    updateStars(rating);
                });
    }

    private void updateStars(double rating) {
        if (!isAdded() || getView() == null) return;

        int ratingInt = (int) Math.round(rating);
        int[] starIds = {R.id.star1, R.id.star2, R.id.star3, R.id.star4, R.id.star5};

        for (int i = 0; i < starIds.length; i++) {
            ImageView star = getView().findViewById(starIds[i]);
            if (star != null) {
                if (i < ratingInt) {
                    star.setImageResource(R.drawable.star_filled);
                } else {
                    star.setImageResource(R.drawable.star_empty);
                }
            }
        }
    }

    /**
     * This method loads the event image with Glide. If the image is not available
     * the ImageView is just cleared.
     *
     * @param event the specified event that image is for
     * @return void
     */
    private void loadEventImage(UserEvent event) {
        if (eventImage == null) {
            return;
        }
        String imageUrl = !TextUtils.isEmpty(event.getImageUrl())
                ? event.getImageUrl()
                : event.getPosterUrl();

        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(this)
                    .load(imageUrl)
                    .into(eventImage);
        } else {
            eventImage.setImageDrawable(null);
        }
    }

    /**
     * Updates organizer TextView with teh users firstname.
     *
     * @param firstName first name of the organizer
     * @return void
     */
    private void setOrganizerLabel(String firstName) {
        if (!TextUtils.isEmpty(firstName)) {
            organizer.setText(getString(R.string.organizer_name_format, firstName));
        } else {
            organizer.setText(getString(R.string.organizer_text));
        }
    }

    /**
     * Helper method to get teh first name.
     *
     * @param value the entire name string
     * @return First name, or null if not available.
     */
    private String extractFirstName(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int spaceIdx = trimmed.indexOf(' ');
        return spaceIdx > 0 ? trimmed.substring(0, spaceIdx) : trimmed;
    }
}
