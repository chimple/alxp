package org.awesomeapp.messenger.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.tasks.ContactSyncTask;

import java.net.URL;

/**
 * Created by Shyamal.Upadhyaya on 28/01/17.
 */

public class SyncOfflineContactService extends Service {

    private static final String DEBUG_TAG = "SyncContacts";
    private ContactSyncTask syncContactTask;
    private URL contactsPath;
    private static final String CONTACT_INFO_SERVER_URL = "https://api.myjson.com/bins/m5mtx";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            ImApp app = (ImApp) getApplication();
            syncContactTask = new ContactSyncTask(app, CONTACT_INFO_SERVER_URL);
            syncContactTask.execute(contactsPath);

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(DEBUG_TAG, "bad Url", e);
        }

        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
