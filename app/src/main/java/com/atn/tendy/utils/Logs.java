package com.atn.tendy.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Created by Admin on 19/08/2017.
 */

public class Logs {
    private static final String DEFAULT_TAG = "tendy";
    public static final boolean DEBUG_MODE = false;

    public static void log(final String log){
        try{
            if(!DEBUG_MODE || log == null) return;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Log.i(DEFAULT_TAG, log);
                }
            });
        } catch (Exception e){e.printStackTrace();}
    }

    public static void log(final String tag, final String log){
        try{
            if(!DEBUG_MODE || tag == null || log == null) return;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Log.i(tag, log);
                }
            });
        } catch (Exception e){e.printStackTrace();}
    }
}
