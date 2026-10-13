package io.github.katakishi0.oberonextended.mobile;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.SharedPreferences;

public final class OrientationPrefs {
    private OrientationPrefs(){}
    public static String selected(Context context,String key,String fallback){
        return context.getSharedPreferences(MobileUi.PREFS,Context.MODE_PRIVATE)
            .getString(key,fallback);
    }
    public static int requested(String value) {
        if("portrait".equals(value))return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        if("landscape".equals(value))return ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
        return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    }
    public static void apply(Activity activity,String key,String fallback){
        int desired=requested(selected(activity,key,fallback));
        if(activity.getRequestedOrientation()!=desired)activity.setRequestedOrientation(desired);
    }
}
