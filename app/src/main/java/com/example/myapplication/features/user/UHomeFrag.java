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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UHomeFrag extends Fragment {

    private UserEventAdapter adapter;

    public UHomeFrag() {
        super(R.layout.fragment_u_home);
    }

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
        adapter.submit(buildDummyEvents(context));

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

    private List<UserEvent> buildDummyEvents(Context context) {
        long now = System.currentTimeMillis();
        List<UserEvent> events = new ArrayList<>();
        events.add(new UserEvent(
                "1",
                "Sunrise Yoga",
                "Willow Studio",
                "Jamie Lee",
                "$25",
                "NUll",
                now + TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(15),
                ContextCompat.getColor(context, R.color.event_banner_charcoal)
        ));
        events.add(new UserEvent(
                "2",
                "HIIT Express",
                "Loft Gym",
                "Marcus Chen",
                "$18",
                "NUll",
                now + TimeUnit.HOURS.toMillis(4),
                ContextCompat.getColor(context, R.color.event_banner_midnight)
        ));
        events.add(new UserEvent(
                "3",
                "Pilates Sculpt",
                "Harbor Club",
                "Aisha Gomez",
                "$32",
                "NUll",
                now + TimeUnit.HOURS.toMillis(6) + TimeUnit.MINUTES.toMillis(45),
                ContextCompat.getColor(context, R.color.event_banner_merlot)
        ));
        events.add(new UserEvent(
                "4",
                "Mindful Breathwork",
                "Riverside Park",
                "Omar Yusuf",
                "$12",
                "NUll",
                now + TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(5),
                ContextCompat.getColor(context, R.color.event_banner_forest)
        ));
        events.add(new UserEvent(
                "5",
                "Strength Fundamentals",
                "Forge Studio",
                "Kelly Bryant",
                "$28",
                "NUll",
                now + TimeUnit.HOURS.toMillis(9),
                ContextCompat.getColor(context, R.color.event_banner_charcoal)
        ));
        events.add(new UserEvent(
                "6",
                "Evening Stretch",
                "Zen Collective",
                "Priya Patel",
                "$14",
                "NUll",
                now + TimeUnit.HOURS.toMillis(12) + TimeUnit.MINUTES.toMillis(30),
                ContextCompat.getColor(context, R.color.event_banner_midnight)
        ));
        events.add(new UserEvent(
                "7",
                "Cycle Club",
                "Velocity Loft",
                "Nina Brooks",
                "$22",
                "NUll",
                now + TimeUnit.HOURS.toMillis(15),
                ContextCompat.getColor(context, R.color.event_banner_merlot)
        ));
        events.add(new UserEvent(
                "8",
                "Cardio Dance",
                "Studio Noir",
                "Leo Martins",
                "$20",
                "NUll",
                now + TimeUnit.HOURS.toMillis(18) + TimeUnit.MINUTES.toMillis(10),
                ContextCompat.getColor(context, R.color.event_banner_charcoal)
        ));
        events.add(new UserEvent(
                "9",
                "Mobility Reset",
                "Athlete Lab",
                "Sasha Kim",
                "$26",
                "NUll",
                now + TimeUnit.HOURS.toMillis(20) + TimeUnit.MINUTES.toMillis(45),
                ContextCompat.getColor(context, R.color.event_banner_forest)
        ));
        events.add(new UserEvent(
                "10",
                "Sunset Flow",
                "Skyline Terrace",
                "Jordan Blake",
                "$24",
                "NUll",
                now + TimeUnit.HOURS.toMillis(22),
                ContextCompat.getColor(context, R.color.event_banner_midnight)
        ));
        events.add(new UserEvent(
                "11",
                "Boxing Basics",
                "Corner Gym",
                "Riley Moore",
                "$19",
                "NUll",
                now + TimeUnit.HOURS.toMillis(24) + TimeUnit.MINUTES.toMillis(5),
                ContextCompat.getColor(context, R.color.event_banner_charcoal)
        ));
        events.add(new UserEvent(
                "12",
                "Restorative Yin",
                "Lotus Lounge",
                "Mira Das",
                "$18",
                "NUll",
                now + TimeUnit.HOURS.toMillis(26) + TimeUnit.MINUTES.toMillis(40),
                ContextCompat.getColor(context, R.color.event_banner_merlot)
        ));
        return events;
    }

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
