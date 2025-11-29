package com.example.myapplication.features.user;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.data.model.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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
    private Long availabilityStartMillis;
    private Long availabilityEndMillis;
    private final SimpleDateFormat availabilityDateFormat =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    /**
     * Default constructor.
     * @param None
     * @return void
     */
    public UHomeFrag() {
        super(R.layout.fragment_u_home);
    }

    /**
     * Sets up the event grid, search/filter controls, and kicks off the initial fetch.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @return void
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
     * @param None
     * @return void
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
     * @param anchor The view to anchor the popup menu to.
     * @return void
     */
    private void showFilterMenu(View anchor) {
        androidx.appcompat.widget.PopupMenu menu = new androidx.appcompat.widget.PopupMenu(requireContext(), anchor);
        menu.getMenu().add(getString(R.string.filter_interests_option));
        menu.getMenu().add(getString(R.string.filter_availability_option));
        if (availabilityStartMillis != null && availabilityEndMillis != null) {
            menu.getMenu().add(getString(R.string.filter_availability_clear_option));
        }
        menu.setOnMenuItemClickListener(item -> {
            CharSequence title = item.getTitle();
            if (TextUtils.equals(title, getString(R.string.filter_interests_option))) {
                showInterestsDialog();
                return true;
            } else if (TextUtils.equals(title, getString(R.string.filter_availability_option))) {
                showAvailabilityPicker();
                return true;
            } else if (TextUtils.equals(title, getString(R.string.filter_availability_clear_option))) {
                clearAvailabilityFilter();
                return true;
            }
            return false;
        });
        menu.show();
    }

    /**
     * Shows a multi-choice dialog for selecting event interests to filter by.
     * @param None
     * @return void
     */
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

    /**
     * Shows a date range picker for selecting availability dates to filter by.
     * @param None
     * @return void
     */
    private void showAvailabilityPicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText(R.string.filter_availability_title);
        Pair<Long, Long> selection = getAvailabilitySelection();
        if (selection != null) {
            builder.setSelection(selection);
        }

        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(result -> {
            if (result == null || result.first == null || result.second == null) {
                return;
            }
            availabilityStartMillis = result.first;
            availabilityEndMillis = result.second;
            applyCurrentFilters();
            Toast.makeText(requireContext(),
                    getString(R.string.filter_availability_applied,
                            formatAvailabilityDate(availabilityStartMillis),
                            formatAvailabilityDate(availabilityEndMillis)),
                    Toast.LENGTH_SHORT).show();
        });
        picker.show(getParentFragmentManager(), "availability_range_picker");
    }

    /**
     * Gets the currently selected availability date range.
     * @param None
     * @return Pair of start and end millis, or null if not set.
     */
    @Nullable
    private Pair<Long, Long> getAvailabilitySelection() {
        if (availabilityStartMillis == null || availabilityEndMillis == null) {
            return null;
        }
        return new Pair<>(availabilityStartMillis, availabilityEndMillis);
    }

    /**
     * Clears the availability date filter.
     * @param None
     * @return void
     */
    private void clearAvailabilityFilter() {
        if (availabilityStartMillis == null && availabilityEndMillis == null) {
            return;
        }
        availabilityStartMillis = null;
        availabilityEndMillis = null;
        applyCurrentFilters();
        Toast.makeText(requireContext(),
                R.string.filter_availability_cleared_toast,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Summarizes the selected interests as a comma-separated string.
     * @param None
     * @return Comma-separated list of selected interests.
     */
    private String summarizeInterests() {
        return TextUtils.join(", ", selectedInterests);
    }

    /**
     * Formats a millis timestamp into a readable date string.
     * @param millis The timestamp in milliseconds.
     * @return Formatted date string.
     */
    private String formatAvailabilityDate(Long millis) {
        if (millis == null) {
            return "";
        }
        return availabilityDateFormat.format(millis);
    }

    /**
     * Returns the start of the day (00:00:00.000) for the given timestamp.
     * @param timeMillis The input timestamp in milliseconds.
     * @return Timestamp at the start of the day.
     */
    private long startOfDay(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Returns the end of the day (23:59:59.999) for the given timestamp.
     * @param timeMillis The input timestamp in milliseconds.
     * @return Timestamp at the end of the day.
     */
    private long endOfDay(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    /**
     *
     * First applies a filter to hide any events that have finished and then,
     * applies the currently selected filters to the full event list and updates the adapter.
     * @param None
     * @return void
     */
    private void applyCurrentFilters() {

        long now = System.currentTimeMillis();

        if (adapter == null) {
            return;
        }


        List<UserEvent> working = new ArrayList<>();

        for(UserEvent event : allEvents){
            if(event != null && isUpcomingEvent(event, now)){
                working.add(event);
            }
        }

        if (!selectedInterests.isEmpty()) {
            working = filterEventsByInterests(working, selectedInterests);
        }
        if (availabilityStartMillis != null && availabilityEndMillis != null) {
            long filterStart = startOfDay(availabilityStartMillis);
            long filterEnd = endOfDay(availabilityEndMillis);
            working = filterEventsByAvailability(working, filterStart, filterEnd);
        }

        adapter.submit(working);

        if (searchInput != null) {
            CharSequence query = searchInput.getText();
            if (query != null && query.length() > 0) {
                adapter.filter(query.toString());
            }
        }
    }

    /**
     * Filters events based on the provided list of interests.
     * @param events The list of events to filter.
     * @param interests The list of interests to filter by.
     * @return A list of events matching the interests.
     */
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


    /**
     * This is a helper method that returns a boolean value based on whether the event
     * draw date has passed or not
     *
     * @param event The event being checked
     * @param currentMillis the current time
     * @return boolean value if event draw has occured
     */
    static boolean isUpcomingEvent(@NonNull UserEvent event, long currentMillis){
        long draw = event.getSelectionDateMillis();

        return draw >= currentMillis;

    }

    /**
     * Filters events based on availability within the specified time range.
     * @param events The list of events to filter.
     * @param startTime The start of the availability range in milliseconds.
     * @param endTime The end of the availability range in milliseconds.
     * @return A list of events available within the specified time range.
     */
    static List<UserEvent> filterEventsByAvailability(List<UserEvent> events, long startTime, long endTime) {
        List<UserEvent> filtered = new ArrayList<>();
        if (events == null || events.isEmpty()) {
            return filtered;
        }
        long normalizedStart = Math.min(startTime, endTime);
        long normalizedEnd = Math.max(startTime, endTime);
        for (UserEvent event : events) {
            if (event == null) {
                continue;
            }
            long eventStart = event.getStartTimeMillis();
            long eventEnd = event.getEndTimeMillis();
            if (eventStart == 0 && eventEnd == 0) {
                continue;
            }
            long actualEnd = eventEnd > 0 ? eventEnd : eventStart;
            if (actualEnd >= normalizedStart && eventStart <= normalizedEnd) {
                filtered.add(event);
            }
        }
        return filtered;
    }



    /**
     * Returns a new list that excludes events owned by the provided user ID.
     * @param events The list of events to filter.
     * @param currentUserId The user ID to exclude events for.
     * @return A list of events not organized by the specified user.
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

        /**
         * Constructor for GridSpacingItemDecoration.
         *
         * @param spanCount The number of columns in the grid.
         * @param spacing   The spacing in pixels to apply between items.
         */
        GridSpacingItemDecoration(int spanCount, int spacing) {
            this.spanCount = spanCount;
            this.spacing = spacing;
        }

        /**
         * Calculates and applies the offsets for each item to maintain even spacing.
         *
         * @param outRect The Rect to receive the output.
         * @param view    The child view to decorate.
         * @param parent  The RecyclerView this ItemDecoration is decorating.
         * @param state   The current state of RecyclerView.
         */
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
