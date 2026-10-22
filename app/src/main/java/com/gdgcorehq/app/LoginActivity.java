package com.gdgcorehq.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.gdgcorehq.app.data.SessionManager;
import com.gdgcorehq.app.databinding.ActivityLoginBinding;
import com.gdgcorehq.app.model.User;

/**
 * Login screen for GDG CoreHQ.
 * Pre-provisioned authentication flow per PRD §3.1.
 * Routes user to AdminDashboardActivity or MemberDashboardActivity based on assigned role.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    private void setupListeners() {
        binding.btnSignIn.setOnClickListener(v -> handleSignIn());

        binding.tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // Quick role shortcuts for Phase 2 UI Demonstration
        binding.btnRoleAdmin.setOnClickListener(v -> {
            binding.etIdentifier.setText("admin");
            binding.etPassword.setText("admin123");
            handleSignIn();
        });

        binding.btnRoleMember.setOnClickListener(v -> {
            binding.etIdentifier.setText("lead");
            binding.etPassword.setText("lead123");
            handleSignIn();
        });

        binding.btnRoleStrikeMember.setOnClickListener(v -> {
            binding.etIdentifier.setText("strike");
            binding.etPassword.setText("strike123");
            handleSignIn();
        });
    }

    private void handleSignIn() {
        String identifier = binding.etIdentifier.getText() != null ?
                binding.etIdentifier.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ?
                binding.etPassword.getText().toString().trim() : "";

        if (identifier.isEmpty()) {
            binding.tilIdentifier.setError("Please enter your assigned PRN or Username");
            return;
        } else {
            binding.tilIdentifier.setError(null);
        }

        if (password.isEmpty()) {
            binding.tilPassword.setError("Please enter your password");
            return;
        } else {
            binding.tilPassword.setError(null);
        }

        binding.btnSignIn.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        SessionManager.getInstance().authenticate(identifier, password, new SessionManager.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                binding.btnSignIn.setEnabled(true);
                binding.progressBar.setVisibility(View.GONE);

                Toast.makeText(LoginActivity.this, "Welcome, " + user.getName(), Toast.LENGTH_SHORT).show();

                Intent intent;
                if (user.isSuperAdmin()) {
                    intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                } else {
                    intent = new Intent(LoginActivity.this, MemberDashboardActivity.class);
                }
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                binding.btnSignIn.setEnabled(true);
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showForgotPasswordDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Account Recovery")
                .setMessage("GDG CoreHQ uses administrator-provisioned accounts. Please contact the Chapter Organizer (Aditya Desai) or Community Manager to reset your password.")
                .setPositiveButton("Understood", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
