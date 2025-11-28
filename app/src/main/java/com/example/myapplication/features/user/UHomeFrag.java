package com.example.myapplication.features.user;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
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
 * User home screen view that delegates data work to the MVC controller/model.
 */
public class UHomeFrag extends Fragment implements UHomeView {

    private UserEventAdapter adapter;
    private EditText searchInput;
    private UHomeController controller;
    private final SimpleDateFormat availabilityDateFormat =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    /**
     * Default constructor.
     */
    public UHomeFrag() {
        super(R.layout.fragment_u_home);
    }

    /**
     * Sets up the event grid, search/filter controls, and kicks off the initial fetch.
     *
     * @param view The View returned by {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, Bundle)}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchInput = view.findViewById(R.id.etSearchEvents);
        ImageButton filterButton = view.findViewById(R.id.btnFilter);
        RecyclerView eventsList = view.findViewById(R.id.rvEvents);

        adapter = new UserEventAdapter();
        Context context = requireContext();

        controller = new UHomeController(
                new FirebaseEventRepository(),
                FirebaseAuth.getInstance(),
                new UHomeModel(),
                this
        );

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

        adapter.setOnEventClickListener(event -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getId());

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_to_user_event_detail, bundle);
        });

        controller.loadEvents();

        if (searchInput != null) {
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
        }

        filterButton.setOnClickListener(this::showFilterMenu);
    }

    /**
     * Placeholder filter menu that demonstrates how filtering options will surface.
     *
     * @param anchor The view to anchor the popup menu to.
     */
    private void showFilterMenu(View anchor) {
        androidx.appcompat.widget.PopupMenu menu = new androidx.appcompat.widget.PopupMenu(requireContext(), anchor);
        menu.getMenu().add(getString(R.string.filter_interests_option));
        menu.getMenu().add(getString(R.string.filter_availability_option));
        if (controller.hasAvailabilityFilter()) {
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
     */
    private void showInterestsDialog() {
        String[] options = getResources().getStringArray(R.array.event_theme_options);
        boolean[] checked = new boolean[options.length];
        List<String> currentSelection = controller.getSelectedInterests();
        for (int i = 0; i < options.length; i++) {
            checked[i] = currentSelection.contains(options[i]);
        }

        final List<String> pendingSelection = new ArrayList<>(currentSelection);

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
                    List<String> applied = controller.getSelectedInterests();
                    if (applied.isEmpty()) {
                        Toast.makeText(requireContext(),
                                R.string.filter_interests_showing_all,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),
                                getString(R.string.filter_interests_applied, summarizeInterests(applied)),
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
     */
    private String summarizeInterests(List<String> interests) {
        return TextUtils.join(", ", interests);
    }

    /**
     * Formats a millis timestamp into a readable date string.
     */
    private String formatAvailabilityDate(@Nullable Long millis) {
        if (millis == null) {
            return "";
        }
        return availabilityDateFormat.format(millis);
    }

    @Override
    public void showEvents(List<UserEvent> events, @Nullable String searchQuery) {
        adapter.submit(events);
        String query = searchQuery == null ? "" : searchQuery;
        if (!TextUtils.isEmpty(query)) {
            adapter.filter(query);
        }
    }

    @Override
    public void showEmptyState(@Nullable String message) {
        adapter.submit(new ArrayList<>());
        if (!TextUtils.isEmpty(message)) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (controller != null) {
            controller.detachView();
        }
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
