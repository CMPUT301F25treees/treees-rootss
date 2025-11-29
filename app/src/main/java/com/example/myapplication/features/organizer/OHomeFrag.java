package com.example.myapplication.features.organizer;

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
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.features.organizer.home.OHomeController;
import com.example.myapplication.features.organizer.home.OHomeModel;
import com.example.myapplication.features.organizer.home.OHomeView;
import com.example.myapplication.features.user.UserEvent;
import com.example.myapplication.features.user.UserEventAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * Organizer home view that delegates data and filtering to the controller/model.
 */
public class OHomeFrag extends Fragment implements OHomeView {

    private UserEventAdapter adapter;
    private EditText searchInput;
    private OHomeController controller;

    public OHomeFrag() {
        super(R.layout.fragment_o_home);
    }

    /**
     * Configures the organizer event grid, search/filter UI, and item click navigation.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchInput = view.findViewById(R.id.etSearchEvents);
        ImageButton filterButton = view.findViewById(R.id.btnFilter);
        RecyclerView eventsList = view.findViewById(R.id.rvEvents);

        adapter = new UserEventAdapter();
        Context context = requireContext();

        controller = new OHomeController(
                new FirebaseEventRepository(),
                FirebaseAuth.getInstance(),
                new OHomeModel(),
                this
        );

        NavController navController = NavHostFragment.findNavController(this);

        adapter.setOnEventClickListener(event -> {
            if (event == null || event.getId() == null) {
                Toast.makeText(requireContext(), "Unable to open event", Toast.LENGTH_SHORT).show();
                return;
            }
            Bundle args = new Bundle();
            args.putString("eventId", event.getId());
            navController.navigate(R.id.action_navigation_organizer_home_to_navigation_organizer_event_detail, args);
        });

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

        controller.loadOrganizerEvents();

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
     * Displays a stub filter menu for future categorization of organizer events.
     */
    private void showFilterMenu(View anchor) {
        androidx.appcompat.widget.PopupMenu menu = new androidx.appcompat.widget.PopupMenu(requireContext(), anchor);
        menu.getMenu().add("Upcoming");
        menu.getMenu().add("Drafts");
        menu.getMenu().add("Archived");
        menu.setOnMenuItemClickListener(item -> {
            controller.onFilterSelected(item.getTitle().toString());
            return true;
        });
        menu.show();
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
    public void showEmptyState(String message) {
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
    public void showInfo(String message) {
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
     * Simple spacing decorator that maintains even padding around grid tiles.
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
         * Calculates the offsets for each item in the grid to ensure even spacing.
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
