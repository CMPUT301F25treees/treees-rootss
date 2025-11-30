package com.example.myapplication.features.admin;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AdminUsersControllerTest {

    private AdminUsersView view;
    private FirebaseFirestore db;
    private CollectionReference usersCollection;
    private ListenerRegistration registration;

    private AdminUsersController controller;
    private ArgumentCaptor<EventListener<QuerySnapshot>> listenerCaptor;

    @Before
    public void setUp() {
        view = mock(AdminUsersView.class);

        db = mock(FirebaseFirestore.class);
        usersCollection = mock(CollectionReference.class);
        registration = mock(ListenerRegistration.class);

        when(db.collection("users")).thenReturn(usersCollection);

        listenerCaptor = ArgumentCaptor.forClass(EventListener.class);
        when(usersCollection.addSnapshotListener(listenerCaptor.capture()))
                .thenReturn(registration);

        controller = new AdminUsersController(view, db);
    }

    @Test
    public void start_loadsNonAdminUsers_andMapsToUserRows() {
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        DocumentSnapshot adminDoc = mock(DocumentSnapshot.class);
        DocumentSnapshot userDoc = mock(DocumentSnapshot.class);
        DocumentSnapshot orgDoc = mock(DocumentSnapshot.class);

        when(snapshot.getDocuments())
                .thenReturn(Arrays.asList(adminDoc, userDoc, orgDoc));

        // admin doc should be filtered out
        when(adminDoc.getString("role")).thenReturn("Admin");

        when(userDoc.getString("role")).thenReturn("User");
        when(userDoc.getId()).thenReturn("u1");
        when(userDoc.getString("firstName")).thenReturn("Alice");
        when(userDoc.getString("lastName")).thenReturn("Smith");
        when(userDoc.getString("name")).thenReturn(null);
        when(userDoc.getString("email")).thenReturn("alice@example.com");
        when(userDoc.getString("avatarUrl")).thenReturn(null);
        when(userDoc.getString("photoUrl")).thenReturn(null);

        when(orgDoc.getString("role")).thenReturn("Organizer");
        when(orgDoc.getId()).thenReturn("u2");
        when(orgDoc.getString("firstName")).thenReturn("");
        when(orgDoc.getString("lastName")).thenReturn("");
        when(orgDoc.getString("name")).thenReturn("Org Name");
        when(orgDoc.getString("email")).thenReturn("org@example.com");
        when(orgDoc.getString("avatarUrl")).thenReturn("http://avatar");
        when(orgDoc.getString("photoUrl")).thenReturn(null);

        // Act
        controller.start();
        EventListener<QuerySnapshot> listener = listenerCaptor.getValue();
        listener.onEvent(snapshot, null);

        // Assert
        ArgumentCaptor<List<AdminUserAdapter.UserRow>> listCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(view).showLoading(true);
        verify(view).showLoading(false);
        verify(view).showUsers(listCaptor.capture());
        verify(view).showEmptyState(false);

        List<AdminUserAdapter.UserRow> rows = listCaptor.getValue();
        // 2 rows: User + Organizer
        org.junit.Assert.assertEquals(2, rows.size());

        AdminUserAdapter.UserRow row1 = rows.get(0);
        AdminUserAdapter.UserRow row2 = rows.get(1);

        // First row should have display name "Alice Smith"
        org.junit.Assert.assertEquals("u1", row1.id);
        org.junit.Assert.assertEquals("Alice Smith", row1.name);
        org.junit.Assert.assertEquals("alice@example.com", row1.email);
        org.junit.Assert.assertEquals("User", row1.role);

        // Second row should use 'name' field
        org.junit.Assert.assertEquals("u2", row2.id);
        org.junit.Assert.assertEquals("Org Name", row2.name);
        org.junit.Assert.assertEquals("org@example.com", row2.email);
        org.junit.Assert.assertEquals("Organizer", row2.role);
    }

    @Test
    public void searchQuery_filtersRowsByNameEmailOrRole() {
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        DocumentSnapshot userDoc = mock(DocumentSnapshot.class);
        DocumentSnapshot orgDoc = mock(DocumentSnapshot.class);

        when(snapshot.getDocuments()).thenReturn(Arrays.asList(userDoc, orgDoc));

        when(userDoc.getString("role")).thenReturn("User");
        when(userDoc.getId()).thenReturn("u1");
        when(userDoc.getString("firstName")).thenReturn("Alice");
        when(userDoc.getString("lastName")).thenReturn("Smith");
        when(userDoc.getString("name")).thenReturn(null);
        when(userDoc.getString("email")).thenReturn("alice@example.com");
        when(userDoc.getString("avatarUrl")).thenReturn(null);
        when(userDoc.getString("photoUrl")).thenReturn(null);

        when(orgDoc.getString("role")).thenReturn("Organizer");
        when(orgDoc.getId()).thenReturn("u2");
        when(orgDoc.getString("firstName")).thenReturn("Bob");
        when(orgDoc.getString("lastName")).thenReturn("Brown");
        when(orgDoc.getString("name")).thenReturn(null);
        when(orgDoc.getString("email")).thenReturn("org@example.com");
        when(orgDoc.getString("avatarUrl")).thenReturn(null);
        when(orgDoc.getString("photoUrl")).thenReturn(null);

        controller.start();
        EventListener<QuerySnapshot> listener = listenerCaptor.getValue();
        listener.onEvent(snapshot, null);

        // Act: search for "alice"
        controller.onSearchQueryChanged("alice");

        ArgumentCaptor<List<AdminUserAdapter.UserRow>> listCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(view, atLeastOnce()).showUsers(listCaptor.capture());

        List<AdminUserAdapter.UserRow> lastShown = listCaptor.getValue();
        org.junit.Assert.assertEquals(1, lastShown.size());
        org.junit.Assert.assertEquals("u1", lastShown.get(0).id);
    }

    @Test
    public void firestoreError_showsMessage() {
        controller.start();
        EventListener<QuerySnapshot> listener = listenerCaptor.getValue();

        FirebaseFirestoreException ex = mock(FirebaseFirestoreException.class);
        when(ex.getMessage()).thenReturn("boom");

        listener.onEvent(null, ex);

        verify(view).showLoading(false);
        verify(view).showMessage(anyString());
    }

    @Test
    public void demoteOrganizer_updatesUserDoc_andNotifiesView() {
        AdminUserAdapter.UserRow row = new AdminUserAdapter.UserRow();
        row.id = "u2";
        row.name = "Org Name";
        row.email = "org@example.com";
        row.role = "Organizer";

        DocumentReference userDocRef = mock(DocumentReference.class);
        Task<Void> updateTask = mock(Task.class);

        when(db.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document(eq("u2"))).thenReturn(userDocRef);
        when(userDocRef.update(eq("role"), eq("User"), eq("suspended"), eq(true)))
                .thenReturn(updateTask);

        // When addOnSuccessListener is called, invoke the listener immediately.
        when(updateTask.addOnSuccessListener(any()))
                .thenAnswer(invocation -> {
                    OnSuccessListener<Void> listener = invocation.getArgument(0);
                    listener.onSuccess(null);
                    return updateTask;
                });

        controller.onDemoteOrganizerRequested(row);

        verify(view, atLeastOnce()).showLoading(true);
        verify(view).showLoading(false);
        verify(view).showMessage(contains("demoted"));
    }
}
