package com.example.myapplication.features.user;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class shows the user home screen.
 *
 * From teh home screen the user can click on an event in the lsit of events to
 * got the detailed view of that sepecified event, go to the scan view, user profile
 * view, or search and filter the events.
 */
public class UHomeFrag extends Fragment {

    private UserEventAdapter adapter;

    public UHomeFrag() {
        super(R.layout.fragment_u_home);
    }

    /**
     * Sets up the event grid, search/filter controls, and kicks off the initial fetch.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText searchInput = view.findViewById(R.id.etSearchEvents);
        ImageButton filterButton = view.findViewById(R.id.btnFilter);
        RecyclerView eventsList = view.findViewById(R.id.rvEvents);

        adapter = new UserEventAdapter();
        Context context = requireContext();

        eventsList.setLayoutManager(new GridLayoutManager(context, 2));
        eventsList.setHasFixedSize(false);
        eventsList.setNestedScrollingEnabled(true);
        int spacing = getResources().getDimensionPixelSize(R.dimen.user_event_spacing);
        eventsList.addItemDecoration(new GridSpacingItemDecoration(2, spacing));
        eventsList.setAdapter(adapter);
        eventsList.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        fetchEventsFromFirestore();

        adapter.setOnEventClickListener(event -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getId());

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_to_user_event_detail, bundle);
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s != null ? s.toString() : null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        filterButton.setOnClickListener(v -> showFilterMenu(v));
    }

    /**
     * Loads all events from Firestore and filters out those owned by the current user.
     */
    private void fetchEventsFromFirestore(){
        FirebaseEventRepository repo = new FirebaseEventRepository();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        repo.getAllEvents(new FirebaseEventRepository.EventListCallback() {
            @Override
            public void onEventsFetched(List<UserEvent> events){
                if(events != null && !events.isEmpty()) {
                    adapter.submit(filterEventsForDisplay(events, currentUserId));
                } else{
                    Toast.makeText(requireContext(), "No events found", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(Exception e){
                Toast.makeText(requireContext(), "Failed to fetch:" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Placeholder filter menu that demonstrates how filtering options will surface.
     */
    private void showFilterMenu(View anchor) {
        androidx.appcompat.widget.PopupMenu menu = new androidx.appcompat.widget.PopupMenu(requireContext(), anchor);
        menu.getMenu().add("Option 1");
        menu.getMenu().add("Option 2");
        menu.getMenu().add("Option 3");
        menu.setOnMenuItemClickListener(item -> {
            Toast.makeText(requireContext(), item.getTitle() + " selected", Toast.LENGTH_SHORT).show();
            return true;
        });
        menu.show();
    }

    /**
     * Returns a new list that excludes events owned by the provided user ID.
     */
    static List<UserEvent> filterEventsForDisplay(List<UserEvent> events, String currentUserId) {
        List<UserEvent> filtered = new ArrayList<>();
        if (events == null || currentUserId == null) {
            return filtered;
        }
        for (UserEvent event : events) {
            if (event == null) {
                continue;
            }
            String organizerId = event.getOrganizerID();
            if (organizerId == null || !organizerId.equals(currentUserId)) {
                filtered.add(event);
            }
        }
        return filtered;
    }



    /**
     * Simple spacing decorator that keeps the event cards evenly spaced in the grid.
     */
    private static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spanCount;
        private final int spacing;

        GridSpacingItemDecoration(int spanCount, int spacing) {
            this.spanCount = spanCount;
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            outRect.left = spacing - column * spacing / spanCount;
            outRect.right = (column + 1) * spacing / spanCount;
            if (position >= spanCount) {
                outRect.top = spacing;
            }
            outRect.bottom = spacing;
        }
    }
}
