package com.example.myapplication.features.organizer;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.myapplication.features.user.UserEvent;
import com.example.myapplication.features.user.UserEventAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OHomeFrag extends Fragment {

    private UserEventAdapter adapter;

    public OHomeFrag() {
        super(R.layout.fragment_o_home);
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

        filterButton.setOnClickListener(this::showFilterMenu);
    }

    private void showFilterMenu(View anchor) {
        androidx.appcompat.widget.PopupMenu menu = new androidx.appcompat.widget.PopupMenu(requireContext(), anchor);
        menu.getMenu().add("Upcoming");
        menu.getMenu().add("Drafts");
        menu.getMenu().add("Archived");
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
                "ORG_PLACEHOLDER",
                "Your First Event",
                "Pick a venue",
                "You",
                "Add price",
                "Draft placeholder until you publish.",
                now + TimeUnit.DAYS.toMillis(3),
                ContextCompat.getColor(context, R.color.event_banner_charcoal)
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
