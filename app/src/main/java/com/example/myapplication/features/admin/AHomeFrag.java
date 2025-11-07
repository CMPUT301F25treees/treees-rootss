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
 * Admin Home fragment that lists events and routes to administrative actions.
 * Reuses the user home layout and subscribes to {@code /events} updates.
 */
public class AHomeFrag extends Fragment {

    private ListenerRegistration reg;
    private UserEventAdapter adapter;

    /**
     * Default constructor.
     */
    public AHomeFrag() {}

    /**
     * Inflates the user home layout for consistent UI between user and admin views.
     *
     * @param inflater           layout inflater
     * @param container          parent container
     * @param savedInstanceState saved state, if any
     * @return the inflated root view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_u_home, container, false);
    }

    /**
     * Initializes RecyclerView, search handling, and Firestore snapshot subscription.
     *
     * @param v                  root view returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param savedInstanceState saved state, if any
     */
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

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
                 * No-op before text changes.
                 *
                 * @param s     text before change
                 * @param start start index
                 * @param count number of characters before change
                 * @param after number of characters that will be added
                 */
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                /**
                 * Filters the event list as the user types.
                 *
                 * @param s       current text
                 * @param start   start index
                 * @param before  number of characters replaced
                 * @param count   number of characters added
                 */
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapter.filter(s == null ? "" : s.toString());
                }

                /**
                 * No-op after text changes.
                 *
                 * @param s editable text after change
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
     * Cleans up resources tied to the fragment view lifecycle.
     * Removes the Firestore listener when the view is destroyed.
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
     * @return pixel value rounded to the nearest integer
     */
    private int dp(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /**
     * ItemDecoration that adds symmetric spacing between grid items.
     */
    static class SpacingDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        /**
         * Creates a spacing decoration.
         *
         * @param space spacing in pixels
         */
        SpacingDecoration(int space) { this.space = space; }

        /**
         * Applies spacing offsets to each item.
         *
         * @param outRect output rectangle to receive the offsets
         * @param view    child view
         * @param parent  RecyclerView containing the item
         * @param state   current RecyclerView state
         */
        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.left = space / 2;
            outRect.right = space / 2;
            outRect.bottom = space;
            if (parent.getChildAdapterPosition(view) < 2) {
                outRect.top = space;
            }
        }
    }
}
