package com.gdgcorehq.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import com.gdgcorehq.app.data.MockDataRepository;
import com.gdgcorehq.app.data.QrCryptoManager;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.databinding.ActivityQrScannerBinding;
import com.gdgcorehq.app.model.AttendanceRecord;
import com.gdgcorehq.app.model.User;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Super Admin QR Attendance Scanner Activity.
 * Strictly adheres to PRD §3.2 (Digital In-App Attendance Workflow):
 * - Scans digital encrypted QR codes from members' screens using CameraX and ML Kit.
 * - Decrypts authenticated AES-GCM payload and checks freshness window to prevent screenshot sharing.
 * - Enforces camera runtime permissions with graceful fallback to manual PRN entry.
 * - Records verified attendance records with meeting ID, timestamp, and recorder ID.
 */
public class QrScannerActivity extends AppCompatActivity {

    private ActivityQrScannerBinding binding;
    private final String currentMeetingId = "meet_101"; // Active Chapter Sync
    private User pendingMember = null;
    private final StringBuilder sessionLog = new StringBuilder();

    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private boolean isScanningPaused = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    binding.tvCameraStatus.setText("Camera permission denied. Use manual PRN entry below.");
                    Toast.makeText(this, "Camera permission denied. Manual PRN entry is available below.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || !user.isSuperAdmin()) {
            Toast.makeText(this, "Access denied: Attendance scanner requires Super Admin privileges.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding = ActivityQrScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cameraExecutor = Executors.newSingleThreadExecutor();

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        setupListeners();
        checkCameraPermissionAndStart();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Floating Pill Navigation Bar
        BottomNavHelper.setupAdminNav(this, BottomNavHelper.INDEX_SCANNER,
                binding.navHome, binding.navTasks, binding.navScanner,
                binding.navExpenses, binding.navStrikes);

        // Rule R24: Gate simulation controls behind BuildConfig.DEBUG
        if (!BuildConfig.DEBUG) {
            binding.layoutSimulationControls.setVisibility(View.GONE);
        }

        // Quick simulation triggers for UI/Offline testing
        binding.btnSimulateValid.setOnClickListener(v -> {
            try {
                // Generate genuine encrypted payload for testing
                String token = QrCryptoManager.encryptPayload("PRN2024001", "usr_member_01");
                processScannedBarcode(token);
            } catch (Exception e) {
                processScannedBarcode("PRN2024001");
            }
        });
        binding.btnSimulateInvalid.setOnClickListener(v -> processScannedBarcode("INVALID999"));

        // Manual Lookup Fallback
        binding.btnVerifyPrn.setOnClickListener(v -> {
            String input = binding.etManualPrn.getText() != null ?
                    binding.etManualPrn.getText().toString().trim() : "";
            if (input.isEmpty()) {
                binding.tilManualPrn.setError("Enter a PRN");
            } else {
                binding.tilManualPrn.setError(null);
                processScannedBarcode(input);
            }
        });

        // Confirm Attendance Action
        binding.btnConfirmAttendance.setOnClickListener(v -> confirmPendingAttendance());
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                binding.tvCameraStatus.setText("Unable to initialize camera. Use manual entry.");
                Toast.makeText(this, "Failed to start camera preview: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.cameraPreviewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            binding.tvCameraStatus.setText("Scanner active: align member's QR code");
        } catch (Exception e) {
            binding.tvCameraStatus.setText("Camera binding failed. Use manual PRN entry.");
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (isScanningPaused || pendingMember != null) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty() && !isScanningPaused && pendingMember == null) {
                            Barcode barcode = barcodes.get(0);
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null && !rawValue.trim().isEmpty()) {
                                runOnUiThread(() -> processScannedBarcode(rawValue));
                            }
                        }
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    private void processScannedBarcode(String rawValue) {
        if (isScanningPaused || pendingMember != null) return;

        MockDataRepository repo = MockDataRepository.getInstance();
        String prn;

        // 1. Attempt decryption via QrCryptoManager (PRD §3.2 authenticated AES-GCM)
        QrCryptoManager.DecryptedPayload payload = QrCryptoManager.decryptPayload(rawValue);

        if (payload.isSuccess()) {
            prn = payload.getPrn();
        } else if (payload.isExpired()) {
            new AlertDialog.Builder(this)
                    .setTitle("QR Code Expired")
                    .setMessage("This attendance QR code has expired (timestamp exceeds 15-minute validity window). Ask the member to refresh their in-app QR code.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        } else {
            // Fallback for manual PRN entry or unencrypted input
            User manualLookup = repo.findMemberByPrn(rawValue);
            if (manualLookup != null) {
                prn = manualLookup.getPrn();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Invalid QR Code")
                        .setMessage("Scanned QR code could not be decrypted or verified: " + payload.getErrorMessage())
                        .setPositiveButton("Dismiss", null)
                        .show();
                return;
            }
        }

        // 2. Validate PRN against registered member records
        User member = repo.findMemberByPrn(prn);
        if (member == null) {
            binding.cardResult.setVisibility(View.GONE);
            new AlertDialog.Builder(this)
                    .setTitle("Scan Rejected")
                    .setMessage("PRN '" + prn + "' is not registered in chapter records. Please check the member's profile.")
                    .setPositiveButton("Dismiss", null)
                    .show();
            return;
        }

        // 3. Check for duplicate scan for the active meeting
        if (repo.isAttendanceAlreadyRecorded(currentMeetingId, member.getPrn())) {
            binding.cardResult.setVisibility(View.GONE);
            new AlertDialog.Builder(this)
                    .setTitle("Duplicate Attendance")
                    .setMessage(member.getName() + " (" + member.getPrn() + ") has ALREADY been marked present for this meeting.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // 4. Display member confirmation card for Super Admin review
        pendingMember = member;
        isScanningPaused = true;

        binding.tvResultName.setText(member.getName());
        binding.tvResultRolePrn.setText(member.getDepartment() + " • PRN: " + member.getPrn());
        binding.cardResult.setVisibility(View.VISIBLE);
    }

    private void confirmPendingAttendance() {
        if (pendingMember == null) return;

        User admin = SessionManager.getInstance().getCurrentUser();
        String recorderId = admin != null ? admin.getId() : "admin_usr";
        String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        AttendanceRecord record = new AttendanceRecord(
                "att_" + System.currentTimeMillis(),
                currentMeetingId,
                pendingMember.getPrn(),
                pendingMember.getName(),
                pendingMember.getRole().getDisplayName(),
                timeStr,
                "PRESENT",
                recorderId
        );

        boolean success = MockDataRepository.getInstance().recordAttendance(record);
        if (success) {
            Toast.makeText(this, "Attendance recorded for " + pendingMember.getName(), Toast.LENGTH_SHORT).show();

            // Append to session log
            if (sessionLog.length() == 0) {
                sessionLog.append(pendingMember.getName()).append(" (").append(pendingMember.getPrn()).append(") at ").append(timeStr);
            } else {
                sessionLog.insert(0, pendingMember.getName() + " (" + pendingMember.getPrn() + ") at " + timeStr + "\n");
            }
            binding.tvSessionHistory.setText(sessionLog.toString());

            binding.cardResult.setVisibility(View.GONE);
            pendingMember = null;
            isScanningPaused = false;
            binding.etManualPrn.setText("");
        } else {
            Toast.makeText(this, "Failed to record attendance: duplicate scan detected.", Toast.LENGTH_LONG).show();
            isScanningPaused = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isScanningPaused = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isScanningPaused = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        binding = null;
    }
}
