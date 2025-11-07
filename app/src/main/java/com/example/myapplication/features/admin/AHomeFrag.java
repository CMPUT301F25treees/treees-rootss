package com.example.myapplication.features.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

/**
 * Admin Home – lists events and routes to admin actions (detail, remove options).
 * Mirrors the user home layout and reads /events (typically ordered by startTimeMillis).
 */
public class AHomeFrag extends Fragment {

    private ListenerRegistration reg;
    private UserEventAdapter adapter;

    public AHomeFrag() {}

    /**
     * Inflates the fragment layout (reuses the user home layout for consistent visuals).
     *
     * @param inflater  layout inflater
     * @param container parent container
     * @param savedInstanceState saved state
     * @return inflated view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_u_home, container, false);
    }

    /**
     * Initializes RecyclerView, search filtering, and Firestore snapshot subscription.
     *
     * @param v root view
     * @param savedInstanceState saved state, if any
     */
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        RecyclerView rv = v.findViewById(R.id.rvEvents);
        EditText search = v.findViewById(R.id.etSearchEvents);

        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.addItemDecoration(new SpacingDecoration(dp(12)));

        adapter = new UserEventAdapter();
        adapter.setOnEventClickListener(event -> {
            Bundle args = new Bundle();
            args.putString("eventId", event.getId());
            NavHostFragment.findNavController(this)
                    .navigate(R.id.navigation_admin_event_detail, args);
        });
        rv.setAdapter(adapter);

        if (search != null) {
            search.addTextChangedListener(new TextWatcher() {
                /**
                 * @param s     text before change
                 * @param start start index
                 * @param count count of old chars
                 * @param after count of new chars
                 */
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                /**
                 * Filters the list while the query text changes.
                 *
                 * @param s      current text
                 * @param start  start index
                 * @param before count of chars before
                 * @param count  count of new chars
                 */
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapter.filter(s == null ? "" : s.toString());
                }

                /**
                 * @param s editable after change
                 */
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        reg = db.collection("events")
                .orderBy("startTimeMillis", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;

                    List<UserEvent> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        UserEvent e = d.toObject(UserEvent.class);
                        if (e != null) {
                            try { e.setId(d.getId()); } catch (Exception ignored) {}
                            list.add(e);
                        }
                    }
                    adapter.submit(list);
                });
    }

    /**
     * Cleans up snapshot listener when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reg != null) reg.remove();
    }

    /**
     * Converts density-independent pixels to raw pixels.
     *
     * @param dp value in dp
     * @return pixel value
     */
    private int dp(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /**
     * ItemDecoration that adds symmetric spacing around event cards in the grid.
     */
    static class SpacingDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        /**
         * @param space spacing in pixels
         */
        SpacingDecoration(int space) { this.space = space; }

        /**
         * Adds spacing offsets around each item.
         *
         * @param outRect output rect to modify
         * @param view    child view
         * @param parent  RecyclerView
         * @param state   RecyclerView state
         */
        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect,
                                   @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            outRect.left = space / 2;
            outRect.right = space / 2;
            outRect.bottom = space;
            if (parent.getChildAdapterPosition(view) < 2) {
                outRect.top = space;
            }
        }
    }
}
