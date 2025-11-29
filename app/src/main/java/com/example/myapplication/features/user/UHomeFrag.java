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
import com.example.myapplication.features.user.home.UHomeController;
import com.example.myapplication.features.user.home.UHomeModel;
import com.example.myapplication.features.user.home.UHomeView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This class shows the user home screen.
 *
 * From teh home screen the user can click on an event in the lsit of events to
 * got the detailed view of that sepecified event, go to the scan view, user profile
 * view, or search and filter the events.
 */
public class UHomeFrag extends Fragment implements UHomeView {

    private UserEventAdapter adapter;
    private EditText searchInput;
    private UHomeController controller;
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

        UHomeModel model = new UHomeModel();
        FirebaseEventRepository repo = new FirebaseEventRepository();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        controller = new UHomeController(repo, auth, model, this);

        controller.loadEvents();

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
                controller.onSearchQueryChanged(s != null ? s.toString() : null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        filterButton.setOnClickListener(v -> showFilterMenu(v));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (controller != null) {
            controller.detachView();
        }
    }

    @Override
    public void showEvents(List<UserEvent> events, @Nullable String searchQuery) {
        adapter.submit(events);
        adapter.filter(searchQuery);
    }

    @Override
    public void showEmptyState(@Nullable String message) {
        adapter.submit(new ArrayList<>());
        if (message != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
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
        if (controller.hasAvailabilityFilter()) {
            menu.getMenu().add(getString(R.string.filter_availability_clear_option));
        }
        menu.getMenu().add(getString(R.string.filter_clear_all_option)); // New item
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
            } else if (TextUtils.equals(title, getString(R.string.filter_clear_all_option))) {
                controller.clearAllFilters();
                Toast.makeText(requireContext(), R.string.filter_all_cleared_toast, Toast.LENGTH_SHORT).show();
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
        List<String> currentInterests = controller.getSelectedInterests();
        boolean[] checked = new boolean[options.length];
        for (int i = 0; i < options.length; i++) {
            checked[i] = currentInterests.contains(options[i]);
        }

        final List<String> pendingSelection = new ArrayList<>(currentInterests);

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
                    controller.updateInterests(pendingSelection);
                    if (pendingSelection.isEmpty()) {
                        Toast.makeText(requireContext(),
                                R.string.filter_interests_showing_all,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),
                                getString(R.string.filter_interests_applied, summarizeInterests(pendingSelection)),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton(R.string.filter_interests_clear, (dialog, which) -> {
                    controller.updateInterests(new ArrayList<>());
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
            controller.updateAvailability(result.first, result.second);
            Toast.makeText(requireContext(),
                    getString(R.string.filter_availability_applied,
                            formatAvailabilityDate(result.first),
                            formatAvailabilityDate(result.second)),
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
        Long start = controller.getAvailabilityStartMillis();
        Long end = controller.getAvailabilityEndMillis();
        if (start == null || end == null) {
            return null;
        }
        return new Pair<>(start, end);
    }

    /**
     * Clears the availability date filter.
     * @param None
     * @return void
     */
    private void clearAvailabilityFilter() {
        if (!controller.hasAvailabilityFilter()) {
            return;
        }
        controller.clearAvailability();
        Toast.makeText(requireContext(),
                R.string.filter_availability_cleared_toast,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Summarizes the selected interests as a comma-separated string.
     * @param interests The list of interests to summarize.
     * @return Comma-separated list of selected interests.
     */
    private String summarizeInterests(List<String> interests) {
        return TextUtils.join(", ", interests);
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

