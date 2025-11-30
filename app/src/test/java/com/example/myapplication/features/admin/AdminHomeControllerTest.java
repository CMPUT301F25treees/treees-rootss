package com.example.myapplication.features.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import androidx.annotation.NonNull;

import com.example.myapplication.features.user.UserEvent;
import com.google.firebase.firestore.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdminHomeControllerTest {

    private AdminHomeView view;
    private FirebaseFirestore db;
    private CollectionReference eventsCollection;
    private Query query;
    private ListenerRegistration registration;

    private AdminHomeController controller;

    // Captured listener for simulating Firestore updates
    private ArgumentCaptor<EventListener<QuerySnapshot>> listenerCaptor;

    @Before
    public void setUp() {
        view = mock(AdminHomeView.class);

        db = mock(FirebaseFirestore.class);
        eventsCollection = mock(CollectionReference.class);
        query = mock(Query.class);
        registration = mock(ListenerRegistration.class);

        when(db.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.orderBy(eq("startTimeMillis"), eq(Query.Direction.DESCENDING)))
                .thenReturn(query);

        listenerCaptor = ArgumentCaptor.forClass(EventListener.class);

        when(query.addSnapshotListener(listenerCaptor.capture()))
                .thenReturn(registration);

        controller = new AdminHomeController(view, db);
    }

    @Test
    public void start_loadsEvents_andShowsAllInEventsMode() {
        // Arrange: mock two events, both with images
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        DocumentSnapshot d1 = mock(DocumentSnapshot.class);
        DocumentSnapshot d2 = mock(DocumentSnapshot.class);

        UserEvent e1 = mock(UserEvent.class);
        UserEvent e2 = mock(UserEvent.class);

        when(snapshot.getDocuments()).thenReturn(Arrays.asList(d1, d2));

        when(d1.toObject(UserEvent.class)).thenReturn(e1);
        when(d2.toObject(UserEvent.class)).thenReturn(e2);

        when(d1.getId()).thenReturn("event-1");
        when(d2.getId()).thenReturn("event-2");

        when(d1.getString("imageUrl")).thenReturn("https://example.com/one.png");
        when(d1.getString("posterUrl")).thenReturn(null);
        when(d2.getString("imageUrl")).thenReturn(null);
        when(d2.getString("posterUrl")).thenReturn("https://example.com/two.png");

        // Act: start controller -> simulate Firestore snapshot callback
        controller.start();

        // The start() call registers a listener; now trigger it manually
        EventListener<QuerySnapshot> listener = listenerCaptor.getValue();
        listener.onEvent(snapshot, null);

        // Assert: view receives both events in EVENTS mode
        ArgumentCaptor<List<UserEvent>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(view).showLoading(true);
        verify(view).showLoading(false);
        verify(view).showEvents(listCaptor.capture());

        List<UserEvent> shown = listCaptor.getValue();
        // We don't care about the exact UserEvent instances (they are mocks),
        // only that both are passed through.
        org.junit.Assert.assertEquals(2, shown.size());
    }

    @Test
    public void photosMode_showsOnlyEventsWithImages() {
        // Arrange: three events, only two have any image fields
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        DocumentSnapshot d1 = mock(DocumentSnapshot.class);
        DocumentSnapshot d2 = mock(DocumentSnapshot.class);
        DocumentSnapshot d3 = mock(DocumentSnapshot.class);

        UserEvent e1 = mock(UserEvent.class);
        UserEvent e2 = mock(UserEvent.class);
        UserEvent e3 = mock(UserEvent.class);

        when(snapshot.getDocuments()).thenReturn(Arrays.asList(d1, d2, d3));

        when(d1.toObject(UserEvent.class)).thenReturn(e1);
        when(d2.toObject(UserEvent.class)).thenReturn(e2);
        when(d3.toObject(UserEvent.class)).thenReturn(e3);

        when(d1.getId()).thenReturn("e1");
        when(d2.getId()).thenReturn("e2");
        when(d3.getId()).thenReturn("e3");

        // e1: has imageUrl
        when(d1.getString("imageUrl")).thenReturn("https://img/1.png");
        when(d1.getString("posterUrl")).thenReturn(null);

        // e2: has posterUrl only
        when(d2.getString("imageUrl")).thenReturn(null);
        when(d2.getString("posterUrl")).thenReturn("https://img/2.png");

        // e3: no images
        when(d3.getString("imageUrl")).thenReturn(null);
        when(d3.getString("posterUrl")).thenReturn(null);

        controller.start();
        EventListener<QuerySnapshot> listener = listenerCaptor.getValue();
        listener.onEvent(snapshot, null);

        // Act: switch to PHOTOS mode
        controller.setMode(AdminHomeMode.PHOTOS);

        // Assert: view.showEvents called with only 2 events
        ArgumentCaptor<List<UserEvent>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(view, atLeastOnce()).showEvents(listCaptor.capture());

        // The last invocation corresponds to the PHOTOS mode list
        List<UserEvent> lastShown = listCaptor.getValue();
        org.junit.Assert.assertEquals(2, lastShown.size());
    }

    @Test
    public void searchQuery_filtersByNameLocationDescrAndImageFields() {
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        DocumentSnapshot d1 = mock(DocumentSnapshot.class);
        DocumentSnapshot d2 = mock(DocumentSnapshot.class);

        UserEvent e1 = mock(UserEvent.class);
        UserEvent e2 = mock(UserEvent.class);

        when(snapshot.getDocuments()).thenReturn(Arrays.asList(d1, d2));

        when(d1.toObject(UserEvent.class)).thenReturn(e1);
        when(d2.toObject(UserEvent.class)).thenReturn(e2);

        when(d1.getId()).thenReturn("e1");
        when(d2.getId()).thenReturn("e2");

        when(d1.getString("name")).thenReturn("Yoga for beginners");
        when(d1.getString("location")).thenReturn("Calgary");
        when(d1.getString("descr")).thenReturn(null);
        when(d1.getString("imageUrl")).thenReturn(null);
        when(d1.getString("posterUrl")).thenReturn(null);

        when(d2.getString("name")).thenReturn("Other event");
        when(d2.getString("location")).thenReturn("Edmonton");
        when(d2.getString("descr")).thenReturn(null);
        when(d2.getString("imageUrl")).thenReturn("https://example.com/yoga-poster.png");
        when(d2.getString("posterUrl")).thenReturn(null);

        controller.start();
        EventListener<QuerySnapshot> listener = listenerCaptor.getValue();
        listener.onEvent(snapshot, null);

        // Search for "yoga" should match both (name of e1, imageUrl of e2)
        controller.onSearchQueryChanged("yoga");

        ArgumentCaptor<List<UserEvent>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(view, atLeastOnce()).showEvents(listCaptor.capture());

        List<UserEvent> lastShown = listCaptor.getValue();
        org.junit.Assert.assertEquals(2, lastShown.size());
    }

    @Test
    public void firestoreError_showsErrorOnView() {
        controller.start();
        EventListener<QuerySnapshot> listener = listenerCaptor.getValue();

        FirebaseFirestoreException ex = mock(FirebaseFirestoreException.class);
        when(ex.getMessage()).thenReturn("boom");

        listener.onEvent(null, ex);

        verify(view).showLoading(false);
        verify(view).showError(anyString());
    }
}
