package com.gdgcorehq.app.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.DrawableRes;
import com.gdgcorehq.app.R;
import com.gdgcorehq.app.databinding.ViewEmptyStateBinding;

public final class EmptyStateHelper {

    private EmptyStateHelper() {}

    public static View createEmptyState(Context context, ViewGroup parent,
                                        @DrawableRes int iconRes,
                                        String title,
                                        String subtitle) {
        ViewEmptyStateBinding binding = ViewEmptyStateBinding.inflate(LayoutInflater.from(context), parent, false);
        binding.ivEmptyIcon.setImageResource(iconRes);
        binding.tvEmptyTitle.setText(title);
        binding.tvEmptySubtitle.setText(subtitle);
        return binding.getRoot();
    }
}
