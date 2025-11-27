package com.example.myapplication.features.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class UEditProfileFragTest {

    private static class Deps {
        FirebaseAuth auth;
        FirebaseUser firebaseUser;
        FirebaseFirestore db;
        CollectionReference usersCollection;
        DocumentReference userDoc;
        WriteBatch writeBatch;
    }

    private Deps createDeps(String uid, String email, DocumentSnapshot snapshot) {
        Deps deps = new Deps();
        deps.auth = mock(FirebaseAuth.class);
        deps.firebaseUser = mock(FirebaseUser.class);
        deps.db = mock(FirebaseFirestore.class);
        deps.usersCollection = mock(CollectionReference.class);
        deps.userDoc = mock(DocumentReference.class);
        deps.writeBatch = mock(WriteBatch.class);

        when(deps.auth.getCurrentUser()).thenReturn(deps.firebaseUser);
        when(deps.firebaseUser.getUid()).thenReturn(uid);
        when(deps.firebaseUser.getEmail()).thenReturn(email);

        when(deps.db.collection("users")).thenReturn(deps.usersCollection);
        when(deps.usersCollection.document(anyString())).thenReturn(deps.userDoc);
        when(deps.db.batch()).thenReturn(deps.writeBatch);
        when(deps.writeBatch.set(eq(deps.userDoc), anyMap(), eq(SetOptions.merge()))).thenReturn(deps.writeBatch);
        when(deps.writeBatch.commit()).thenReturn(Tasks.forResult(null));
        when(deps.userDoc.update(anyString(), any())).thenReturn(Tasks.forResult(null));

        DocumentSnapshot emptySnapshot = mock(DocumentSnapshot.class);
        when(emptySnapshot.exists()).thenReturn(false);
        when(deps.userDoc.get()).thenReturn(Tasks.forResult(snapshot != null ? snapshot : emptySnapshot));

        return deps;
    }

    private FragmentScenario<UEditProfileFrag> launchFragment(Deps deps) {
        return FragmentScenario.launchInContainer(
                UEditProfileFrag.class,
                null,
                R.style.Theme_MyApplication,
                new FragmentFactory() {
                    @NonNull
                    @Override
                    public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
                        UEditProfileFrag fragment = new UEditProfileFrag();
                        fragment.setAuth(deps.auth);
                        fragment.setDb(deps.db);
                        return fragment;
                    }
                }
        );
    }

    @Test
    public void populateFields_usesFirestoreDataWhenPresent() {
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.getString("firstName")).thenReturn("Alice");
        when(snapshot.getString("lastName")).thenReturn("Smith");
        when(snapshot.getString("cell")).thenReturn("5551234");
        when(snapshot.getString("email")).thenReturn("snap@example.com");

        Deps deps = createDeps("uid123", "auth@example.com", snapshot);
        FragmentScenario<UEditProfileFrag> scenario = launchFragment(deps);

        scenario.onFragment(fragment -> {
            Robolectric.flushForegroundThreadScheduler();
            TextInputEditText firstNameInput = fragment.requireView().findViewById(R.id.edit_first_name_input);
            TextInputEditText lastNameInput = fragment.requireView().findViewById(R.id.edit_last_name_input);
            TextInputEditText phoneInput = fragment.requireView().findViewById(R.id.edit_phone_input);
            TextInputEditText emailInput = fragment.requireView().findViewById(R.id.edit_email_input);

            assertEquals("Alice", firstNameInput.getText().toString());
            assertEquals("Smith", lastNameInput.getText().toString());
            assertEquals("5551234", phoneInput.getText().toString());
            assertEquals("snap@example.com", emailInput.getText().toString());
        });
    }

    @Test
    public void attemptSave_blocksEmptyFirstName() {
        Deps deps = createDeps("uid123", "auth@example.com", null);
        FragmentScenario<UEditProfileFrag> scenario = launchFragment(deps);

        scenario.onFragment(fragment -> {
            TextInputEditText firstNameInput = fragment.requireView().findViewById(R.id.edit_first_name_input);
            TextInputEditText emailInput = fragment.requireView().findViewById(R.id.edit_email_input);
            MaterialButton saveButton = fragment.requireView().findViewById(R.id.btnSaveProfile);
            TextInputLayout firstNameLayout = fragment.requireView().findViewById(R.id.tilEditFirstName);

            firstNameInput.setText("");
            emailInput.setText("valid@example.com");

            saveButton.performClick();

            assertEquals(fragment.getString(R.string.edit_profile_first_name_required), firstNameLayout.getError());
            verify(deps.writeBatch, never()).commit();
        });
    }

    @Test
    public void attemptSave_blocksInvalidEmail() {
        Deps deps = createDeps("uid123", "auth@example.com", null);
        FragmentScenario<UEditProfileFrag> scenario = launchFragment(deps);

        scenario.onFragment(fragment -> {
            TextInputEditText firstNameInput = fragment.requireView().findViewById(R.id.edit_first_name_input);
            TextInputEditText emailInput = fragment.requireView().findViewById(R.id.edit_email_input);
            MaterialButton saveButton = fragment.requireView().findViewById(R.id.btnSaveProfile);
            TextInputLayout emailLayout = fragment.requireView().findViewById(R.id.tilEditEmail);

            firstNameInput.setText("Valid");
            emailInput.setText("not-an-email");

            saveButton.performClick();

            assertEquals(fragment.getString(R.string.edit_profile_email_invalid), emailLayout.getError());
            verify(deps.writeBatch, never()).commit();
        });
    }

    @Test
    public void attemptSave_updatesProfileWhenEmailUnchanged() {
        Deps deps = createDeps("uid123", "auth@example.com", null);
        FragmentScenario<UEditProfileFrag> scenario = launchFragment(deps);

        scenario.onFragment(fragment -> {
            TextInputEditText firstNameInput = fragment.requireView().findViewById(R.id.edit_first_name_input);
            TextInputEditText lastNameInput = fragment.requireView().findViewById(R.id.edit_last_name_input);
            TextInputEditText phoneInput = fragment.requireView().findViewById(R.id.edit_phone_input);
            TextInputEditText emailInput = fragment.requireView().findViewById(R.id.edit_email_input);
            MaterialButton saveButton = fragment.requireView().findViewById(R.id.btnSaveProfile);

            NavController navController = mock(NavController.class);
            Navigation.setViewNavController(fragment.requireView(), navController);

            firstNameInput.setText("Updated");
            lastNameInput.setText("User");
            phoneInput.setText("999");
            emailInput.setText("auth@example.com"); // unchanged

            saveButton.performClick();
            Robolectric.flushForegroundThreadScheduler();

            ArgumentCaptor<Map<String, Object>> updatesCaptor = ArgumentCaptor.forClass(Map.class);
            verify(deps.writeBatch).set(eq(deps.userDoc), updatesCaptor.capture(), eq(SetOptions.merge()));
            Map<String, Object> updates = updatesCaptor.getValue();

            assertEquals("Updated", updates.get("firstName"));
            assertEquals("Updated", updates.get("username"));
            assertEquals("User", updates.get("lastName"));
            assertEquals("999", updates.get("cell"));

            verify(deps.writeBatch).commit();
            verify(navController).popBackStack();
        });
    }

    @Test
    public void attemptSave_allowsOptionalFieldsToBeCleared() {
        Deps deps = createDeps("uid123", "auth@example.com", null);
        FragmentScenario<UEditProfileFrag> scenario = launchFragment(deps);

        scenario.onFragment(fragment -> {
            TextInputEditText firstNameInput = fragment.requireView().findViewById(R.id.edit_first_name_input);
            TextInputEditText lastNameInput = fragment.requireView().findViewById(R.id.edit_last_name_input);
            TextInputEditText phoneInput = fragment.requireView().findViewById(R.id.edit_phone_input);
            TextInputEditText emailInput = fragment.requireView().findViewById(R.id.edit_email_input);
            MaterialButton saveButton = fragment.requireView().findViewById(R.id.btnSaveProfile);

            NavController navController = mock(NavController.class);
            Navigation.setViewNavController(fragment.requireView(), navController);

            firstNameInput.setText("Updated");
            lastNameInput.setText("");
            phoneInput.setText("");
            emailInput.setText("auth@example.com"); // unchanged

            saveButton.performClick();
            Robolectric.flushForegroundThreadScheduler();

            ArgumentCaptor<Map<String, Object>> updatesCaptor = ArgumentCaptor.forClass(Map.class);
            verify(deps.writeBatch).set(eq(deps.userDoc), updatesCaptor.capture(), eq(SetOptions.merge()));
            Map<String, Object> updates = updatesCaptor.getValue();

            assertNull(updates.get("lastName"));
            assertNull(updates.get("cell"));

            verify(deps.writeBatch).commit();
            verify(navController).popBackStack();
        });
    }
}
