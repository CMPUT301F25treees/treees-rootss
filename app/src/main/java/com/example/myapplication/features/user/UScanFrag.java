package com.example.myapplication.features.user;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.R;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

/**
 * {@code UScanFrag} handles QR code scanning functionality for users.
 * <p>
 * This fragment uses the ZXing {@link DecoratedBarcodeView} to scan QR codes continuously
 * and automatically navigate to the corresponding event detail page upon successful scan.
 * <p>
 * The scanned QR code should contain an {@code eventId} that is used to load
 * the event details in {@link com.example.myapplication.features.user.UEventDetailFrag}.
 * <p>
 * It also manages camera permission requests at runtime, ensuring smooth camera access.
 */
public class UScanFrag extends Fragment {
    /** The barcode scanner view component */
    private DecoratedBarcodeView barcodeView;
    /** Permission request code for camera access */
    private static final int CAMERA_REQUEST_CODE = 101;

    /**
     * Default public constructor for the fragment.
     * Inflates {@code R.layout.fragment_u_scan} as the associated view.
     */
    public UScanFrag() {
        super(R.layout.fragment_u_scan);
    }

    /**
     * Called immediately after the view hierarchy associated with this fragment has been created.
     * <p>
     * Sets up the continuous barcode scanner and defines its callback behavior.
     *
     * @param view               the view returned by {@link #onCreateView}
     * @param savedInstanceState previously saved state, if any
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup Scanner
        barcodeView = view.findViewById(R.id.barcode_scanner);

        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result == null) return;

                // stop more callbacks
                barcodeView.pause();

                String eventId = result.getText();  // QR contains eventId

                Bundle args = new Bundle();
                args.putString("eventId", eventId);

                NavHostFragment.findNavController(UScanFrag.this)
                        .navigate(R.id.action_UScanFrag_to_UEventDetailFrag, args);
            }

        });
    }

    /**
     * Called when the fragment becomes visible.
     * <p>
     * Checks and requests camera permission if necessary.
     * Resumes barcode scanning once permission is granted.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
    }

    /**
     * Called when the fragment is no longer interacting with the user.
     * <p>
     * Pauses the camera to conserve resources.
     */
    @Override
    public void onPause() {
        if (barcodeView != null) barcodeView.pause();
        super.onPause();
    }

    /**
     * Handles the result of the runtime camera permission request.
     *
     * @param requestCode  the request code passed in {@link #requestPermissions(String[], int)}
     * @param permissions  the requested permissions
     * @param grantResults the corresponding grant results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Start Camera
                barcodeView.resume();
            } else {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
