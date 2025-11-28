package com.example.myapplication.features.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.R;
import com.example.myapplication.core.DeviceLoginStore;
import com.example.myapplication.core.UserSession;
import com.example.myapplication.data.model.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies that a remembered user is automatically restored when the login button is tapped
 * after relaunching the app without explicitly logging out.
 */
@RunWith(AndroidJUnit4.class)
public class RememberedLoginTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DeviceLoginStore.markLoggedOut(context);
        UserSession.getInstance().clearSession();
    }

    @After
    public void tearDown() {
        DeviceLoginStore.markLoggedOut(context);
        UserSession.getInstance().clearSession();
    }

    @Test
    public void loginButton_autoLogsRememberedUser() {
        User remembered = new User();
        remembered.setUid("LOCAL_saved_user");
        remembered.setEmail("remembered@example.com");
        remembered.setUsername("Remembered User");
        remembered.setRole("user");

        DeviceLoginStore.rememberUser(context, remembered);

        NavController navController = mock(NavController.class);

        FragmentScenario<WelcomeFrag> scenario = FragmentScenario.launchInContainer(
                WelcomeFrag.class,
                null,
                R.style.Theme_MyApplication,
                Lifecycle.State.RESUMED
        );

        scenario.onFragment(fragment -> {
            Navigation.setViewNavController(fragment.requireView(), navController);
            fragment.requireView().findViewById(R.id.btnLogin).performClick();
        });

        assertNotNull("User should be set in session", UserSession.getInstance().getCurrentUser());
        assertEquals("Remembered user is applied",
                remembered.getUid(),
                UserSession.getInstance().getCurrentUser().getUid());
        verify(navController).navigate(R.id.navigation_user_home);

        User persisted = DeviceLoginStore.getRememberedUser(context);
        assertNotNull("User stays persisted on device", persisted);
        assertEquals(remembered.getUid(), persisted.getUid());
        assertEquals(remembered.getEmail(), persisted.getEmail());
        assertEquals(remembered.getUsername(), persisted.getUsername());
    }
}
