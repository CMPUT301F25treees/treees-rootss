package com.example.myapplication.features.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.List;

/**
 * Admin "Browse Profiles" fragment that lists non-admin users.
 * <p>
 * This fragment acts as the view in an MVC design for the admin user
 * management screen. It configures the RecyclerView, handles navigation
 * to user detail screens, and delegates data loading and organizer
 * demotion to {@link AdminUsersController}. The controller delivers
 * filtered {@link AdminUserAdapter.UserRow} items through the
 * {@link AdminUsersView} interface.
 * <p>
 */
public class AUsersFrag extends Fragment implements AdminUsersView {

    private RecyclerView rvUsers;
    private TextView emptyView;
    private EditText etSearchUsers;

    private AdminUserAdapter adapter;
    private AdminUsersController controller;

    public AUsersFrag() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_a_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvUsers = view.findViewById(R.id.rvUsers);
        emptyView = view.findViewById(R.id.empty);
        etSearchUsers = view.findViewById(R.id.etSearchUsers);
        ImageButton btnFilter = view.findViewById(R.id.btnFilter); // currently unused, reserved for future filters

        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminUserAdapter();
        final NavController navController = NavHostFragment.findNavController(this);

        // Navigate to admin user detail on row click
        adapter.setOnUserClick(userRow -> {
            if (userRow == null || userRow.id == null) {
                return;
            }
            Bundle args = new Bundle();
            args.putString("uid", userRow.id);
            args.putString("name", userRow.name);
            args.putString("email", userRow.email);
            args.putString("role", userRow.role);
            args.putString("avatarUrl", userRow.avatarUrl);
            navController.navigate(R.id.navigation_admin_user_detail, args);
        });

        // Demote organizer from inline "remove" action
        adapter.setOnRemoveClick(userRow -> {
            if (userRow == null) {
                return;
            }
            showDemoteConfirmation(userRow);
        });

        rvUsers.setAdapter(adapter);

        // Controller (C in MVC)
        controller = new AdminUsersController(this);
        controller.start();

        // Search bar delegates query changes to the controller
        if (etSearchUsers != null) {
            etSearchUsers.addTextChangedListener(new TextWatcher() {
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

//        if (btnFilter != null) {
//            btnFilter.setOnClickListener(v -> {
//                Toast.makeText(requireContext(),
//                        "Additional filters not implemented yet.",
//                        Toast.LENGTH_SHORT).show();
//            });
//        }
    }

    @Override
    public void onDestroyView() {
        if (controller != null) {
            controller.stop();
            controller = null;
        }
        rvUsers = null;
        emptyView = null;
        etSearchUsers = null;
        adapter = null;
        super.onDestroyView();
    }

    // ---------------------------------------------------------------------
    // AdminUsersView implementation (View in MVC)
    // ---------------------------------------------------------------------

    @Override
    public void showUsers(@NonNull List<AdminUserAdapter.UserRow> users) {
        if (adapter != null) {
            adapter.submit(users);
        }
        // empty state is also updated via showEmptyState, but we keep this safe
        if (emptyView != null) {
            emptyView.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void showEmptyState(boolean showEmpty) {
        if (emptyView != null) {
            emptyView.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void showLoading(boolean loading) {
        // Could hook into a ProgressBar or SwipeRefreshLayout if desired.
    }

    @Override
    public void showMessage(@NonNull String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    // ---------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------

    private void showDemoteConfirmation(@NonNull AdminUserAdapter.UserRow userRow) {
        String name = (userRow.name != null && !userRow.name.isEmpty())
                ? userRow.name
                : userRow.email;

        new AlertDialog.Builder(requireContext())
                .setTitle("Demote organizer?")
                .setMessage("This will demote " + name + " to a regular user "
                        + "and mark their account as suspended.")
                .setPositiveButton("Demote", (d, w) -> {
                    if (controller != null) {
                        controller.onDemoteOrganizerRequested(userRow);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
