package com.example.myapplication.features.organizer;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.R;
import com.example.myapplication.core.ServiceLocator;
import com.example.myapplication.core.UserSession;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.data.repo.EventRepository;
import com.example.myapplication.data.repo.ImageRepository;
import com.example.myapplication.features.user.UserEvent;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Organizer edit event screen. Loads an existing event, pre-populates the form, and
 * persists updates back to Firestore when the user taps Update.
 */
public class OEditEventFrag extends Fragment {

    private static final int REQ_POSTER = 2001;

    private TextInputEditText titleInput;
    private TextInputEditText addressInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText capacityInput;
    private TextInputEditText priceInput;
    private TextInputEditText startDateInput;
    private TextInputEditText endDateInput;
    private TextInputEditText selectionDateInput;
    private TextInputEditText entrantsInput;
    private SwitchCompat geoSwitch;
    private MaterialButton startDateButton;
    private MaterialButton endDateButton;
    private MaterialButton selectionDateButton;
    private MaterialButton posterButton;
    private MaterialButton updateButton;
    private MaterialButton backButton;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private long startDateMillis = 0L;
    private long endDateMillis = 0L;
    private long selectionDateMillis = 0L;
    private Uri posterUri;

    private String eventId;
    private UserEvent currentEvent;
    private EventRepository eventRepository;
    private final FirebaseEventRepository firebaseEventRepository = new FirebaseEventRepository();
    private ImageRepository imageRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_o_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventRepository = ServiceLocator.getEventRepository();
        imageRepository = new ImageRepository();

        bindViews(view);
        setupInteractions();

        eventId = getArguments() != null ? getArguments().getString("eventId") : null;
        if (eventId == null) {
            Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        loadEvent();
    }

    private void bindViews(View view) {
        titleInput = view.findViewById(R.id.etTitle);
        addressInput = view.findViewById(R.id.etAddress);
        descriptionInput = view.findViewById(R.id.etDescription);
        capacityInput = view.findViewById(R.id.etCapacity);
        priceInput = view.findViewById(R.id.etPrice);
        startDateInput = view.findViewById(R.id.etStartDate);
        endDateInput = view.findViewById(R.id.etEndDate);
        selectionDateInput = view.findViewById(R.id.etSelectionDate);
        entrantsInput = view.findViewById(R.id.etEntrants);
        geoSwitch = view.findViewById(R.id.swGeoLocation);
        startDateButton = view.findViewById(R.id.btnStartDate);
        endDateButton = view.findViewById(R.id.btnEndDate);
        selectionDateButton = view.findViewById(R.id.btnSelectionDate);
        posterButton = view.findViewById(R.id.btnSelectPoster);
        updateButton = view.findViewById(R.id.btnUpdateEvent);
        backButton = view.findViewById(R.id.btnBack);
    }

    private void setupInteractions() {
        startDateButton.setOnClickListener(v -> pickDate(millis -> {
            startDateMillis = millis;
            startDateInput.setText(dateFormat.format(new Date(millis)));
        }));

        endDateButton.setOnClickListener(v -> pickDate(millis -> {
            endDateMillis = millis;
            endDateInput.setText(dateFormat.format(new Date(millis)));
        }));

        selectionDateButton.setOnClickListener(v -> pickDate(millis -> {
            selectionDateMillis = millis;
            selectionDateInput.setText(dateFormat.format(new Date(millis)));
        }));

        posterButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, getString(R.string.edit_choose_poster)), REQ_POSTER);
        });

        updateButton.setOnClickListener(v -> onUpdateClicked());

        backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private interface DateCallback {
        void onDateChosen(long millis);
    }

    private void pickDate(DateCallback callback) {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth, 0, 0, 0);
                    callback.onDateChosen(calendar.getTimeInMillis());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_POSTER && resultCode == Activity.RESULT_OK && data != null) {
            posterUri = data.getData();
            if (isAdded()) {
                Toast.makeText(requireContext(), "Poster selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadEvent() {
        firebaseEventRepository.fetchEventById(eventId, new FirebaseEventRepository.SingleEventCallback() {
            @Override
            public void onEventFetched(UserEvent event) {
                if (!isAdded()) {
                    return;
                }
                currentEvent = event;
                if (currentEvent == null) {
                    Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(OEditEventFrag.this).popBackStack();
                    return;
                }
                populateForm(currentEvent);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), "Failed to load event", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(OEditEventFrag.this).popBackStack();
            }
        });
    }

    private void populateForm(UserEvent event) {
        titleInput.setText(event.getName());
        addressInput.setText(event.getLocation());
        descriptionInput.setText(event.getDescr());

        if (event.getCapacity() > 0) {
            capacityInput.setText(String.valueOf(event.getCapacity()));
        } else {
            capacityInput.setText("");
        }

        Double priceValue = event.getPrice();
        if (priceValue != null) {
            if (Math.abs(priceValue - Math.round(priceValue)) < 0.005) {
                priceInput.setText(String.valueOf(Math.round(priceValue)));
            } else {
                priceInput.setText(String.format(Locale.getDefault(), "%.2f", priceValue));
            }
        } else {
            priceInput.setText("");
        }

        startDateMillis = event.getStartTimeMillis();
        if (startDateMillis > 0) {
            startDateInput.setText(dateFormat.format(new Date(startDateMillis)));
        }

        endDateMillis = event.getEndTimeMillis();
        if (endDateMillis > 0) {
            endDateInput.setText(dateFormat.format(new Date(endDateMillis)));
        }

        selectionDateMillis = event.getSelectionDateMillis();
        if (selectionDateMillis > 0) {
            selectionDateInput.setText(dateFormat.format(new Date(selectionDateMillis)));
        }

        if (event.getEntrantsToDraw() > 0) {
            entrantsInput.setText(String.valueOf(event.getEntrantsToDraw()));
        } else {
            entrantsInput.setText("");
        }

        geoSwitch.setChecked(event.isGeoRequired());
    }

    private void onUpdateClicked() {
        if (currentEvent == null) {
            Toast.makeText(requireContext(), "Event not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = getTrimmed(titleInput);
        String address = getTrimmed(addressInput);
        String description = getTrimmed(descriptionInput);
        String capacityStr = getTrimmed(capacityInput);
        String priceStr = getTrimmed(priceInput);
        String entrantsStr = getTrimmed(entrantsInput);

        if (title.isEmpty() || address.isEmpty() || description.isEmpty() || priceStr.isEmpty()
                || entrantsStr.isEmpty() || startDateMillis == 0 || endDateMillis == 0 || selectionDateMillis == 0) {
            Toast.makeText(requireContext(), "Please complete all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacity = 0;
        if (!capacityStr.isEmpty()) {
            try {
                capacity = Integer.parseInt(capacityStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Capacity must be numeric", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        double priceValue;
        try {
            priceValue = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid price", Toast.LENGTH_SHORT).show();
            return;
        }

        int entrants;
        try {
            entrants = Integer.parseInt(entrantsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Entrants drawn must be numeric", Toast.LENGTH_SHORT).show();
            return;
        }

        currentEvent.setName(title);
        currentEvent.setLocation(address);
        currentEvent.setDescr(description);
        currentEvent.setCapacity(capacity);
        currentEvent.setPrice(priceValue);
        currentEvent.setStartTimeMillis(startDateMillis);
        currentEvent.setEndTimeMillis(endDateMillis);
        currentEvent.setSelectionDateMillis(selectionDateMillis);
        currentEvent.setEntrantsToDraw(entrants);
        currentEvent.setGeoRequired(geoSwitch.isChecked());

        UserSession session = UserSession.getInstance();
        if (session != null && session.isLoggedIn() && session.getCurrentUser() != null) {
            currentEvent.setOrganizerID(session.getCurrentUser().getUid());
        }

        if (posterUri != null) {
            imageRepository.uploadImage(posterUri, new ImageRepository.UploadCallback() {
                @Override
                public void onSuccess(String secureUrl) {
                    currentEvent.setImageUrl(secureUrl);
                    currentEvent.setPosterUrl(secureUrl);
                    persistChanges();
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(), "Poster upload failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            persistChanges();
        }
    }

    private void persistChanges() {
        eventRepository.updateEvent(eventId, currentEvent, aVoid -> {
            if (!isAdded()) {
                return;
            }
            Toast.makeText(requireContext(), "Event updated", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
        }, e -> {
            if (!isAdded()) {
                return;
            }
            Toast.makeText(requireContext(), "Failed to update event", Toast.LENGTH_SHORT).show();
        });
    }

    private String getTrimmed(TextInputEditText input) {
        return input != null && input.getText() != null ? input.getText().toString().trim() : "";
    }
}
