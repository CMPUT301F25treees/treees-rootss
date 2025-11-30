package com.example.myapplication.features.user;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.features.user.UserEvent;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Displays event details for entrants, allows joining the waitlist,
 * and captures optional geolocation.
 */
public class UEventDetailFrag extends Fragment {

    private static final int LOCATION_REQUEST_CODE = 201;

    private TextView title, organizer, location, price, endTime, descr, waitingList;

    private MaterialButton joinWaitlistBtn;
    private boolean inWaitlist = false;

    private String eventId;

    // Whether geolocation is required for this event — set when binding event data
    private boolean geoRequired = false;

    private FusedLocationProviderClient fusedLocationClient;


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
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_u_event_detail, container, false);
    }


    /**
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        title = view.findViewById(R.id.EventTitle);
        waitingList = view.findViewById(R.id.WaitinglistText);
        organizer = view.findViewById(R.id.OrganizerTitle);
        location = view.findViewById(R.id.addressText);
        price = view.findViewById(R.id.price);
        endTime = view.findViewById(R.id.endTime);
        descr = view.findViewById(R.id.description);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Back button
        ImageButton backButton = view.findViewById(R.id.bckButton);
        backButton.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        eventId = getArguments() != null ? getArguments().getString("eventId") : null;

        if (eventId == null) {
            Toast.makeText(requireContext(), "Event not found.", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
            return;
        }

        // Load event details
        FirebaseEventRepository repo = new FirebaseEventRepository();
        repo.fetchEventById(eventId, new FirebaseEventRepository.SingleEventCallback() {
            @Override
            public void onEventFetched(UserEvent event) {
                bindEventData(event);

                // Geo requirement flag (if your UserEvent supports it)
                geoRequired = event.isGeoRequired();   // <-- Adjust if getter name differs
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Could not load event.", Toast.LENGTH_SHORT).show();
            }
        });


        // Join waitlist button
        joinWaitlistBtn = view.findViewById(R.id.joinWaitlist);
        joinWaitlistBtn.setOnClickListener(x -> {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(getContext(), "Please log in first!", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = user.getUid();

            if (inWaitlist){
                repo.leaveWaitlist(eventId, uid, v -> {
                    Toast.makeText(getContext(), "You have left the waitlist.", Toast.LENGTH_SHORT).show();

                    inWaitlist = false;
                    if (joinWaitlistBtn !=null){
                        joinWaitlistBtn.setText("Join Waitlist");
                    }
                    refreshEventDetail(eventId);
                }, e -> {
                    Toast.makeText(getContext(), "Could not leave waitlist", Toast.LENGTH_SHORT).show();
                });
            } else {
                if (geoRequired && !hasLocationPermission()) {
                    requestPermissions(
                            new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                            LOCATION_REQUEST_CODE
                    );
                    return;
                }
                captureLocationAndJoin(uid);
            }
        });
    }


    /** Bind event details to the UI */
    private void bindEventData(UserEvent event) {
        long millisLeft = event.getEndTimeMillis() - System.currentTimeMillis();
        long daysLeft = (long) Math.ceil(millisLeft / (1000.0 * 60 * 60 * 24));

        title.setText(event.getName());
        organizer.setText("Organizer: " + event.getInstructor());
        location.setText(event.getLocation());

        String priceText = event.getPriceDisplay();
        if (TextUtils.isEmpty(priceText)) {
            price.setText(getString(R.string.event_price_unavailable));
        } else {
            price.setText(getString(R.string.event_price_label, priceText));
        }

        descr.setText(event.getDescr());
        endTime.setText("Days Left: " + Math.max(daysLeft, 0));

        waitingList.setText("Currently in Waitinglist: " +
                (event.getWaitlist() != null ? event.getWaitlist().size() : 0));

        ImageView imageView = requireView().findViewById(R.id.eventImage);
        ImageView qrImageView = requireView().findViewById(R.id.qrCodeImage);

        if (event.getImageUrl() != null)
            Glide.with(this).load(event.getImageUrl()).into(imageView);

        if (qrImageView != null) {
            if (event.getQrData() != null) {
                Glide.with(this).load(event.getQrData()).into(qrImageView);

                /**
                 * Allows the user to save the QR code image by tapping on it.
                 * The QR drawable inside the ImageView is converted into a Bitmap
                 * and stored into the device's gallery.
                 */
                qrImageView.setOnClickListener(v -> {
                    Bitmap bmp = getBitmapFromImageView(qrImageView);
                    if (bmp == null) {
                        Toast.makeText(requireContext(), "No QR to save.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Uri uri = saveBitmapToGallery(bmp, "event_" + eventId + "_qr");
                    if (uri != null) {
                        Toast.makeText(requireContext(), "QR saved to Photos.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to save QR.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                qrImageView.setOnClickListener(null);
            }
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user!=null) ? user.getUid() : null;

        if( uid != null && event.getWaitlist() != null && event.getWaitlist().contains(uid)){
            inWaitlist = true;
            joinWaitlistBtn.setText("Leave Waitlist");
        } else {
            inWaitlist = false;
            joinWaitlistBtn.setText("Join Waitlist");
        }

        RatingController ratingController = new RatingController();
        ratingController.fetchOrganizerRating(event.getOrganizerID(), new RatingController.OnRatingFetchedListener() {
            @Override
            public void onRatingFetched(double rating) {
                updateStars(rating);
            }

            @Override
            public void onError(Exception e) {
                // Optional: handle error or just show empty stars
            }
        });
    }

    private void updateStars(double rating) {
        if (!isAdded() || getView() == null) return;
        
        int ratingInt = (int) Math.round(rating);
        int[] starIds = {R.id.star1, R.id.star2, R.id.star3, R.id.star4, R.id.star5};

        for (int i = 0; i < starIds.length; i++) {
            ImageView star = getView().findViewById(starIds[i]);
            if (star != null) {
                if (i < ratingInt) {
                    star.setImageResource(R.drawable.star_filled);
                } else {
                    star.setImageResource(R.drawable.star_empty);
                }
            }
        }
    }


    /**
     * @param eventId
     */
    private void refreshEventDetail(String eventId) {
        FirebaseEventRepository repo = new FirebaseEventRepository();
        repo.fetchEventById(eventId, new FirebaseEventRepository.SingleEventCallback() {
            @Override
            public void onEventFetched(UserEvent event) {
                bindEventData(event);
            }

            @Override
            public void onError(Exception e) { }
        });
    }


    /**
     * @return
     */
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * @param uid
     */
    private void captureLocationAndJoin(String uid) {

        FirebaseEventRepository repo = new FirebaseEventRepository();

        // If location NOT required → join immediately
        if (!geoRequired || !hasLocationPermission()) {
            joinWithLocation(repo, uid, null, null);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        joinWithLocation(repo, uid,
                                location.getLatitude(),
                                location.getLongitude());
                    } else {
                        Toast.makeText(
                                getContext(),
                                "Could not get location. Try again outdoors.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Location failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }


    /**
     * Joins the waitlist with optional location data.
     * @param repo
     * @param uid
     * @param lat
     * @param lng
     */
    private void joinWithLocation(FirebaseEventRepository repo,
                                  String uid,
                                  @Nullable Double lat,
                                  @Nullable Double lng) {

        repo.joinWaitlist(eventId, uid, lat, lng, a -> {

            repo.fetchEventById(eventId, new FirebaseEventRepository.SingleEventCallback() {
                @Override
                public void onEventFetched(UserEvent event) {
                    showWaitlistInfoDialog(event.getEntrantsToDraw());
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(),
                            "Unable to reload event.",
                            Toast.LENGTH_SHORT).show();
                }
            });

            refreshEventDetail(eventId);

        }, e -> Toast.makeText(getContext(),
                "Could not join waitlist.",
                Toast.LENGTH_SHORT).show());
    }

    /**
     * Extracts a Bitmap from the drawable currently displayed in the specified ImageView.
     * <p>
     * If the drawable is already a BitmapDrawable, its bitmap is returned directly.
     * If the drawable is a vector or another type of drawable, it is drawn onto a newly
     * created Bitmap using a Canvas.
     *
     * @param imageView The ImageView containing the QR code drawable.
     * @return A Bitmap representation of the ImageView's drawable, or {@code null} if no drawable exists.
     */
    @Nullable
    private Bitmap getBitmapFromImageView(@NonNull ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else {
            Bitmap bmp = Bitmap.createBitmap(
                    imageView.getWidth(),
                    imageView.getHeight(),
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bmp);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bmp;
        }
    }

    /**
     * Saves a Bitmap image into the user's device gallery using the MediaStore API.
     * <p>
     * The image is saved as a PNG file with the provided base file name.
     *
     * @param bitmap   The Bitmap to save.
     * @param fileName Desired base file name (without extension).
     * @return A {@link Uri} referencing the saved image, or {@code null} if saving failed.
     */
    @Nullable
    private Uri saveBitmapToGallery(@NonNull Bitmap bitmap, @NonNull String fileName) {
        ContentResolver resolver = requireContext().getContentResolver();

        Uri imagesUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            imagesUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

        // Save into a custom folder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EventLottery");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri uri = resolver.insert(imagesUri, values);
        if (uri == null) {
            return null;
        }

        OutputStream out = null;
        try {
            out = resolver.openOutputStream(uri);
            if (out != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
            }

            return uri;

        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) { }
            }
        }
    }

    /**
     * Displays the custom dialog pop up with the details of the draw for teh
     * specified event.
     *
     * @param entrantsToDraw the number of people to be drawn
     */
    private void showWaitlistInfoDialog(int entrantsToDraw) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_waitlist_info);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );
        }

        TextView infoText = dialog.findViewById(R.id.infoText);
        MaterialButton okButton = dialog.findViewById(R.id.okButton);

        String message = "Thank you for joining the waitlist.\n" + entrantsToDraw + " entrants will be selected at random " + "from the total pool.";

        infoText.setText(message);

        okButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    /**
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *                     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null)
                    captureLocationAndJoin(user.getUid());

            } else {
                Toast.makeText(requireContext(),
                        "Location permission denied.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
