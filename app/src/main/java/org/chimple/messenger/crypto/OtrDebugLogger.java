package org.chimple.messenger.crypto;

import org.chimple.messenger.ImApp;
import org.chimple.messenger.util.Debug;
import org.chimple.messenger.util.LogCleaner;
import android.util.Log;

public class OtrDebugLogger {

    public static void log(String msg) {
        if (Debug.DEBUG_ENABLED)// && Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG))
            Log.d(ImApp.LOG_TAG, LogCleaner.clean(msg));
    }

    public static void log(String msg, Exception e) {
        Log.e(ImApp.LOG_TAG, LogCleaner.clean(msg), e);
    }
}
