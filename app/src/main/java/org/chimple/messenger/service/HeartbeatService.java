package org.chimple.messenger.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import org.chimple.messenger.ImApp;
import org.chimple.messenger.Preferences;

/**
 * This service exists because a foreground service receiving a wakeup alarm from the OS will cause
 * the service process to lose its foreground status and be killed.  This service runs in the UI process instead.
 *
 * @author devrandom
 *
 */
public class HeartbeatService extends Service {
    public static final String HEARTBEAT_ACTION = "info.guardianproject.otr.app.im.SERVICE.HEARTBEAT";
    public static final String NETWORK_STATE_ACTION = "info.guardianproject.otr.app.im.SERVICE.NETWORK_STATE";
    public static final String NETWORK_STATE_EXTRA = "state";
    public static final String NETWORK_INFO_EXTRA = "info";

    public static final String SYNC_CONTACT_SERVICE_ACTION = "org.chimple.messenger.SyncOfflineContactService.ACTION";

    private static final String TAG = "GB.HeartbeatService";
    private PendingIntent mPendingIntent;
    private PendingIntent mSyncContactPendingIntent;
    private Intent mRelayIntent;
    private ServiceHandler mServiceHandler;
    private NetworkConnectivityReceiver mNetworkConnectivityListener;
    private static final int EVENT_NETWORK_STATE_CHANGED = 200;


    // Our heartbeat interval in seconds.
    // The user controlled preference heartbeat interval is in these units (i.e. minutes).
    private static final long HEARTBEAT_INTERVAL = 1000 * 60;
    private long mHeartbeatInterval = HEARTBEAT_INTERVAL;


    @Override
    public void onCreate() {
        super.onCreate();
        this.mPendingIntent = PendingIntent.getService(this, 0, new Intent(HEARTBEAT_ACTION, null,
                this, HeartbeatService.class), 0);
        this.mSyncContactPendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0, new Intent(this, SyncContactUpdateReceiver.class), 0);
        this.mRelayIntent = new Intent(HEARTBEAT_ACTION, null, this, RemoteImService.class);
        mHeartbeatInterval = Preferences.getHeartbeatInterval() * HEARTBEAT_INTERVAL;

        startHeartbeat(mHeartbeatInterval);

        mServiceHandler = new ServiceHandler();

        mNetworkConnectivityListener = new NetworkConnectivityReceiver();
        NetworkConnectivityReceiver.registerHandler(mServiceHandler, EVENT_NETWORK_STATE_CHANGED);
        mNetworkConnectivityListener.startListening(this);
    }

    void startHeartbeat(long interval) {
        AlarmManager alarmManager = (AlarmManager)this.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(mPendingIntent);
        alarmManager.cancel(this.mSyncContactPendingIntent);
        if (interval > 0)
        {
            //alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval, interval, mPendingIntent);
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval, interval, mPendingIntent);
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval, interval, mSyncContactPendingIntent);
        }
    }

    @Override
    public void onDestroy() {
        startHeartbeat(0);
        NetworkConnectivityReceiver.unregisterHandler(mServiceHandler);
        mNetworkConnectivityListener.stopListening();
        mNetworkConnectivityListener = null;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && HEARTBEAT_ACTION.equals(intent.getAction())) {
            startHeartbeat(mHeartbeatInterval);
            startService(mRelayIntent);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public static void startBeating(Context context) {
        context.startService(new Intent(context, HeartbeatService.class));
    }

    public static void stopBeating(Context context) {
        context.stopService(new Intent(context, HeartbeatService.class));
    }

    void networkStateChanged() {
        // Callback may be async
        if (mNetworkConnectivityListener == null)
            return;

        Intent intent = new Intent(NETWORK_STATE_ACTION, null, this, RemoteImService.class);
        intent.putExtra(NETWORK_INFO_EXTRA, mNetworkConnectivityListener.getNetworkInfo());
        intent.putExtra(NETWORK_STATE_EXTRA, mNetworkConnectivityListener.getState().ordinal());
        startService(intent);


        //if net is connected
        //start Sync Contact Service TBD - start later after user is logged in (first time) or whenever networks is available
        if(ImApp.readyForSyncContactWhenNetworkIsAvailable) {
            Intent syncOfflineContactIntent = new Intent(SYNC_CONTACT_SERVICE_ACTION, null, this, SyncOfflineContactService.class);
            startService(syncOfflineContactIntent);
        }

        //if account is not sync then sync to XMPP Connection
        ImApp app = (ImApp)getApplicationContext();
        Log.d("getDefaultAccountId", ""+app.getDefaultAccountId());
        Log.d("getDefaultProviderId", ""+app.getDefaultProviderId());
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVENT_NETWORK_STATE_CHANGED:
               // Log.d(TAG, "network");
                networkStateChanged();
                break;

            default:
            }
        }
    }
}
