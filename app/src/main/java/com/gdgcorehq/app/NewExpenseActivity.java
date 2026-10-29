package com.gdgcorehq.app;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.gdgcorehq.app.data.MockDataRepository;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.databinding.ActivityNewExpenseBinding;
import com.gdgcorehq.app.model.Reimbursement;
import com.gdgcorehq.app.model.User;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Core Member New Expense / Receipt Capture Activity.
 * Strictly adheres to PRD §3.5 and PROJECT_RULES R22:
 * Members capture a receipt photo, specify amount, date, and category,
 * submitting the record as Pending for Admin review.
 */
public class NewExpenseActivity extends AppCompatActivity {

    private ActivityNewExpenseBinding binding;
    private String attachedReceiptName = null;
    private byte[] receiptBytes = null;

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

        // Simulated Camera / Receipt Capture
        binding.cardCaptureReceipt.setOnClickListener(v -> {
            attachedReceiptName = "receipt_chapter_" + System.currentTimeMillis() + ".jpg";
            receiptBytes = generateReceiptPlaceholderBytes();
            binding.tvReceiptStatus.setText("Receipt Image Attached");
            binding.tvReceiptStatus.setTextColor(getColor(R.color.status_success));
            binding.tvReceiptDetails.setText(attachedReceiptName + " (Ready to upload)");
            binding.ivReceiptStatusIcon.setImageResource(R.drawable.ic_check);
            binding.ivReceiptStatusIcon.setColorFilter(getColor(R.color.status_success));
            Toast.makeText(this, "Receipt photo captured.", Toast.LENGTH_SHORT).show();
        });

        binding.btnSubmitExpense.setOnClickListener(v -> submitExpense());
    }

    private void submitExpense() {
        String amountStr = binding.etAmount.getText() != null ?
                binding.etAmount.getText().toString().trim() : "";
        String category = binding.etCategory.getText() != null ?
                binding.etCategory.getText().toString().trim() : "";
        String date = binding.etDate.getText() != null ?
                binding.etDate.getText().toString().trim() : "";

        if (attachedReceiptName == null) {
            Toast.makeText(this, "Please tap the box above to capture or attach a receipt photo.", Toast.LENGTH_LONG).show();
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
        String memberId = current != null ? current.getId() : "usr_member_01";
        String memberName = current != null ? current.getName() : "Siddhi Kulkarni";

        // Provide clear loading state and disable submit button (Rule R23)
        binding.btnSubmitExpense.setEnabled(false);
        binding.progressBarExpense.setVisibility(android.view.View.VISIBLE);

        final byte[] bytesToUpload = (receiptBytes != null) ? receiptBytes : generateReceiptPlaceholderBytes();

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
                binding.progressBarExpense.setVisibility(android.view.View.GONE);
                Toast.makeText(NewExpenseActivity.this, "Expense submitted as Pending for Admin review.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String message) {
                binding.btnSubmitExpense.setEnabled(true);
                binding.progressBarExpense.setVisibility(android.view.View.GONE);
                Toast.makeText(NewExpenseActivity.this, "Failed to upload receipt: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private byte[] generateReceiptPlaceholderBytes() {
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(300, 300, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        canvas.drawColor(android.graphics.Color.WHITE);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(android.graphics.Color.DKGRAY);
        paint.setTextSize(24);
        paint.setAntiAlias(true);
        canvas.drawText("GDG CoreHQ Receipt", 30, 80, paint);
        canvas.drawText("Verified Chapter Expense", 30, 140, paint);
        canvas.drawText("TKIET Chapter Sync", 30, 200, paint);

        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream);
        return stream.toByteArray();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
