package com.example.myapplication.features.organizer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.features.user.UserEvent;

public class OEventDetailFrag extends Fragment {
    private TextView title, organizer, location, price, endTime, descr, waitingList;
    private String eventId;
    private final FirebaseEventRepository eventRepository = new FirebaseEventRepository();

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
        endTime = view.findViewById(R.id.endTime);
        descr = view.findViewById(R.id.description);

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
                (event.getWaitlist() != null ? event.getWaitlist().size() : 0)
        );
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
}
