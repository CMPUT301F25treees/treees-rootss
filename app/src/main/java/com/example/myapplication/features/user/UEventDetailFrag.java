package com.example.myapplication.features.user;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.bumptech.glide.Glide;

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

        MaterialButton joinWaitlistButton = view.findViewById(R.id.joinWaitlist);
        joinWaitlistButton.setOnClickListener(x -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(getContext(), "Please log in first!", Toast.LENGTH_SHORT).show();
                return;
            }
            String uid = user.getUid();

            FirebaseEventRepository repo = new FirebaseEventRepository();
            repo.joinWaitlist(eventId, uid, a -> {
                repo.fetchEventById(eventId, new FirebaseEventRepository.SingleEventCallback() {
                    @Override
                    public void onEventFetched(UserEvent event) {
                        int cap = event.getCapacity();
                        int drawn = event.getEntrantsToDraw();


                        new AlertDialog.Builder(requireContext())
                                .setTitle("You have joined the waitlist!")
                                .setMessage("The event has a waitlist capacity of " + cap + ", from which " +
                                        drawn + " will be drawn randomly. We promise.")
                                .setPositiveButton("Okay", (dialogInterface, i) -> dialogInterface.dismiss())
                                .show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "Unable to get event", Toast.LENGTH_SHORT).show();
                    }
                });
                refreshEventDetail(eventId);
            }, e-> Toast.makeText(getContext(), "Sorry, you could not join at the moment!", Toast.LENGTH_SHORT).show());
        });


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

        ImageView imageView = requireView().findViewById(R.id.eventImage);
        String imageUrl = event.getImageUrl();

        if(imageUrl != null && !imageUrl.isEmpty()){
            Glide.with(this)
                    .load(imageUrl)
                    .into(imageView);
        }
    }

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
}
