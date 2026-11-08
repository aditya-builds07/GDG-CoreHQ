package com.gdgcorehq.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.gdgcorehq.app.data.MockDataRepository;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.databinding.ActivityNewExpenseBinding;
import com.gdgcorehq.app.model.Reimbursement;
import com.gdgcorehq.app.model.User;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Core Member New Expense / Receipt Capture Activity.
 * Strictly adheres to PRD §3.5 and PROJECT_RULES R22:
 * Members capture a real receipt photo via camera (TakePicture + FileProvider)
 * or choose from gallery, compressed to JPEG (max ~1600px, quality ~80),
 * specify amount, date, and category, submitting the record as Pending for Admin review.
 */
public class NewExpenseActivity extends AppCompatActivity {

    private ActivityNewExpenseBinding binding;
    private String attachedReceiptName = null;
    private byte[] receiptBytes = null;
    private Uri tempCameraUri = null;

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), isSuccess -> {
                if (Boolean.TRUE.equals(isSuccess) && tempCameraUri != null) {
                    processImageUri(tempCameraUri);
                }
            });

    private final ActivityResultLauncher<String> pickGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processImageUri(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewExpenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Default to today's date
        String today = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
        binding.etDate.setText(today);

        setupListeners();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Camera or Gallery Receipt Capture
        binding.cardCaptureReceipt.setOnClickListener(v -> showImageSourceDialog());

        binding.btnSubmitExpense.setOnClickListener(v -> submitExpense());
    }

    private void showImageSourceDialog() {
        CharSequence[] options = {"Take Photo with Camera", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Attach Receipt Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        launchCamera();
                    } else {
                        pickGalleryLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void launchCamera() {
        try {
            File cacheDir = new File(getCacheDir(), "receipts");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            File photoFile = new File(cacheDir, "receipt_capture_" + System.currentTimeMillis() + ".jpg");
            tempCameraUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );
            takePictureLauncher.launch(tempCameraUri);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to initialize camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processImageUri(Uri uri) {
        try {
            // 1. Decode image bounds
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, boundsOptions);
            }

            int origWidth = boundsOptions.outWidth;
            int origHeight = boundsOptions.outHeight;
            if (origWidth <= 0 || origHeight <= 0) {
                Toast.makeText(this, "Unable to read image dimensions.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Compute sample size to avoid OOM
            int inSampleSize = 1;
            int maxDim = Math.max(origWidth, origHeight);
            while (maxDim / (inSampleSize * 2) >= 1600) {
                inSampleSize *= 2;
            }

            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = inSampleSize;

            Bitmap bitmap;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(is, null, decodeOptions);
            }

            if (bitmap == null) {
                Toast.makeText(this, "Failed to decode receipt image.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. Scale down exactly so max dimension is <= 1600px
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > 1600 || height > 1600) {
                float scale = Math.min(1600f / width, 1600f / height);
                int targetWidth = Math.round(width * scale);
                int targetHeight = Math.round(height * scale);
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
                if (scaled != bitmap) {
                    bitmap.recycle();
                    bitmap = scaled;
                }
            }

            // 4. Compress to JPEG (quality ~80)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            receiptBytes = baos.toByteArray();
            attachedReceiptName = "receipt_" + System.currentTimeMillis() + ".jpg";

            // 5. Update UI with thumbnail
            binding.ivReceiptStatusIcon.setImageTintList(null);
            binding.ivReceiptStatusIcon.setImageBitmap(bitmap);
            binding.tvReceiptStatus.setText("Receipt Image Attached");
            binding.tvReceiptStatus.setTextColor(getColor(R.color.status_success));
            binding.tvReceiptDetails.setText(attachedReceiptName + " (" + (receiptBytes.length / 1024) + " KB, Ready to upload)");
            Toast.makeText(this, "Receipt photo attached.", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error processing receipt image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void submitExpense() {
        String amountStr = binding.etAmount.getText() != null ?
                binding.etAmount.getText().toString().trim() : "";
        String category = binding.etCategory.getText() != null ?
                binding.etCategory.getText().toString().trim() : "";
        String date = binding.etDate.getText() != null ?
                binding.etDate.getText().toString().trim() : "";

        // Strictly enforce receipt requirement per PRD §3.5
        if (receiptBytes == null || attachedReceiptName == null) {
            Toast.makeText(this, "Receipt photo is required. Please tap the box above to capture or select a receipt.", Toast.LENGTH_LONG).show();
            return;
        }

        if (amountStr.isEmpty()) {
            binding.tilAmount.setError("Please enter the expense amount");
            return;
        } else {
            binding.tilAmount.setError(null);
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                binding.tilAmount.setError("Amount must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            binding.tilAmount.setError("Invalid amount format");
            return;
        }

        if (category.isEmpty()) {
            binding.tilCategory.setError("Category is required");
            return;
        } else {
            binding.tilCategory.setError(null);
        }

        User current = SessionManager.getInstance().getCurrentUser();
        if (current == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        String memberId = current.getId();
        String memberName = (current.getName() != null && !current.getName().isEmpty())
                ? current.getName() : "Member";

        // Provide clear loading state and disable submit button (Rule R23)
        binding.btnSubmitExpense.setEnabled(false);
        binding.progressBarExpense.setVisibility(View.VISIBLE);

        final byte[] bytesToUpload = receiptBytes;

        MockDataRepository.getInstance().uploadReceiptImage(memberId, bytesToUpload, attachedReceiptName, new com.gdgcorehq.app.data.FirebaseDataRepository.UploadCallback() {
            @Override
            public void onSuccess(String downloadUri) {
                Reimbursement expense = new Reimbursement(
                        "exp_" + System.currentTimeMillis(),
                        memberId,
                        memberName,
                        amount,
                        date.isEmpty() ? "Today" : date,
                        category,
                        downloadUri,
                        Reimbursement.Status.PENDING,
                        null
                );

                MockDataRepository.getInstance().submitReimbursement(expense);
                binding.progressBarExpense.setVisibility(View.GONE);
                Toast.makeText(NewExpenseActivity.this, "Expense submitted as Pending for Admin review.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String message) {
                binding.btnSubmitExpense.setEnabled(true);
                binding.progressBarExpense.setVisibility(View.GONE);
                Toast.makeText(NewExpenseActivity.this, "Failed to upload receipt: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
