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
        if (current == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        String memberId = current.getId();
        String memberName = (current.getName() != null && !current.getName().isEmpty())
                ? current.getName() : "Member";

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
        // Minimal 1x1 GIF / JPEG payload
        return new byte[]{
                (byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x39, (byte) 0x61,
                (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x80, (byte) 0x00,
                (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x21, (byte) 0xF9, (byte) 0x04, (byte) 0x01, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x2C, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00,
                (byte) 0x00, (byte) 0x02, (byte) 0x02, (byte) 0x44, (byte) 0x01, (byte) 0x00,
                (byte) 0x3B
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
