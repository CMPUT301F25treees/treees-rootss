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
        // Arrange
        List<String> interests = Arrays.asList("Music", "Art");
        controller.updateInterests(interests); // Set some interests

        controller.updateAvailability(100L, 200L); // Set some availability

        // Act
        controller.clearAllFilters();

        // Assert
        verify(mockModel).setSelectedInterests(new ArrayList<>());
        verify(mockModel).clearAvailability();
        // Verify that applyFiltersInternal was called, which leads to showEvents/showEmptyState
        verify(mockView).showEvents(mockModel.buildDisplayEvents(), ""); 
    }

    @Test
    public void loadEvents_setsEventsAndRefreshesView() {
        // Arrange
        List<UserEvent> fetchedEvents = Arrays.asList(new UserEvent(), new UserEvent());
        ArgumentCaptor<FirebaseEventRepository.EventListCallback> callbackCaptor =
                ArgumentCaptor.forClass(FirebaseEventRepository.EventListCallback.class);

        // Act
        controller.loadEvents();

        // Simulate repository returning events
        verify(mockRepository).getAllEvents(callbackCaptor.capture());
        callbackCaptor.getValue().onEventsFetched(fetchedEvents);

        // Assert
        verify(mockModel).setEvents(UHomeModel.filterEventsForDisplay(fetchedEvents, "testUserId"));
        verify(mockView).showEvents(mockModel.buildDisplayEvents(), "");
    }

    @Test
    public void onSearchQueryChanged_updatesSearchQueryAndRefreshesView() {
        // Arrange
        String newQuery = "concert";

        // Act
        controller.onSearchQueryChanged(newQuery);

        // Assert
        verify(mockView).showEvents(mockModel.buildDisplayEvents(), newQuery);
    }

    @Test
    public void updateInterests_updatesModelAndRefreshesView() {
        // Arrange
        List<String> newInterests = Arrays.asList("Sports");

        // Act
        controller.updateInterests(newInterests);

        // Assert
        verify(mockModel).setSelectedInterests(newInterests);
        verify(mockView).showEvents(mockModel.buildDisplayEvents(), "");
    }

    @Test
    public void updateAvailability_updatesModelAndRefreshesView() {
        // Arrange
        Long start = 1000L;
        Long end = 2000L;

        // Act
        controller.updateAvailability(start, end);

        // Assert
        verify(mockModel).setAvailabilityRange(start, end);
        verify(mockView).showEvents(mockModel.buildDisplayEvents(), "");
    }

    @Test
    public void clearAvailability_clearsModelAndRefreshesView() {
        // Act
        controller.clearAvailability();

        // Assert
        verify(mockModel).clearAvailability();
        verify(mockView).showEvents(mockModel.buildDisplayEvents(), "");
    }
}
