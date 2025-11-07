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

public class AUsersFrag extends Fragment {

    private ListenerRegistration reg;
    private AdminUserAdapter adapter;

    public AUsersFrag() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        return i.inflate(R.layout.fragment_a_users, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        RecyclerView rv = v.findViewById(R.id.rvUsers);
        EditText search  = v.findViewById(R.id.etSearchUsers);
        View empty       = v.findViewById(R.id.empty);

        // The reference screenshot for Users is a vertical list (single column)
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.addItemDecoration(new SpacingDecoration(dp(12)));
        rv.setClipToPadding(false);
        rv.setPadding(rv.getPaddingLeft(), rv.getPaddingTop(), rv.getPaddingRight(), dp(88));

        adapter = new AdminUserAdapter();
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
                        String role = safe(d.getString("role"));       // "User", "Organizer", "Admin", etc.
                        String roleLower = role.toLowerCase();
                        if (!(roleLower.equals("user") || roleLower.equals("organizer"))) {
                            // Only browse non-admin profiles per the story
                            continue;
                        }
                        String first = safe(d.getString("firstName"));
                        String last  = safe(d.getString("lastName"));
                        String name  = (first + " " + last).trim();
                        if (name.isEmpty()) name = safe(d.getString("name")); // fallback if your team uses 'name'

                        AdminUserAdapter.UserRow u = new AdminUserAdapter.UserRow();
                        u.id        = d.getId();
                        u.name      = name;
                        u.email     = safe(d.getString("email"));
                        u.role      = role; // keep original case for display
                        // Support either 'avatarUrl' or 'photoUrl' depending on your team:
                        String avatar = d.getString("avatarUrl");
                        if (avatar == null) avatar = d.getString("photoUrl");
                        u.avatarUrl = safe(avatar);

                        list.add(u);
                    }
                    adapter.submit(list);
                    empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reg != null) reg.remove();
    }

    private static String safe(String s){ return s == null ? "" : s; }

    private int dp(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int)(dp * density + 0.5f);
    }

    static class SpacingDecoration extends RecyclerView.ItemDecoration {
        private final int space;
        SpacingDecoration(int space){ this.space = space; }
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
