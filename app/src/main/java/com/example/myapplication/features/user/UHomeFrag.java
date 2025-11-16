package com.example.myapplication.features.user;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * This class shows the user home screen.
 *
 * From teh home screen the user can click on an event in the lsit of events to
 * got the detailed view of that sepecified event, go to the scan view, user profile
 * view, or search and filter the events.
 */
public class UHomeFrag extends Fragment {

    private UserEventAdapter adapter;
    private EditText searchInput;
    private final List<UserEvent> allEvents = new ArrayList<>();
    private final List<String> selectedInterests = new ArrayList<>();

    public UHomeFrag() {
        super(R.layout.fragment_u_home);
    }

    /**
     * Sets up the event grid, search/filter controls, and kicks off the initial fetch.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchInput = view.findViewById(R.id.etSearchEvents);
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
                    List<UserEvent> displayable = filterEventsForDisplay(events, currentUserId);
                    allEvents.clear();
                    allEvents.addAll(displayable);
                    applyCurrentFilters();
                } else{
                    allEvents.clear();
                    adapter.submit(new ArrayList<>());
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
        menu.getMenu().add(getString(R.string.filter_interests_option));
        menu.getMenu().add(getString(R.string.filter_availability_option));
        menu.setOnMenuItemClickListener(item -> {
            CharSequence title = item.getTitle();
            if (TextUtils.equals(title, getString(R.string.filter_interests_option))) {
                showInterestsDialog();
                return true;
            } else if (TextUtils.equals(title, getString(R.string.filter_availability_option))) {
                Toast.makeText(requireContext(),
                        getString(R.string.filter_availability_placeholder),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void showInterestsDialog() {
        String[] options = getResources().getStringArray(R.array.event_theme_options);
        boolean[] checked = new boolean[options.length];
        for (int i = 0; i < options.length; i++) {
            checked[i] = selectedInterests.contains(options[i]);
        }

        final List<String> pendingSelection = new ArrayList<>(selectedInterests);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.filter_interests_title)
                .setMultiChoiceItems(options, checked, (dialog, which, isChecked) -> {
                    String option = options[which];
                    if (isChecked) {
                        if (!pendingSelection.contains(option)) {
                            pendingSelection.add(option);
                        }
                    } else {
                        pendingSelection.remove(option);
                    }
                })
                .setPositiveButton(R.string.filter_interests_apply, (dialog, which) -> {
                    selectedInterests.clear();
                    selectedInterests.addAll(pendingSelection);
                    applyCurrentFilters();
                    if (selectedInterests.isEmpty()) {
                        Toast.makeText(requireContext(),
                                R.string.filter_interests_showing_all,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),
                                getString(R.string.filter_interests_applied, summarizeInterests()),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton(R.string.filter_interests_clear, (dialog, which) -> {
                    selectedInterests.clear();
                    applyCurrentFilters();
                    Toast.makeText(requireContext(),
                            R.string.filter_interests_cleared_toast,
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String summarizeInterests() {
        return TextUtils.join(", ", selectedInterests);
    }

    private void applyCurrentFilters() {
        if (adapter == null) {
            return;
        }

        List<UserEvent> working = new ArrayList<>(allEvents);
        if (!selectedInterests.isEmpty()) {
            working = filterEventsByInterests(working, selectedInterests);
        }

        adapter.submit(working);

        if (searchInput != null) {
            CharSequence query = searchInput.getText();
            if (query != null && query.length() > 0) {
                adapter.filter(query.toString());
            }
        }
    }

    static List<UserEvent> filterEventsByInterests(List<UserEvent> events, List<String> interests) {
        List<UserEvent> filtered = new ArrayList<>();
        if (events == null || events.isEmpty()) {
            return filtered;
        }
        if (interests == null || interests.isEmpty()) {
            filtered.addAll(events);
            return filtered;
        }
        for (UserEvent event : events) {
            if (event == null) {
                continue;
            }
            String theme = event.getTheme();
            if (TextUtils.isEmpty(theme)) {
                continue;
            }
            for (String interest : interests) {
                if (!TextUtils.isEmpty(interest) && theme.equalsIgnoreCase(interest)) {
                    filtered.add(event);
                    break;
                }
            }
        }
        return filtered;
    }

    static List<UserEvent> filterEventsByAvailability(List<UserEvent> events, long startTime, long endTime) {
        List<UserEvent> filtered = new ArrayList<>();
        if (events == null) {
            return filtered;
        }
        for (UserEvent event : events) {
            if (event == null) {
                continue;
            }
            long eventStart = event.getStartTimeMillis();
            long eventEnd = event.getEndTimeMillis();
            if (eventStart >= startTime && eventEnd <= endTime) {
                filtered.add(event);
            }
        }
        return filtered;
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
