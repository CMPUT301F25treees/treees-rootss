package com.example.myapplication.features.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.data.model.Event;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a fragment that displays all the events that the logged-in user has joined the waitlist for.
 *
 * This fragment queries Firestore for all events, filters the ones where the user's id appears in the
 * waitlist, while also making sure that only future events are shown. If the user clicks on an event
 * it will take them to the detailed event page.
 */
public class UWaitlistFrag extends Fragment implements UWaitlistAdapter.OnItemClickListener{

    private RecyclerView recyclerView;
    private UWaitlistAdapter adapter;
    private FirebaseEventRepository repo;
    private String curentUid;

    /**
     * Inflates the waitlist fragment layout, initializes the UI components, the Firebase repo, and
     * load the user's waitlisted events
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return the root view of the inflated fragment layout
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_u_waitlist, container, false);

        View backButton = view.findViewById(R.id.bckButton);
        backButton.setOnClickListener(v->{
            Navigation.findNavController(view).navigateUp();
        });

        recyclerView = view.findViewById(R.id.recyclerEvents);
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

                    long drawDate = event.getSelectionDateMillis();
                    long current = System.currentTimeMillis();

                    if(drawDate < current){
                        continue;
                    }

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
        Bundle bundle = new Bundle();
        bundle.putString("eventId", event.getId());
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_to_user_event_detail, bundle);

    }



}
