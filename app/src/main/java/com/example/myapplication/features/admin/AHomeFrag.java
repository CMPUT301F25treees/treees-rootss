package com.example.myapplication.features.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.features.user.UserEvent;
import com.example.myapplication.features.user.UserEventAdapter;

import java.util.List;

/**
 * Admin home fragment that displays a grid of events and photos.
 * <p>
 * This class implements the view layer of an MVC design for the admin
 * home screen. It is responsible for configuring the RecyclerView,
 * handling user interactions with the search bar and mode selector, and
 * delegating data loading and filtering to {@link AdminHomeController}.
 * The controller in turn delivers filtered {@link UserEvent} lists to
 * this fragment via the {@link AdminHomeView} interface.
 * <p>
 */
public class AHomeFrag extends Fragment implements AdminHomeView {

    private RecyclerView rvEvents;
    private EditText etSearchEvents;
    private ImageButton btnFilter;

    private UserEventAdapter adapter;
    private AdminHomeController controller;

    public AHomeFrag() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Admin reuses the user home layout
        return inflater.inflate(R.layout.fragment_u_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvEvents = view.findViewById(R.id.rvEvents);
        etSearchEvents = view.findViewById(R.id.etSearchEvents);
        btnFilter = view.findViewById(R.id.btnFilter);

        // Set up RecyclerView with grid layout and spacing
        rvEvents.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvEvents.addItemDecoration(new SpacingDecoration(dp(12), dp(12), dp(12)));
        rvEvents.setPadding(
                rvEvents.getPaddingLeft(),
                rvEvents.getPaddingTop(),
                rvEvents.getPaddingRight(),
                dp(72)
        );
        rvEvents.setClipToPadding(false);

        adapter = new UserEventAdapter();
        final NavController navController = NavHostFragment.findNavController(this);

        // Click behavior:
        // - EVENTS mode  -> navigate to admin event detail
        // - PHOTOS mode -> navigate directly to remove-options screen
        adapter.setOnEventClickListener(event -> {
            if (event == null || event.getId() == null || controller == null) {
                return;
            }
            Bundle args = new Bundle();
            args.putString("eventId", event.getId());

            AdminHomeMode mode = controller.getCurrentMode();
            if (mode == AdminHomeMode.EVENTS) {
                navController.navigate(R.id.navigation_admin_event_detail, args);
            } else {
                navController.navigate(R.id.navigation_admin_remove_options, args);
            }
        });

        rvEvents.setAdapter(adapter);

        // Controller (C in MVC)
        controller = new AdminHomeController(this);
        controller.start();

        // Search bar delegates query changes to the controller
        if (etSearchEvents != null) {
            etSearchEvents.setHint("Search events");
            etSearchEvents.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // no-op
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (controller != null) {
                        controller.onSearchQueryChanged(s != null ? s.toString() : "");
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // no-op
                }
            });
        }

        // Filter button shows a simple mode menu (Events / Photos)
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showModeMenu());
        }
    }

    @Override
    public void onDestroyView() {
        if (controller != null) {
            controller.stop();
            controller = null;
        }
        rvEvents = null;
        etSearchEvents = null;
        btnFilter = null;
        adapter = null;
        super.onDestroyView();
    }

    // ---------------------------------------------------------------------
    // AdminHomeView implementation (View in MVC)
    // ---------------------------------------------------------------------

    @Override
    public void showEvents(@NonNull List<UserEvent> events) {
        if (adapter != null) {
            adapter.submit(events);
        }
    }

    @Override
    public void showMode(@NonNull AdminHomeMode mode) {
        if (etSearchEvents != null) {
            // Reset query text when mode changes to keep behavior predictable
            etSearchEvents.setText("");
            if (mode == AdminHomeMode.EVENTS) {
                etSearchEvents.setHint("Search events");
            } else {
                etSearchEvents.setHint("Search photos");
            }
        }

        // Inform the admin of the current mode
        Toast.makeText(
                requireContext(),
                (mode == AdminHomeMode.EVENTS) ? "Events mode" : "Photos mode",
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public void showLoading(boolean loading) {
        // No dedicated progress bar for admin home yet; could be added if needed.
    }

    @Override
    public void showError(@NonNull String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private void showModeMenu() {
        if (btnFilter == null || controller == null) {
            return;
        }

        PopupMenu popup = new PopupMenu(requireContext(), btnFilter);
        // 1 = Events, 2 = Photos
        popup.getMenu().add(0, 1, 0, "Events");
        popup.getMenu().add(0, 2, 1, "Photos");

        popup.setOnMenuItemClickListener((MenuItem item) -> {
            int id = item.getItemId();
            if (id == 1) {
                controller.setMode(AdminHomeMode.EVENTS);
                return true;
            } else if (id == 2) {
                controller.setMode(AdminHomeMode.PHOTOS);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private int dp(int value) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    /**
     * Simple item decoration that applies symmetric spacing for a 2-column grid.
     */
    private static class SpacingDecoration extends RecyclerView.ItemDecoration {
        private final int horizontal;
        private final int vertical;
        private final int top;

        SpacingDecoration(int horizontal, int vertical, int top) {
            this.horizontal = horizontal;
            this.vertical = vertical;
            this.top = top;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect,
                                   @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int spanIndex = position % 2;

            outRect.top = (position < 2) ? top : vertical;
            outRect.left = (spanIndex == 0) ? horizontal : horizontal / 2;
            outRect.right = (spanIndex == 1) ? horizontal : horizontal / 2;
            outRect.bottom = vertical;
        }
    }
}
