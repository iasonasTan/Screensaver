package com.app.screensaver;

import android.content.Context;
import android.graphics.Color;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

public class MyButton extends AppCompatButton {
    private final Vibrator mVibrator;
    private final VibrationEffect mActionDownVibeEffect =VibrationEffect.createOneShot(40, VibrationEffect.EFFECT_TICK);

    public MyButton(@NonNull Context context) {
        this(context, null);
    }

    public MyButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mVibrator=context.getSystemService(Vibrator.class);
        setBackgroundResource(R.drawable.button_background);
        setTextColor(Color.WHITE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        animateThis();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mVibrator.vibrate(mActionDownVibeEffect);
        }
        return super.onTouchEvent(event);
    }

    private void animateThis() {
        final long DURATION=140L;
        final float SCALE=0.96f;
        Runnable removeAnim=()->
                animate().scaleX(1f).scaleY(1f).setDuration(DURATION).start();
        animate().scaleX(SCALE).scaleY(SCALE).setDuration(140L)
                .withEndAction(removeAnim).start();
    }
}
