package com.example.myapplication.features.organizer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.os.Build;
import android.view.View;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.core.UserSession;
import com.example.myapplication.data.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class OProfileFragTest {

    @Test
    public void editCard_navigatesToSharedUserEditProfile() {
        try (MockedStatic<FirebaseFirestore> firestoreStatic = Mockito.mockStatic(FirebaseFirestore.class)) {
            firestoreStatic.when(FirebaseFirestore::getInstance).thenReturn(mock(FirebaseFirestore.class));

            User user = new User();
            user.setUid("uid123");
            user.setUsername("Organizer");
            user.setRole("organizer");
            UserSession.getInstance().setCurrentUser(user);

            FragmentScenario<OProfileFrag> scenario = FragmentScenario.launchInContainer(
                    OProfileFrag.class,
                    null,
                    R.style.Theme_MyApplication,
                    null
            );

            scenario.onFragment(fragment -> {
                NavController navController = mock(NavController.class);
                Navigation.setViewNavController(fragment.requireView(), navController);

                View editCard = fragment.requireView().findViewById(R.id.cardEditInfo);
                editCard.performClick();

                verify(navController).navigate(R.id.navigation_user_edit_profile);
            });

            UserSession.getInstance().setCurrentUser(null);
        }
    }
}
