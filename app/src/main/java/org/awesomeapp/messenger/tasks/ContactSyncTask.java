package org.awesomeapp.messenger.tasks;

import android.os.AsyncTask;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.model.SyncContact;

import java.net.URL;
import java.util.List;

/**
 * Created by Shyamal.Upadhyaya on 07/02/17.
 */

public class ContactSyncTask extends AsyncTask<URL, Void, List<SyncContact>> {

    private ContactSyncProcessor contactSyncProcessor;
    private static final String DEBUG_TAG = "SyncContacts";

    public ContactSyncTask(ImApp app, String uriForContact) {
        contactSyncProcessor = new ContactSyncProcessor(app, uriForContact);
    }

    @Override
    protected List<SyncContact> doInBackground(URL... params) {
        boolean succeeded = false;
        List<SyncContact> contacts = contactSyncProcessor.parseContacts();
        return contacts;
    }

    @Override
    protected void onPostExecute(List<SyncContact> contacts) {
        //For now, only download contacts if user logged in...
        System.out.println("on Contact sync post execute account:" + contactSyncProcessor.getmApp().getDefaultAccountId());
        System.out.println("on Contact sync post execute provider:" + contactSyncProcessor.getmApp().getDefaultProviderId());
        if (contactSyncProcessor.getmApp().getDefaultAccountId() != -1 && contacts != null) {
            contactSyncProcessor.processSyncContacts(contacts);
        }

        ImApp.readyForSyncContactWhenNetworkIsAvailable = false;
    }
}

