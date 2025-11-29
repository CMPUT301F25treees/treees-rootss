package com.example.myapplication.features.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.data.model.Event;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class UWaitlistFrag extends Fragment implements UWaitlistAdapter.OnItemClickListener{

    private RecyclerView recyclerView;
    private UWaitlistAdapter adapter;
    private FirebaseEventRepository repo;
    private String curentUid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_u_waitlist, container, false);

        recyclerView = view.findViewById(R.id.waitlistRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UWaitlistAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

         repo = new FirebaseEventRepository();
         curentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

         loadWaitlistEvents();

         return view;
    }

    private void loadWaitlistEvents(){
        repo.getAllEvents(new FirebaseEventRepository.EventListCallback() {
            @Override
            public void onEventsFetched(List<UserEvent> events) {
                List<UserEvent> waitlistEvents = new ArrayList<>();

                for(UserEvent event : events){
                    List<String> waitlist = event.getWaitlist();

                    if(waitlist != null && waitlist.contains(curentUid)){
                        waitlistEvents.add(event);
                    }
                }
                adapter.setItems(waitlistEvents);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "There was an error loading the events", Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * This method will be used to navigate to the User Detail page of the
     * specified event.
     * @param event the event that was clicked
     */
    @Override
    public void onEventClick(UserEvent event){

    }



}
