package com.example.myapplication.features.organizer;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.core.ServiceLocator;
import com.example.myapplication.core.UserSession;
import com.example.myapplication.data.model.Event;
import com.example.myapplication.data.repo.EventRepository;
import com.example.myapplication.data.repo.ImageRepository;
import com.example.myapplication.features.user.UserEvent;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class OCreateEventFrag extends Fragment {

    private static final int REQ_POSTER = 1001;

    // Create Event Inputs
    private EditText titleInput;
    private EditText addressInput;
    private EditText descInput;
    private EditText capacityInput;
    private EditText entrantsDrawnInput;
    private EditText priceInput;

    // Dates
    private TextView startDateLabel;
    private TextView endDateLabel;
    private TextView selectionDateLabel;

    private ImageButton startDateButton;
    private ImageButton endDateButton;
    private ImageButton selectionDateButton;
    private ImageButton insertPosterButton;
    private Switch geoSwitch;
    private MaterialButton createButton;

    private long startDateMillis = 0;
    private long endDateMillis = 0;
    private long selectionDateMillis = 0;
    private Uri posterUri = null;

    private ImageRepository imageRepository;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public OCreateEventFrag(){}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_o_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        imageRepository = new ImageRepository();

        // Inputs
        titleInput = view.findViewById(R.id.event_title_input);
        addressInput = view.findViewById(R.id.event_address_input);
        descInput = view.findViewById(R.id.event_desc_input);
        capacityInput = view.findViewById(R.id.event_cap_input);
        priceInput = view.findViewById(R.id.event_price_input);
        entrantsDrawnInput = view.findViewById(R.id.entrants_drawn_input);

        // Buttons and Switch
        startDateButton = view.findViewById(R.id.startDateButton);
        endDateButton = view.findViewById(R.id.endDateButton);
        selectionDateButton = view.findViewById(R.id.selectionDateButton);
        insertPosterButton = view.findViewById(R.id.insertPosterButton);
        geoSwitch = view.findViewById(R.id.switchGeoLocation);
        createButton = view.findViewById(R.id.createEventButton);

        // Labels
        startDateLabel = view.findViewById(R.id.createEventStartDate);
        endDateLabel = view.findViewById(R.id.createEventEndDate);
        selectionDateLabel = view.findViewById(R.id.createEventSelectionDate);

        // Date pickers
        startDateButton.setOnClickListener(v -> pickDate((millis) -> {
            startDateMillis = millis;
            startDateLabel.setText(
                    getString(R.string.event_start_date_title_text) + "  " + dateFormat.format(millis)
            );
        }));

        endDateButton.setOnClickListener(v -> pickDate((millis) -> {
            endDateMillis = millis;
            endDateLabel.setText(
                    getString(R.string.event_end_date_title_text) + "  " + dateFormat.format(millis)
            );
        }));

        selectionDateButton.setOnClickListener(v -> pickDate((millis) -> {
            selectionDateMillis = millis;
            selectionDateLabel.setText(
                    getString(R.string.event_draw_date_title_text) + "  " + dateFormat.format(millis)
            );
        }));

        // Poster picker
        insertPosterButton.setOnClickListener(v -> openImagePicker());

        // Create button
        createButton.setOnClickListener(v -> onCreateClicked());


    }

    // Small callback interface for date selection
    private interface DateCallback {
        void onDateChosen(long millis);
    }

    private void pickDate(DateCallback callback) {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (DatePicker dp, int year, int month, int dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth, 0, 0, 0);
                    callback.onDateChosen(calendar.getTimeInMillis());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select poster"), REQ_POSTER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_POSTER && resultCode == Activity.RESULT_OK && data != null) {
            posterUri = data.getData();
            Toast.makeText(getContext(), "Poster selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void onCreateClicked() {
        Log.d("OCreateEventFrag", "onCreateClicked Called");
        String title = titleInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String descr = descInput.getText().toString().trim();
        String capacityStr = capacityInput.getText().toString().trim();
        String priceStr = priceInput.getText().toString().trim();
        String entrantsStr = entrantsDrawnInput.getText().toString().trim();

        // basic validation
        if (title.isEmpty() || address.isEmpty() || descr.isEmpty()
                || entrantsStr.isEmpty()
                || startDateMillis == 0 || endDateMillis == 0 || selectionDateMillis == 0) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacity = 0;
        if (!capacityStr.isEmpty()) {
            try {
                capacity = Integer.parseInt(capacityStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Capacity must be a number", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        double price = 0.0;
        if (!priceStr.isEmpty()) {
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Price must be a valid number", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int entrantsToDraw;
        try {
            entrantsToDraw = Integer.parseInt(entrantsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Entrants drawn must be a number", Toast.LENGTH_SHORT).show();
            return;
        }


        UserEvent event = new UserEvent();
        event.setName(title);
        event.setLocation(address);
        event.setDescr(descr);
        event.setCapacity(capacity);
        event.setPrice(price);
        event.setStartTimeMillis(startDateMillis);
        event.setEndTimeMillis(endDateMillis);
        event.setSelectionDateMillis(selectionDateMillis);
        event.setEntrantsToDraw(entrantsToDraw);
        event.setGeoRequired(geoSwitch.isChecked());
        event.setOrganizerID(UserSession.getInstance().getCurrentUser().getUid());


        if (posterUri != null) {
            imageRepository.uploadImage( posterUri, new ImageRepository.UploadCallback() {
                @Override
                public void onSuccess(String secureUrl) {
                    event.setImageUrl((secureUrl));
                    saveEvent(event);
                }

                @Override
                public void onError(String e) {
                    Toast.makeText(getContext(), "Image Upload failed: " + e, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            saveEvent(event);
        }
    }


    private void saveEvent(UserEvent event) {
        ServiceLocator.getEventRepository().createEvent(
                requireContext(),
                event,
                aVoid -> {
                    Toast.makeText(getContext(), "Event created!", Toast.LENGTH_SHORT).show();
                    // TODO: navigate to OEventDetails
                },
                e -> Toast.makeText(getContext(),
                        "Failed to create event: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

}