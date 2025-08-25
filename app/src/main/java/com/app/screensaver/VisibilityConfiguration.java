package com.app.screensaver;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public final class VisibilityConfiguration implements Parcelable {
    public final boolean clock, battery, timer;

    public VisibilityConfiguration(boolean clock, boolean battery, boolean timer) {
        this.clock = clock;
        this.battery = battery;
        this.timer = timer;
    }

    private VisibilityConfiguration(Parcel in) {
        clock = in.readBoolean();
        battery = in.readBoolean();
        timer = in.readBoolean();
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
    }
}
