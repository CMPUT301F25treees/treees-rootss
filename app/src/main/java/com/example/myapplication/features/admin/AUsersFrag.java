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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
/**
 * Admin “Browse Profiles” fragment.
 * <p>Shows a searchable list of non-admin users (User/Organizer), opens profile detail on tap,
 * and supports inline organizer demotion via a confirmation dialog.</p>
 */
public class AUsersFrag extends Fragment {

    private ListenerRegistration reg;
    private AdminUserAdapter adapter;

    /** Default no-argument constructor. */
    public AUsersFrag() {}

    /**
     * Inflates the admin users layout.
     *
     * @param i  layout inflater
     * @param c  optional parent container
     * @param b  saved instance state, if any
     * @return   the inflated root view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        return i.inflate(R.layout.fragment_a_users, c, false);
    }

    /**
     * Initializes list, search behavior, and Firestore snapshot subscription.
     *
     * @param v root view returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param b saved instance state, if any
     */
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        RecyclerView rv = v.findViewById(R.id.rvUsers);
        EditText search  = v.findViewById(R.id.etSearchUsers);
        View empty       = v.findViewById(R.id.empty);

        // Vertical list (matches your reference)
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.addItemDecoration(new SpacingDecoration(dp(12)));
        rv.setClipToPadding(false);
        rv.setPadding(rv.getPaddingLeft(), rv.getPaddingTop(), rv.getPaddingRight(), dp(88));

        adapter = new AdminUserAdapter();

        // open detail
        adapter.setOnUserClick(u -> {
            Bundle args = new Bundle();
            args.putString("uid", u.id);
            args.putString("name", u.name);
            args.putString("email", u.email);
            args.putString("role", u.role);
            args.putString("avatarUrl", u.avatarUrl);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.navigation_admin_user_detail, args);
        });

        // remove organizer
        adapter.setOnRemoveClick(u -> new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Remove organizer?")
                .setMessage("This will revoke organizer permissions for " +
                        (u.name == null || u.name.isEmpty() ? u.email : u.name) + ".")
                .setPositiveButton("Remove", (d, w) -> demoteOrganizer(u.id))
                .setNegativeButton("Cancel", null)
                .show());

        rv.setAdapter(adapter);

        if (search != null) {
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapter.filter(s == null ? "" : s.toString());
                    empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Firestore: read /users and adapt your schema (firstName, lastName, role)
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        reg = db.collection("users")
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;
                    List<AdminUserAdapter.UserRow> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String role = safe(d.getString("role"));  // "User" | "Organizer" | "Admin"
                        String r = role.toLowerCase();
                        if (!(r.equals("user") || r.equals("organizer"))) {
                            // only browse entrant/organizer
                            continue;
                        }
                        String first = safe(d.getString("firstName"));
                        String last  = safe(d.getString("lastName"));
                        String name  = (first + " " + last).trim();
                        if (name.isEmpty()) name = safe(d.getString("name")); // fallback

                        AdminUserAdapter.UserRow u = new AdminUserAdapter.UserRow();
                        u.id        = d.getId();
                        u.name      = name;
                        u.email     = safe(d.getString("email"));
                        u.role      = role;
                        String avatar = d.getString("avatarUrl");
                        if (avatar == null) avatar = d.getString("photoUrl");
                        u.avatarUrl = safe(avatar);

                        list.add(u);
                    }
                    adapter.submit(list);
                    empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                });
    }

    /**
     * Demotes an organizer to {@code role="User"} and sets {@code suspended=true}.
     *
     * @param uid user document id
     */
    private void demoteOrganizer(String uid){
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("role", "User", "suspended", true)
                .addOnSuccessListener(x -> android.widget.Toast.makeText(requireContext(),
                        "Organizer removed", android.widget.Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> android.widget.Toast.makeText(requireContext(),
                        "Failed: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show());
    }

    /** Removes the Firestore snapshot listener when the view is destroyed. */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reg != null) reg.remove();
    }

    /**
     * Null-safe helper for optional strings.
     *
     * @param s input string (may be {@code null})
     * @return empty string if {@code s} is null; otherwise {@code s}
     */
    private static String safe(String s){ return s == null ? "" : s; }

    /**
     * Converts density-independent pixels to raw pixels.
     *
     * @param dp value in dp
     * @return pixel value rounded to the nearest integer
     */
    private int dp(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int)(dp * density + 0.5f);
    }

    /** ItemDecoration that adds symmetrical spacing between list items. */
    static class SpacingDecoration extends RecyclerView.ItemDecoration {
        private final int space;
        /**
         * Creates a spacing decoration.
         *
         * @param space spacing in pixels
         */
        SpacingDecoration(int space){ this.space = space; }
        /**
         * Applies spacing offsets to each item.
         *
         * @param outRect output rectangle to receive offsets
         * @param view    child view
         * @param parent  RecyclerView containing the item
         * @param state   current RecyclerView state
         */
        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.left = space/2;
            outRect.right = space/2;
            outRect.bottom = space;
            if (parent.getChildAdapterPosition(view) == 0) outRect.top = space;
        }
    }
}
