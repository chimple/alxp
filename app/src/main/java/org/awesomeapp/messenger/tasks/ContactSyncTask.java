package org.awesomeapp.messenger.tasks;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
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
import org.awesomeapp.messenger.service.NetworkConnectivityReceiver;
import org.awesomeapp.messenger.service.adapters.CustomHttpClient;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;

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
 * Created by Shyamal.Upadhyaya on 07/02/17.
 */

public class ContactSyncTask extends AsyncTask<URL, Void, List<SyncContact>> {

    private static final String DEBUG_TAG = "SyncContacts";

    private static final String CONTACT_INFO_SERVER_URL = "https://api.myjson.com/bins/bl9p5";

    private URL contactsPath;

    private HttpClient httpClient;

    private int HTTP_SUCCESS_STATUS_CODE = 200;
    long mDefaultProviderId = -1;
    long mDefaultAccountId = -1;
    long defaultContactListId = -1;
    private Uri mContactUrl = null;
    ImApp mApp;
    ContentResolver mResolver = null;

    public ContactSyncTask(ImApp app, String uriForContact)
    {
        try {
            mApp = app;
            mResolver = mApp.getContentResolver();
            httpClient = CustomHttpClient.getHttpClient();
            contactsPath = new URL(uriForContact);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d(DEBUG_TAG, "bad Url", e);
        }

    }

    @Override
    protected List<SyncContact> doInBackground(URL... params) {
        boolean succeeded = false;
        URL downloadPath = contactsPath;
        List<SyncContact> contacts = null;
        if (downloadPath != null) {
            contacts = parseContacts(downloadPath);
        }
        return contacts;
    }

    private List<SyncContact> parseContacts(URL downloadPath) {
        List<SyncContact> contactInfos = null;
        try {
            if(mApp.getmNetworkState() == NetworkConnectivityReceiver.State.CONNECTED)
            {
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
                        contactInfos = Arrays.asList(gson.fromJson(reader, SyncContact[].class));
                        content.close();
                    } catch (Exception ex) {
                        Log.d(DEBUG_TAG, "parse JSON is: " + ex);
                        contactInfos = failedLoadingPosts();
                    }
                } else {
                    Log.d(DEBUG_TAG, "Server responded with status code: " + statusLine.getStatusCode());
                    contactInfos = failedLoadingPosts();
                }
            } else {
                contactInfos = failedLoadingPosts();
            }

        } catch (Exception ex) {
            //Log.e(TAG, "Failed to send HTTP POST request due to: " + ex);
            contactInfos = failedLoadingPosts();
        }
        return contactInfos;
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
        System.out.println("IN delete contact infomation for offline contacts" + mContactUrl);
        String username = contact.getAddress().getAddress();
        String selection = Imps.Contacts.USERNAME + "=?";
        String[] selectionArgs = {username};
        mResolver.delete(mContactUrl, selection, selectionArgs);
    }

    private Uri buildContactUrl() {
        Uri.Builder builder = Imps.Contacts.CONTENT_URI_CONTACTS_BY.buildUpon();
        //find provider by name
        long providerId = 0;
        String where = Imps.Provider.NAME + "=?";
        String[] selectionArgs = new String[] { "Loopback" };

        Cursor c = mResolver.query(Imps.Provider.CONTENT_URI, null /* projection */, where,
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

        mDefaultAccountId = mApp.getDefaultAccountId();
        ContentUris.appendId(builder, mDefaultAccountId);


        String selection = Imps.ContactList.NAME + "=? AND " + Imps.ContactList.PROVIDER
                + "=? AND " + Imps.ContactList.ACCOUNT + "=?";
        String[] selectionArgs1 = { mApp.getString(R.string.buddies), Long.toString(mApp.getDefaultProviderId()),
                Long.toString(mApp.getDefaultAccountId()) };

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

    private void insertContactContent(Contact contact, long listId, int type) {
        System.out.println("IN INSERT contact infomation for offline contacts" + mContactUrl);
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
            System.out.println("addressInfo:" + addressInfo);
            System.out.println("Adding and deleting offline contact");
            buildContactUrl();
            deleteContactFromDataBase(newContactInfo);
            insertContactContent(newContactInfo, defaultContactListId, Imps.Contacts.TYPE_NORMAL);
        }
    }

    private List<SyncContact> failedLoadingPosts() {
        //notify or log error
        List<SyncContact> cList = OnboardingManager.getOffLineContacts(mApp.getApplicationContext());
        return cList;
    }

    @Override
    protected void onPostExecute(List<SyncContact> contacts) {
        //For now, only download contacts if user logged in...
        System.out.println("on Contact sync post execute account:" + mApp.getDefaultAccountId());
        System.out.println("on Contact sync post execute provider:" + mApp.getDefaultProviderId());
        if(mApp.getDefaultAccountId() != -1 && contacts != null) {
            processSyncContacts(contacts);
        }
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
}

