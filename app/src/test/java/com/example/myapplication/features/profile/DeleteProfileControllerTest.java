package com.example.myapplication.features.profile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.example.myapplication.data.repo.UserRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

public class DeleteProfileControllerTest {

    @Mock
    private DeleteProfileView view;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FirebaseAuth firebaseAuth;

    @Mock
    private FirebaseUser firebaseUser;

    private MockedStatic<FirebaseAuth> mockedFirebaseAuth;

    private DeleteProfileController controller;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        mockedFirebaseAuth = mockStatic(FirebaseAuth.class);
        mockedFirebaseAuth.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
        when(firebaseAuth.getCurrentUser()).thenReturn(firebaseUser);
        when(firebaseUser.getUid()).thenReturn("test-uid");

        controller = new DeleteProfileController(view, userRepository);
    }

    @After
    public void tearDown() {
        mockedFirebaseAuth.close();
    }

    @Test
    public void onDeleteProfileClicked_showsConfirmationDialog() {
        controller.onDeleteProfileClicked();
        verify(view).showConfirmationDialog();
    }

    @Test
    public void onDeleteConfirmed_userNull_showsToast() {
        when(firebaseAuth.getCurrentUser()).thenReturn(null);
        controller.onDeleteConfirmed();
        verify(view).showToast(anyString());
        verify(view, never()).showProgress(true);
    }

    @Test
    public void onDeleteConfirmed_happyPath() {
        // Setup successful callbacks
        doAnswer(invocation -> {
            Runnable success = invocation.getArgument(1);
            success.run();
            return null;
        }).when(userRepository).deleteEventsForUser(anyString(), any(), any());

        doAnswer(invocation -> {
            Runnable success = invocation.getArgument(1);
            success.run();
            return null;
        }).when(userRepository).deleteUserDocument(anyString(), any(), any());

        doAnswer(invocation -> {
            Runnable success = invocation.getArgument(1);
            success.run();
            return null;
        }).when(userRepository).deleteAuthUser(any(), any(), any());

        controller.onDeleteConfirmed();

        verify(view).showProgress(true);
        verify(userRepository).deleteEventsForUser(eq("test-uid"), any(), any());
        verify(userRepository).deleteUserDocument(eq("test-uid"), any(), any());
        verify(userRepository).deleteAuthUser(eq(firebaseUser), any(), any());
        verify(view).showProgress(false);
        verify(view).showToast("Profile deleted successfully");
        verify(view).navigateOnSuccess();
    }

    @Test
    public void onDeleteConfirmed_eventDeletionFails() {
        doAnswer(invocation -> {
            OnFailureListener failure = invocation.getArgument(2);
            failure.onFailure(new Exception("Event delete failed"));
            return null;
        }).when(userRepository).deleteEventsForUser(anyString(), any(), any());

        controller.onDeleteConfirmed();

        verify(view).showProgress(true);
        verify(userRepository).deleteEventsForUser(eq("test-uid"), any(), any());
        verify(userRepository, never()).deleteUserDocument(anyString(), any(), any());
        verify(view).showProgress(false);
        verify(view).showToast("Failed: Event delete failed");
    }

    @Test
    public void onAdminDeleteConfirmed_organizer_happyPath() {
        String uid = "other-uid";
        String role = "organizer";

        doAnswer(invocation -> {
            Runnable success = invocation.getArgument(1);
            success.run();
            return null;
        }).when(userRepository).disableEventsForUser(eq(uid), any(), any());

        doAnswer(invocation -> {
            Runnable success = invocation.getArgument(1);
            success.run();
            return null;
        }).when(userRepository).deleteUserDocument(eq(uid), any(), any());

        controller.onAdminDeleteConfirmed(uid, role);

        verify(view).showProgress(true);
        verify(userRepository).disableEventsForUser(eq(uid), any(), any());
        verify(userRepository).deleteUserDocument(eq(uid), any(), any());
        verify(view).showProgress(false);
        verify(view).showToast("Profile deleted");
        verify(view).navigateOnSuccess();
    }

    @Test
    public void onAdminDeleteConfirmed_user_happyPath() {
        String uid = "other-uid";
        String role = "user";

        doAnswer(invocation -> {
            Runnable success = invocation.getArgument(1);
            success.run();
            return null;
        }).when(userRepository).deleteUserDocument(eq(uid), any(), any());

        controller.onAdminDeleteConfirmed(uid, role);

        verify(view).showProgress(true);
        verify(userRepository, never()).disableEventsForUser(anyString(), any(), any());
        verify(userRepository).deleteUserDocument(eq(uid), any(), any());
        verify(view).showProgress(false);
        verify(view).showToast("Profile deleted");
        verify(view).navigateOnSuccess();
    }
}
