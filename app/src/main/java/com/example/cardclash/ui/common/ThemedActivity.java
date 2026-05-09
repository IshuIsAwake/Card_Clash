package com.example.cardclash.ui.common;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cardclash.core.theme.ThemeApplier;

/**
 * Base activity that applies the active theme before {@code setContentView}.
 * Subclasses must call {@code super.onCreate(state)} first.
 */
public abstract class ThemedActivity extends AppCompatActivity {
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeApplier.applyToActivity(this);
        super.onCreate(savedInstanceState);
    }
}
