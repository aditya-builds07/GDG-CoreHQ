package com.gdgcorehq.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.gdgcorehq.app.databinding.ActivityMainBinding;

/**
 * Main entry point for GDG CoreHQ.
 * <p>
 * Phase 1 (Foundation): Displays the app branding and a placeholder state
 * indicating that the login screen will be implemented in Phase 2 when
 * Firebase Authentication is configured by Developer B.
 * <p>
 * This Activity will be replaced by LoginActivity as the launcher in Phase 2.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
