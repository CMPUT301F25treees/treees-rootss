package com.example.myapplication;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.example.myapplication.core.DeviceLoginStore;
import com.example.myapplication.core.UserSession;
import com.example.myapplication.data.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

/**
 * This class is the main entry point of the application
 *
 * This class is responsible for managing navigation across all fragments
 * of the application. The bottomnavigation is updated based on the user role.
 */
public class MainActivity extends AppCompatActivity {
    private BottomNavigationView navView;
    private NavController navController;
    private int currentMenuResId = -1;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navView = findViewById(R.id.nav_view);
        navView.setItemIconTintList(null);
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        NavigationUI.setupWithNavController(navView, navController);
        navView.setOnItemSelectedListener(this::handleBottomNavSelection);
        currentMenuResId = -1;

        navController.addOnDestinationChangedListener((c, d, a) -> {
            boolean hide = d.getId() == R.id.navigation_welcome
                    || d.getId() == R.id.navigation_login
                    || d.getId() == R.id.navigation_register
                    || d.getId() == R.id.navigation_user_event_detail
                    || d.getId() == R.id.navigation_u_edit_profile
                    || d.getId() == R.id.navigation_user_notifications
                    || d.getId() == R.id.navigation_user_past_events
                    || d.getId() == R.id.navigation_user_waitlist
                    || d.getId() == R.id.navigation_organizer_event_detail
                    || d.getId() == R.id.navigation_organizer_event_edit
                    || d.getId() == R.id.navigation_organizer_waitlist
                    || d.getId() == R.id.navigation_admin_notifications
                    || d.getId() == R.id.navigation_admin_event_detail
                    || d.getId() == R.id.navigation_admin_user_detail
                    || d.getId() == R.id.navigation_admin_remove_options;

            navView.setVisibility(hide ? View.GONE : View.VISIBLE);
            if (!hide) {
                updateBottomNavMenu();
            }
        });
    }

    /**
     * This method updates the bottom navigation menu to match the users current role
     */
    private void updateBottomNavMenu() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        String role = currentUser != null ? currentUser.getRole() : null;

        int desiredMenu = resolveMenuForRole(role);

        if (currentMenuResId == desiredMenu) {
            return;
        }

        navView.getMenu().clear();
        navView.inflateMenu(desiredMenu);
        NavigationUI.setupWithNavController(navView, navController);
        navView.setOnItemSelectedListener(this::handleBottomNavSelection);
        currentMenuResId = desiredMenu;
    }

    /**
     * This method determines which navigation menu should be used
     *
     * @param role the user role
     * @return the id of the determined bottom nav
     */
    private int resolveMenuForRole(String role) {
        if (role == null) {
            return R.menu.bottom_nav_user;
        }
        if ("admin".equalsIgnoreCase(role)) {
            return R.menu.bottom_nav_admin;
        }
        if ("organizer".equalsIgnoreCase(role)) {
            return R.menu.bottom_nav_organizer;
        }
        return R.menu.bottom_nav_user;
    }

    /**
     * Refreshes the navigation menu
     */
    public void refreshNavigationForRole() {
        updateBottomNavMenu();
    }

    /**
     * Navigates to the "destination" difined by the nav menu
     * @param destinationId
     */
    public void navigateToBottomDestination(@IdRes int destinationId) {
        navView.setSelectedItemId(destinationId);
    }

    /**
     * Handles bottom navigation item selection
     *
     * @param item the selected menu item
     * @return true if the selection was handled
     */
    private boolean handleBottomNavSelection(MenuItem item) {
        return NavigationUI.onNavDestinationSelected(item, navController);
    }

    /**
     * Performs user logout
     */
    public void performLogout() {
        FirebaseAuth.getInstance().signOut();
        UserSession.getInstance().clearSession();
        DeviceLoginStore.markLoggedOut(getApplicationContext());

        if (navController != null) {
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(navController.getGraph().getId(), true)
                    .build();
            navController.navigate(R.id.navigation_welcome, null, options);
        }
        currentMenuResId = -1;
    }
}
