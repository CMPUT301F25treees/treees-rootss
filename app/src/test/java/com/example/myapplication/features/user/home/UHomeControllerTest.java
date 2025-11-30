package com.example.myapplication.features.user.home;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.features.user.UserEvent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UHomeControllerTest {

    private UHomeController controller;
    private FirebaseEventRepository mockRepository;
    private FirebaseAuth mockAuth;
    private UHomeModel mockModel;
    private UHomeView mockView;
    private FirebaseUser mockFirebaseUser;

    @Before
    public void setUp() {
        mockRepository = mock(FirebaseEventRepository.class);
        mockAuth = mock(FirebaseAuth.class);
        mockModel = mock(UHomeModel.class);
        mockView = mock(UHomeView.class);
        mockFirebaseUser = mock(FirebaseUser.class);

        when(mockAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn("testUserId");

        controller = new UHomeController(mockRepository, mockAuth, mockModel, mockView);
    }

    @Test
    public void clearAllFilters_clearsInterestsAndAvailabilityAndRefreshesView() {
        List<String> interests = Arrays.asList("Music", "Art");
        controller.updateInterests(interests); // Set some interests

        controller.updateAvailability(100L, 200L); // Set some availability

        controller.clearAllFilters();

        verify(mockModel).setSelectedInterests(new ArrayList<>());
        verify(mockModel).clearAvailability();
        // Verify that applyFiltersInternal was called, which leads to showEvents/showEmptyState
        verify(mockView).showEvents(mockModel.buildDisplayEvents(), ""); 
    }

    @Test
    public void loadEvents_setsEventsAndRefreshesView() {
        List<UserEvent> fetchedEvents = Arrays.asList(new UserEvent(), new UserEvent());
        ArgumentCaptor<FirebaseEventRepository.EventListCallback> callbackCaptor =
                ArgumentCaptor.forClass(FirebaseEventRepository.EventListCallback.class);

        controller.loadEvents();

        verify(mockRepository).getAllEvents(callbackCaptor.capture());
        callbackCaptor.getValue().onEventsFetched(fetchedEvents);

        verify(mockModel).setEvents(UHomeModel.filterEventsForDisplay(fetchedEvents, "testUserId"));
        verify(mockView).showEvents(mockModel.buildDisplayEvents(), "");
    }

    @Test
    public void onSearchQueryChanged_updatesSearchQueryAndRefreshesView() {
        String newQuery = "concert";

        controller.onSearchQueryChanged(newQuery);

        verify(mockView).showEvents(mockModel.buildDisplayEvents(), newQuery);
    }

    @Test
    public void updateInterests_updatesModelAndRefreshesView() {
        List<String> newInterests = Arrays.asList("Sports");

        controller.updateInterests(newInterests);

        verify(mockModel).setSelectedInterests(newInterests);
        verify(mockView).showEvents(mockModel.buildDisplayEvents(), "");
    }

    @Test
    public void updateAvailability_updatesModelAndRefreshesView() {
        Long start = 1000L;
        Long end = 2000L;

        controller.updateAvailability(start, end);

        verify(mockModel).setAvailabilityRange(start, end);
        verify(mockView).showEvents(mockModel.buildDisplayEvents(), "");
    }

    @Test
    public void clearAvailability_clearsModelAndRefreshesView() {
        controller.clearAvailability();

        verify(mockModel).clearAvailability();
        verify(mockView).showEvents(mockModel.buildDisplayEvents(), "");
    }
}
