package com.example.myapplication.features.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.google.android.material.button.MaterialButton;

public class UEventDetailFrag extends Fragment {

    private TextView title, organizer, location, price, endTime, descr, waitingList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        return  inflater.inflate(R.layout.fragment_u_event_detail, container, false);
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

        MaterialButton backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(x -> {
            Navigation.findNavController(view).navigateUp();
        });

        String eventId = getArguments() != null ? getArguments().getString("eventId") : null;
        if(eventId != null){
            FirebaseEventRepository repo = new FirebaseEventRepository();
            repo.fetchEventById(eventId, new FirebaseEventRepository.SingleEventCallback() {
                @Override
                public void onEventFetched(UserEvent event) {
                    bindEventData(event);
                }

                @Override
                public void onError(Exception e) {

                }
            });
        }
    }

    private void bindEventData(UserEvent event) {
        long millisLeft = event.getEndTimeMillis() - System.currentTimeMillis();
        long daysLeft = (long) Math.ceil(millisLeft / (1000.0 * 60 * 60 * 24));

        title.setText(event.getName());
        organizer.setText("Organizer: " + event.getInstructor());
        location.setText(event.getLocation());
        price.setText("Price: " + event.getPrice());
        descr.setText(event.getDescr());
        endTime.setText("Days Left: " + Math.max(daysLeft, 0));
        waitingList.setText("Currently in Waitinglist: " +
                (event.getWaitlist() != null ? event.getWaitlist().size() : 0)
        );
    }
}
