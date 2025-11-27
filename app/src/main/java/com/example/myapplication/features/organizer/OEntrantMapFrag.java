package com.example.myapplication.features.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.data.model.EntrantLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

/**
 * Entrant Map Screen. Allows an event organizer to view the the locations
 * of entrants if they wish to do so.
 */
public class OEntrantMapFrag extends Fragment implements OnMapReadyCallback {

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    private MapView mapView;
    private GoogleMap googleMap;
    private String eventId;

    private final FirebaseEventRepository eventRepo = new FirebaseEventRepository();

    /**
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.  The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_o_entrant_map, container, false);
    }

    /**
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventId = getArguments() != null ? getArguments().getString("eventId") : null;
        if (eventId == null) {
            Toast.makeText(requireContext(), "Event not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        mapView = view.findViewById(R.id.entrantMapView);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
    }

    /**
     * @param gMap
     * The Google Map that's being displayed.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap gMap) {
        googleMap = gMap;
        loadEntrantLocations();
    }

    /**
     * Loads the locations of entrants into the map.
     */
    private void loadEntrantLocations() {
        eventRepo.getWaitlistLocations(eventId, new FirebaseEventRepository.WaitlistLocationCallback() {
            @Override
            public void onLocationsFetched(List<EntrantLocation> locations) {
                if (!isAdded() || googleMap == null) return;

                if (locations.isEmpty()) {
                    Toast.makeText(requireContext(),
                            "No location data for this event yet.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

                int idx = 1;
                for (EntrantLocation loc : locations) {
                    LatLng point = new LatLng(loc.getLat(), loc.getLng());
                    googleMap.addMarker(new MarkerOptions()
                            .position(point)
                            .title("Entrant " + idx));
                    boundsBuilder.include(point);
                    idx++;
                }

                LatLngBounds bounds = boundsBuilder.build();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        "Failed to load locations: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     *  Called when the fragment is visible to the user and actively running.
     */
    // MapView lifecycle hooks
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    /**
     *  Called when the fragment is no longer resumed.
     */
    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    /**
     *  Called when the fragment is no longer started.
     */
    @Override
    public void onStop() {
        if (mapView != null) mapView.onStop();
        super.onStop();
    }

    /**
     *  Called when the fragment is no longer visible to the user.
     */
    @Override
    public void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    /**
     *  Called when the fragment is no longer in the foreground.
     */
    @Override
    public void onDestroyView() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroyView();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    /**
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
            if (mapViewBundle == null) {
                mapViewBundle = new Bundle();
                outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
            }
            mapView.onSaveInstanceState(mapViewBundle);
        }
    }
}
