package com.example.myapplication.features.organizer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class OEventDetailFrag extends Fragment {
    private TextView title;
    private TextView organizer;
    private TextView location;
    private TextView price;
    private TextView startDate;
    private TextView descr;
    private TextView waitingList;
    private ImageView eventImage;
    private String eventId;
    private final FirebaseEventRepository eventRepository = new FirebaseEventRepository();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        return  inflater.inflate(R.layout.fragment_o_event_details, container, false);
    }

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

        Button backButton = view.findViewById(R.id.backButton);
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
        viewWaitlistButton.setOnClickListener(btn ->
                Toast.makeText(requireContext(), "Waitlist view coming soon", Toast.LENGTH_SHORT).show()
        );

        Button editEventButton = view.findViewById(R.id.editEvent);
        editEventButton.setOnClickListener(button -> {
            Bundle args = new Bundle();
            args.putString("eventId", eventId);
            Navigation.findNavController(view)
                    .navigate(R.id.action_navigation_organizer_event_detail_to_navigation_organizer_event_edit, args);
        });

        refreshEventDetail(eventId);
    }

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
        loadOrganizerName(event);
        loadEventImage(event);
    }

    private void refreshEventDetail(String eventId) {
        eventRepository.fetchEventById(eventId, new FirebaseEventRepository.SingleEventCallback() {
            @Override
            public void onEventFetched(UserEvent event) {
                bindEventData(event);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Failed to load event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (eventId != null) {
            refreshEventDetail(eventId);
        }
    }

    private void updatePrice(String priceText) {
        if (TextUtils.isEmpty(priceText)) {
            price.setText(getString(R.string.price_unavailable));
        } else {
            price.setText(priceText);
        }
    }

    private void updateStartDate(long startMillis) {
        if (startMillis > 0) {
            String formatted = dateFormat.format(new Date(startMillis));
            startDate.setText(getString(R.string.start_date_value, formatted));
        } else {
            startDate.setText(getString(R.string.start_date_placeholder));
        }
    }

    private void updateWaitingListCount(UserEvent event) {
        int count = event.getWaitlist() != null ? event.getWaitlist().size() : 0;
        waitingList.setText(getString(R.string.waitinglist_text) + " " + count);
    }

    private void loadOrganizerName(UserEvent event) {
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
                });
    }

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

    private void setOrganizerLabel(String firstName) {
        if (!TextUtils.isEmpty(firstName)) {
            organizer.setText(getString(R.string.organizer_name_format, firstName));
        } else {
            organizer.setText(getString(R.string.organizer_text));
        }
    }

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
