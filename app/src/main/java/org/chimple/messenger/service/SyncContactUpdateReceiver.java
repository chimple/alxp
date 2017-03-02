package org.chimple.messenger.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.chimple.messenger.ImApp;

import static org.chimple.messenger.service.HeartbeatService.SYNC_CONTACT_SERVICE_ACTION;

/**
 * Created by Shyamal.Upadhyaya on 09/02/17.
 */

public class SyncContactUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "SyncContactUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Sync Alarm");
        ImApp app = (ImApp)(context.getApplicationContext());
        ImApp.readyForSyncContactWhenNetworkIsAvailable = true;
        if(app.getmNetworkState() == NetworkConnectivityReceiver.State.CONNECTED)
        {
            Intent syncOfflineContactIntent = new Intent(HeartbeatService.SYNC_CONTACT_SERVICE_ACTION, null, context, SyncOfflineContactService.class);
            context.startService(syncOfflineContactIntent);
        }


    }
}
