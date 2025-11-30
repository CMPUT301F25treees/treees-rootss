package com.example.myapplication.features.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.features.user.UserEvent;
import com.example.myapplication.features.user.UserEventAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.navigation.NavController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import androidx.appcompat.widget.PopupMenu;

public class AHomeFrag extends Fragment {

    private ListenerRegistration reg;
    private UserEventAdapter adapter;

    // Admin-side state
    private final List<UserEvent> allEvents = new ArrayList<>();
    private final List<UserEvent> allImageEvents = new ArrayList<>();
    private final Map<String, DocumentSnapshot> eventDocsById = new HashMap<>();
    private String currentQuery = "";

    private enum Mode {
        EVENTS,
        PHOTOS
    }

    private Mode currentMode = Mode.EVENTS;

    private EditText etSearchEvents;
    private ImageButton btnFilter;

    public AHomeFrag() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Admin reuses the user home layout
        return inflater.inflate(R.layout.fragment_u_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvEvents = view.findViewById(R.id.rvEvents);
        etSearchEvents = view.findViewById(R.id.etSearchEvents);
        btnFilter = view.findViewById(R.id.btnFilter);

        // RecyclerView setup (same as before)
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

        // Click behavior depends on mode:
        // - EVENTS mode: go to admin event detail
        // - PHOTOS mode: go directly to remove-options screen
        adapter.setOnEventClickListener(event -> {
            if (event == null || event.getId() == null) {
                return;
            }
            Bundle args = new Bundle();
            args.putString("eventId", event.getId());
            if (currentMode == Mode.EVENTS) {
                navController.navigate(R.id.navigation_admin_event_detail, args);
            } else {
                navController.navigate(R.id.navigation_admin_remove_options, args);
            }
        });

        rvEvents.setAdapter(adapter);

        // Initial hint for events mode
        if (etSearchEvents != null) {
            etSearchEvents.setHint("Search Events");
        }

        // Admin search: we now handle filtering in this fragment, not via adapter.filter
        if (etSearchEvents != null) {
            etSearchEvents.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // no-op
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQuery = (s == null) ? "" : s.toString();
                    applyAdminFilter(currentQuery);
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // no-op
                }
            });
        }

        // Filter button -> dropdown menu (Events / Photos)
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showModeMenu());
        }

        // Firestore subscription: same query, but we cache docs and build two lists
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        reg = db.collection("events")
                .orderBy("startTimeMillis", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Optional: log error
                        return;
                    }

                    allEvents.clear();
                    allImageEvents.clear();
                    eventDocsById.clear();

                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            UserEvent event = doc.toObject(UserEvent.class);
                            if (event == null) {
                                continue;
                            }
                            event.setId(doc.getId());
                            allEvents.add(event);
                            eventDocsById.put(event.getId(), doc);

                            String imageUrl = safe(doc.getString("imageUrl"));
                            String posterUrl = safe(doc.getString("posterUrl"));
                            if (!imageUrl.isEmpty() || !posterUrl.isEmpty()) {
                                allImageEvents.add(event);
                            }
                        }
                    }

                    // Apply current search + mode to update the adapter
                    applyAdminFilter(currentQuery);
                });
    }

    @Override
    public void onDestroyView() {
        if (reg != null) {
            reg.remove();
            reg = null;
        }
        super.onDestroyView();
    }

    // === NEW: show dropdown menu for Events / Photos ===
    private void showModeMenu() {
        if (btnFilter == null) {
            return;
        }

        PopupMenu popup = new PopupMenu(requireContext(), btnFilter);
        // Simple IDs: 1 = Events, 2 = Photos
        popup.getMenu().add(0, 1, 0, "Events");
        popup.getMenu().add(0, 2, 1, "Photos");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                setMode(Mode.EVENTS);
                return true;
            } else if (id == 2) {
                setMode(Mode.PHOTOS);
                return true;
            }
            return false;
        });

        popup.show();
    }

    // === NEW: apply selected mode, update hint, and re-filter ===
    private void setMode(Mode mode) {
        if (currentMode == mode) {
            return;
        }
        currentMode = mode;

        if (etSearchEvents != null) {
            if (currentMode == Mode.EVENTS) {
                etSearchEvents.setHint("Search events");
            } else {
                etSearchEvents.setHint("Search Photos");
            }
            // Reset query when switching modes
            etSearchEvents.setText("");
        }
        currentQuery = "";

        Toast.makeText(
                requireContext(),
                (currentMode == Mode.EVENTS) ? "Events mode" : "Photos mode",
                Toast.LENGTH_SHORT
        ).show();

        applyAdminFilter(currentQuery);
    }

    // Central admin filter: uses mode + query
    private void applyAdminFilter(String query) {
        List<UserEvent> base = (currentMode == Mode.EVENTS) ? allEvents : allImageEvents;

        if (base == null || base.isEmpty()) {
            adapter.submit(Collections.emptyList());
            return;
        }

        if (query == null || query.trim().isEmpty()) {
            adapter.submit(new ArrayList<>(base));
            return;
        }

        String q = query.trim().toLowerCase(Locale.getDefault());
        List<UserEvent> filtered = new ArrayList<>();

        for (UserEvent event : base) {
            DocumentSnapshot doc = eventDocsById.get(event.getId());
            if (matchesQuery(doc, q)) {
                filtered.add(event);
            }
        }

        adapter.submit(filtered);
    }

    // Checks whether an event/doc matches the query
    private boolean matchesQuery(DocumentSnapshot doc, String query) {
        if (doc == null) {
            return false;
        }
        return contains(doc.getString("name"), query)
                || contains(doc.getString("location"), query)
                || contains(doc.getString("descr"), query)
                || contains(doc.getString("imageUrl"), query)
                || contains(doc.getString("posterUrl"), query);
    }

    private boolean contains(String value, String query) {
        return value != null
                && value.toLowerCase(Locale.getDefault()).contains(query);
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private int dp(int value) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

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
        public void getItemOffsets(@NonNull android.graphics.Rect outRect,
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
