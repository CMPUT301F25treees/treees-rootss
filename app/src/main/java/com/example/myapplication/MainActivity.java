package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.example.myapplication.core.UserSession;
import com.example.myapplication.data.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
        currentMenuResId = -1;

        navController.addOnDestinationChangedListener((c, d, a) -> {
            boolean hide = d.getId() == R.id.navigation_welcome
                    || d.getId() == R.id.navigation_login
                    || d.getId() == R.id.navigation_register
                    || d.getId() == R.id.navigation_user_event_detail
                    || d.getId() == R.id.navigation_organizer_event_detail
                    || d.getId() == R.id.navigation_organizer_event_edit;
            navView.setVisibility(hide ? View.GONE : View.VISIBLE);
            if (!hide) {
                updateBottomNavMenu();
            }
        });
    }

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
        currentMenuResId = desiredMenu;
    }

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

    public void refreshNavigationForRole() {
        updateBottomNavMenu();
    }

    public void navigateToBottomDestination(@IdRes int destinationId) {
        navView.setSelectedItemId(destinationId);
    }
}
