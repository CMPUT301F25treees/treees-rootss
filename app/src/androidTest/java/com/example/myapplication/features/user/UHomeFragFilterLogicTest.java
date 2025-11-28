package com.example.myapplication.features.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class UHomeFragFilterLogicTest {

    @Test
    public void filterEventsByInterests_matchesCaseInsensitiveThemes() {
        UserEvent music = eventWithTheme("Music");
        UserEvent sports = eventWithTheme("Sports");
        UserEvent lowerCaseMusic = eventWithTheme("music");
        UserEvent noTheme = eventWithTheme(null);

        List<UserEvent> filtered = UHomeFrag.filterEventsByInterests(
                Arrays.asList(music, sports, lowerCaseMusic, noTheme),
                Arrays.asList("MUSIC", "art")
        );

        assertEquals(2, filtered.size());
        assertTrue(filtered.contains(music));
        assertTrue(filtered.contains(lowerCaseMusic));
        assertFalse(filtered.contains(sports));
        assertFalse(filtered.contains(noTheme));
    }

    @Test
    public void filterEventsByInterests_returnsAllWhenNoInterestsProvided() {
        List<UserEvent> events = Arrays.asList(
                eventWithTheme("Music"),
                eventWithTheme("Sports")
        );

        List<UserEvent> filtered = UHomeFrag.filterEventsByInterests(events, Collections.emptyList());

        assertEquals(events, filtered);
    }

    @Test
    public void filterEventsByAvailability_includesEventsOverlappingRangeAndNormalizesBounds() {
        UserEvent early = eventWithTimes(1_000L, 1_500L);
        UserEvent middle = eventWithTimes(3_000L, 3_500L);
        UserEvent openEnded = eventWithTimes(2_500L, 0);
        UserEvent late = eventWithTimes(5_000L, 5_500L);

        List<UserEvent> filtered = UHomeFrag.filterEventsByAvailability(
                Arrays.asList(early, middle, openEnded, late),
                4_000L,
                2_000L
        );

        assertEquals(2, filtered.size());
        assertTrue(filtered.contains(middle));
        assertTrue(filtered.contains(openEnded));
        assertFalse(filtered.contains(early));
        assertFalse(filtered.contains(late));
    }

    @Test
    public void filterEventsForDisplay_excludesCurrentUsersEvents() {
        UserEvent mine = eventWithOrganizer("user-1");
        UserEvent theirs = eventWithOrganizer("user-2");
        UserEvent noOrganizer = eventWithOrganizer(null);

        List<UserEvent> filtered = UHomeFrag.filterEventsForDisplay(
                Arrays.asList(mine, theirs, null, noOrganizer),
                "user-1"
        );

        assertEquals(2, filtered.size());
        assertTrue(filtered.contains(theirs));
        assertTrue(filtered.contains(noOrganizer));
        assertFalse(filtered.contains(mine));
    }

    @Test
    public void filterEventsForDisplay_handlesNullInputs() {
        assertTrue(UHomeFrag.filterEventsForDisplay(null, "user-1").isEmpty());
        assertTrue(UHomeFrag.filterEventsForDisplay(Collections.emptyList(), "user-1").isEmpty());
        assertTrue(UHomeFrag.filterEventsForDisplay(Arrays.asList(eventWithOrganizer("user-2")), null).isEmpty());
    }

    @Test
    public void isUpcomingEvent_handlesMissingEndTime() {
        long now = 10_000L;
        UserEvent upcomingWithEnd = eventWithTimes(11_000L, 12_000L);
        UserEvent ongoingNoEnd = eventWithTimes(11_000L, 0);
        UserEvent past = eventWithTimes(1_000L, 2_000L);

        assertTrue(UHomeFrag.isUpcomingEvent(upcomingWithEnd, now));
        assertTrue(UHomeFrag.isUpcomingEvent(ongoingNoEnd, now));
        assertFalse(UHomeFrag.isUpcomingEvent(past, now));
    }

    private static UserEvent eventWithTheme(String theme) {
        UserEvent event = new UserEvent();
        event.setTheme(theme);
        event.setStartTimeMillis(1_000L);
        event.setEndTimeMillis(2_000L);
        return event;
    }

    private static UserEvent eventWithTimes(long start, long end) {
        UserEvent event = new UserEvent();
        event.setStartTimeMillis(start);
        event.setEndTimeMillis(end);
        return event;
    }

    private static UserEvent eventWithOrganizer(String organizerId) {
        UserEvent event = new UserEvent();
        event.setOrganizerID(organizerId);
        return event;
    }
}
