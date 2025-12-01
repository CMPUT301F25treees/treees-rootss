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
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.R;
import com.example.myapplication.core.ServiceLocator;
import com.example.myapplication.core.UserSession;
import com.example.myapplication.data.model.User;
import com.example.myapplication.data.repo.ImageRepository;
import com.example.myapplication.features.user.UserEvent;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.example.myapplication.core.ImageUtils;

/**
 * This class is for users to be able to create new events when they are in their
 * organizer state. Event details are collected and validated such as: Title, Address,
 * Description, Capacity, Price, start data, end date, selection data, and image.
 * After that the image gets uploaded and the new event is created and saved to Firestore.
 */
public class OCreateEventFrag extends Fragment {

    /** Request code for image picker intent */
    private static final int REQ_POSTER = 1001;

    // Create Event Inputs
    private EditText titleInput;
    private EditText addressInput;
    private EditText descInput;
    private EditText capacityInput;
    private EditText entrantsDrawnInput;
    private EditText priceInput;
    private MaterialAutoCompleteTextView themeInput;

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
    private String selectedTheme = "";

    private long startDateMillis = 0;
    private long endDateMillis = 0;
    private long selectionDateMillis = 0;
    private Uri posterUri = null;

    private ImageRepository imageRepository;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    /** Default constructor
     * */
    public OCreateEventFrag(){}

    /**
     * This method inflates the layout for the fragment.
     *
     * @param inflater LayoutInflater object that can be used to inflate any views in the fragment
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_o_create_event, container, false);
    }

    /**
     * This method gets called after the view has been created. Initializes input fields,
     * buttons, labels, and their listeners.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
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
        themeInput = view.findViewById(R.id.event_theme_input);

        ArrayAdapter<CharSequence> themeAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.event_theme_options,
                android.R.layout.simple_dropdown_item_1line
        );
        themeInput.setAdapter(themeAdapter);
        themeInput.setOnItemClickListener((parent, itemView, position, id) ->
                selectedTheme = parent.getItemAtPosition(position).toString());
        themeInput.setOnClickListener(v -> themeInput.showDropDown());

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

    /**
     * This method opens a date picker dialog and returns the selected date through a callback.
     *
     * @param callback Receives the selected date.
     */
    private void pickDate(DateCallback callback) {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                R.style.MyDatePickerTheme,
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

    /**
     * Launches the image picker so the user can select an image to upload.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select poster"), REQ_POSTER);
    }

    /**
     * This method handles the results from the image picker.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     *
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_POSTER && resultCode == Activity.RESULT_OK && data != null) {
            posterUri = data.getData();
            Toast.makeText(getContext(), "Poster selected", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This method collects all teh user inputs and validates them. After that a new
     * event object is created. Before saving the object to Firestore, if there is an
     * image it gets uploaded. Then the event is saved with the imageUrl that gets
     * returned after upload.
     *
     * ImageRepository is used to upload the image.
     *
     */
    private void onCreateClicked() {
        Log.d("OCreateEventFrag", "onCreateClicked Called");
        String title = titleInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String descr = descInput.getText().toString().trim();
        String capacityStr = capacityInput.getText().toString().trim();
        String priceStr = priceInput.getText().toString().trim();
        String entrantsStr = entrantsDrawnInput.getText().toString().trim();
        String theme = themeInput.getText() != null
                ? themeInput.getText().toString().trim()
                : "";

        // basic validation
        if (title.isEmpty() || address.isEmpty() || descr.isEmpty()
                || entrantsStr.isEmpty() || theme.isEmpty()
                || startDateMillis == 0 || endDateMillis == 0 || selectionDateMillis == 0) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedTheme = theme;

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
                price = Integer.parseInt(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Price must be a number", Toast.LENGTH_SHORT).show();
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
        event.setTheme(selectedTheme);

        User user = UserSession.getInstance().getCurrentUser();
        event.setInstructor(user.getUsername());

        // If no poster is selected, use the default profile image
        if (posterUri == null) {
            posterUri = ImageUtils.createDefaultPosterUri(requireContext());
        }

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

    /**
     * Helper method that clears all user-input fields in the Create Event form.
     */
    private void clearForm() {
        // Text inputs
        titleInput.setText("");
        addressInput.setText("");
        descInput.setText("");
        capacityInput.setText("");
        priceInput.setText("");
        entrantsDrawnInput.setText("");
        themeInput.setText("");
        selectedTheme = "";

    }


    /**
     * This method uses EventRepository to save the event to Firestore. Depending
     * whether saving is successful or not, a message gets displayed.
     *
     * @param event event the event object to be saved.
     */
    private void saveEvent(UserEvent event) {
        ServiceLocator.getEventRepository().createEvent(
                requireContext(),
                event,
                aVoid -> {
                    Toast.makeText(getContext(), "Event created!", Toast.LENGTH_SHORT).show();

                    clearForm();

                    Bundle bundle = new Bundle();
                    bundle.putString("eventId", event.getId());

                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_navigation_organizer_create_to_navigation_organizer_event_detail, bundle);
                },
                e -> Toast.makeText(getContext(),
                        "Failed to create event: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

}
