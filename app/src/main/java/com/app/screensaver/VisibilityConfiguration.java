package com.app.screensaver;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public final class VisibilityConfiguration implements Parcelable {
    public final boolean clock, battery, timer, seconds;

    public VisibilityConfiguration(boolean clock, boolean battery, boolean timer, boolean seconds) {
        this.clock = clock;
        this.battery = battery;
        this.timer = timer;
        this.seconds = seconds;
    }

    private VisibilityConfiguration(Parcel in) {
        clock = in.readBoolean();
        battery = in.readBoolean();
        timer = in.readBoolean();
        seconds = in.readBoolean();
    }

    public static final Creator<VisibilityConfiguration> CREATOR = new Creator<>() {
        @Override
        public VisibilityConfiguration createFromParcel(Parcel in) {
            return new VisibilityConfiguration(in);
        }

        @Override
        public VisibilityConfiguration[] newArray(int size) {
            return new VisibilityConfiguration[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeBoolean(clock);
        dest.writeBoolean(battery);
        dest.writeBoolean(timer);
        dest.writeBoolean(seconds);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                "VisibilityConfiguration{clock=%s, battery=%s, timer=%s, seconds=%s}"
                , clock, battery, timer, seconds);
    }
}
