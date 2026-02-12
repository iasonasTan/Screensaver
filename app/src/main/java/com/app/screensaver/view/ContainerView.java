package com.app.screensaver.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;

public final class ContainerView extends AppCompatTextView {
    private final int mKey;

    public ContainerView(@NonNull Context context, int key) {
        super(context);
        this.mKey = key;
        Typeface typeface = ResourcesCompat.getFont(context, key);
        setTypeface(typeface);
        setTextColor(Color.WHITE);
    }

    public ContainerView(@NonNull Context context) {
        this(context, 0);
    }

    @NonNull
    @Override
    public String toString() {
        return getResources().getResourceEntryName(mKey);
    }

    public int getKey() {
        return mKey;
    }
}
