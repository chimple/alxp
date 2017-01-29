package org.awesomeapp.messenger.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.model.Address;
import org.awesomeapp.messenger.model.Contact;
import org.awesomeapp.messenger.model.Presence;
import org.awesomeapp.messenger.model.SyncContact;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.adapters.CustomHttpClient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import im.zom.messenger.R;

import static org.awesomeapp.messenger.service.adapters.ContactListManagerAdapter.convertPresenceStatus;

/**
 * Created by Shyamal.Upadhyaya on 28/01/17.
 */

public class SyncOfflineContactService extends Service {

    private static final String DEBUG_TAG = "SyncContacts";
    private static final String CONTACT_INFO_SERVER_URL = "https://api.myjson.com/bins/bl9p5";
    private SyncContactTask syncContactTask;
    private URL contactsPath;
    private HttpClient httpClient;
    private int HTTP_SUCCESS_STATUS_CODE = 200;
    ContentResolver mResolver = null;
    long mDefaultProviderId = -1;
    long mDefaultAccountId = -1;
    long defaultContactListId = -1;
    private Uri mContactUrl = null;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            httpClient = CustomHttpClient.getHttpClient();
            contactsPath = new URL(CONTACT_INFO_SERVER_URL);
            syncContactTask = new SyncContactTask();
            syncContactTask.execute(contactsPath);
            mResolver = getApplication().getContentResolver();

            mContactUrl = buildContactUrl();

        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d(DEBUG_TAG, "bad Url", e);
        }

        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private class LoopbackAddress extends Address {
        private String address;
        private String name;
        private String resource;

        public LoopbackAddress() {
        }

        public LoopbackAddress(String name, String address, String resource) {
            this.name = name;
            this.address = address;
            this.resource = resource;
        }

        @Override
        public String getBareAddress() {
            return address;
        }

        @Override
        public String getAddress() {
            return address;
        }

        @Override
        public String getUser() {
            return name;
        }

        @Override
        public String getResource() {
            return null;
        }

        @Override
        public void readFromParcel(Parcel source) {
            name = source.readString();
            address = source.readString();
            resource = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest) {
            dest.writeString(name);
            dest.writeString(address);
            dest.writeString(resource);
        }
    }


    private Uri buildContactUrl() {
        Uri.Builder builder = Imps.Contacts.CONTENT_URI_CONTACTS_BY.buildUpon();
        //find provider by name
        long providerId = 0;
        String where = Imps.Provider.NAME + "=?";
        String[] selectionArgs = new String[] { "Loopback" };

        Cursor c = getContentResolver().query(Imps.Provider.CONTENT_URI, null /* projection */, where,
                selectionArgs, null);

        mDefaultProviderId = -1;

        try {
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                mDefaultProviderId = c.getLong(0);
                ContentUris.appendId(builder, mDefaultProviderId);
            }

        } finally {
            c.close();
        }

        ImApp app = (ImApp)getApplication();
        mDefaultAccountId = app.getDefaultAccountId();
        ContentUris.appendId(builder, mDefaultAccountId);


        String selection = Imps.ContactList.NAME + "=? AND " + Imps.ContactList.PROVIDER
                + "=? AND " + Imps.ContactList.ACCOUNT + "=?";
        String[] selectionArgs1 = { getApplicationContext().getString(R.string.buddies), Long.toString(app.getDefaultProviderId()),
                Long.toString(app.getDefaultAccountId()) };

        String[] CONTACT_LIST_ID_PROJECTION = { Imps.ContactList._ID };

        Cursor cursor = mResolver.query(Imps.ContactList.CONTENT_URI, CONTACT_LIST_ID_PROJECTION,
                selection, selectionArgs1, null); // no sort order
        try {
            if (cursor.moveToFirst()) {
                defaultContactListId = cursor.getLong(0);
            }
        }finally {
            cursor.close();
        }

        mContactUrl = builder.build();
        return mContactUrl;
    }

    private class SyncContactTask extends AsyncTask<URL, Void, Boolean> {

        private static final String DEBUG_TAG = "SyncOfflineContactService$SyncContactTask";

        @Override
        protected Boolean doInBackground(URL... params) {
            boolean succeeded = false;
            URL downloadPath = params[0];

            if (downloadPath != null) {
                succeeded = parseContacts(downloadPath);
            }
            return succeeded;
        }

        private boolean parseContacts(URL downloadPath) {
            boolean succeeded = false;

            try {
                //Create an HTTP client
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(String.valueOf(downloadPath));

                //Perform the request and check the status code
                HttpResponse response = httpClient.execute(request);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HTTP_SUCCESS_STATUS_CODE) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();

                    try {
                        //Read the server response and attempt to parse it as JSON
                        Reader reader = new InputStreamReader(content);
                        GsonBuilder gsonBuilder = new GsonBuilder();
                        Gson gson = gsonBuilder.create();
                        List<SyncContact> contactInfos = Arrays.asList(gson.fromJson(reader, SyncContact[].class));
                        content.close();
                        processSyncContacts(contactInfos);
                        succeeded = true;
                    } catch (Exception ex) {
                        Log.e(DEBUG_TAG, "parse JSON issue: " + ex);
                        failedLoadingPosts();
                    }
                } else {
                    Log.e(DEBUG_TAG, "Server responded with status code: " + statusLine.getStatusCode());
                    failedLoadingPosts();
                }
            } catch (Exception ex) {
                //Log.e(TAG, "Failed to send HTTP POST request due to: " + ex);
                failedLoadingPosts();
            }
            return succeeded;
        }

        private int translateClientType(Presence presence) {
            int clientType = presence.getClientType();
            switch (clientType) {
                case Presence.CLIENT_TYPE_MOBILE:
                    return Imps.Presence.CLIENT_TYPE_MOBILE;
                default:
                    return Imps.Presence.CLIENT_TYPE_DEFAULT;
            }
        }

        private ContentValues getPresenceValues(Contact c) {
            Presence p = c.getPresence();
            ContentValues values = new ContentValues(3);
            values.put(Imps.Contacts.PRESENCE_STATUS, convertPresenceStatus(p));
            values.put(Imps.Contacts.PRESENCE_CUSTOM_STATUS, p.getStatusText());
            values.put(Imps.Presence.CLIENT_TYPE, translateClientType(p));
            return values;
        }


        private void deleteContactFromDataBase(Contact contact) {
            String username = contact.getAddress().getAddress();
            String selection = Imps.Contacts.USERNAME + "=?";
            String[] selectionArgs = {username};
            mResolver.delete(mContactUrl, selection, selectionArgs);
        }

        private void insertContactContent(Contact contact, long listId, int type) {
            ContentValues values = getContactContentValues(contact, listId);
            values.put(Imps.Contacts.TYPE, type);
            Uri uri = mResolver.insert(mContactUrl, values);
            ContentValues presenceValues = getPresenceValues(contact);
            mResolver.insert(Imps.Presence.CONTENT_URI, presenceValues);
        }

        private ContentValues getContactContentValues(Contact contact, long listId) {
            final String username = contact.getAddress().getAddress();
            final String nickname = contact.getName();
            int type = Imps.Contacts.TYPE_NORMAL;
            ContentValues values = new ContentValues(4);
            values.put(Imps.Contacts.USERNAME, username);
            values.put(Imps.Contacts.NICKNAME, nickname);
            values.put(Imps.Contacts.CONTACTLIST, listId);
            values.put(Imps.Contacts.TYPE, type);
            return values;
        }

        private void processSyncContacts(List<SyncContact> contacts) {
            //delete all records from database of this kind of contacts
            //insert all contacts into database

            System.out.println("processing contacts");
            for (SyncContact contact : contacts) {
                Address addressInfo = new LoopbackAddress(contact.userName, contact.address, null);
                Contact newContactInfo = new Contact(addressInfo, contact.nickName);
                newContactInfo.setPresence(new Presence(Presence.AVAILABLE, "available", null, null,
                        Presence.CLIENT_TYPE_DEFAULT));
                deleteContactFromDataBase(newContactInfo);
                insertContactContent(newContactInfo, defaultContactListId, Imps.Contacts.TYPE_NORMAL);
            }
        }

        private void failedLoadingPosts() {
            //notify or log error
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Context context = SyncOfflineContactService.this
                    .getApplicationContext();
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(NOTIFICATION_SERVICE);

        }
    }
}
