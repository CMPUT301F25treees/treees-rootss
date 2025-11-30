package com.example.myapplication.features.admin;

/**
 * Enumerates the available display modes for the admin home screen.
 * <p>
 * In {@link AdminHomeMode#EVENTS} mode the grid shows all events that match
 * the current search query. In {@link AdminHomeMode#PHOTOS} mode the grid
 * shows only events that have an image associated with them, effectively
 * acting as a "photo gallery" for admins.
 * <p>
 */
public enum AdminHomeMode {
    /**
     * Standard mode: the admin home grid displays events.
     */
    EVENTS,

    /**
     * Photo mode: the admin home grid displays only events that have images.
     */
    PHOTOS
}
