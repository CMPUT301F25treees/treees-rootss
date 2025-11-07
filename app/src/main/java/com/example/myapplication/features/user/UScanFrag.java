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
 * This class deals with scanning a QR code and identifying which event to navigate to.
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
     * When the fragment becomes visible this method will resume the barcode scanner
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
     * When fragment is not beign shown to teh user the barcode scanner is paused.
     */
    @Override
    public void onPause() {
        if (barcodeView != null) barcodeView.pause();
        super.onPause();
    }

    /**
     * This method handles the camera permission request.
     *
     * @param requestCode The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     *
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
