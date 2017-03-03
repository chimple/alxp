package org.chimple.messenger.tasks;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.chimple.messenger.ImApp;
import org.chimple.messenger.model.Address;
import org.chimple.messenger.model.Contact;
import org.chimple.messenger.model.Presence;
import org.chimple.messenger.model.SyncContact;
import org.chimple.messenger.model.WordInformation;
import org.chimple.messenger.provider.Imps;
import org.chimple.messenger.service.NetworkConnectivityReceiver;
import org.chimple.messenger.service.adapters.CustomHttpClient;
import org.chimple.messenger.ui.onboarding.OnboardingManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import im.zom.messenger.R;

import static org.chimple.messenger.service.adapters.ContactListManagerAdapter.convertPresenceStatus;

/**
 * Created by Shyamal.Upadhyaya on 09/02/17.
 */

public class ContactSyncProcessor {

    private URL contactsPath;

    private HttpClient httpClient;

    private int HTTP_SUCCESS_STATUS_CODE = 200;
    long mDefaultProviderId = -1;
    long mDefaultAccountId = -1;
    long defaultContactListId = -1;
    private Uri mContactUrl = null;
    ImApp mApp;
    ContentResolver mResolver = null;
    private static final String DEBUG_TAG = "SyncContacts";
    List<SyncContact> contacts = null;

    public ContactSyncProcessor(ImApp app, String uriForContact) {
        try {
            mApp = app;
            mResolver = mApp.getContentResolver();
            httpClient = CustomHttpClient.getHttpClient();
            contactsPath = new URL(uriForContact);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public ImApp getmApp() {
        return mApp;
    }

    public void setmApp(ImApp mApp) {
        this.mApp = mApp;
    }

    public List<SyncContact> getContacts() {
        return contacts;
    }

    public void setContacts(List<SyncContact> contacts) {
        this.contacts = contacts;
    }

    public List<SyncContact> parseContacts() {
        List<SyncContact> contactInfos = null;
        try {
            if(mApp.getmNetworkState() == NetworkConnectivityReceiver.State.CONNECTED)
            {
                //Create an HTTP client
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(String.valueOf(contactsPath));

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

    private boolean checkIfContactExists(SyncContact contact) {
        System.out.println("IN fetching contact infomation for offline contacts" + mContactUrl);
        String username = contact.getAddress();
        String selection = Imps.Contacts.USERNAME + "=?";
        String[] selectionArgs = {username};
        String[] projection = {Imps.Contacts.USERNAME};
        Cursor rCursor = mResolver.query(mContactUrl, projection, selection, selectionArgs, null);
        try {
            if (rCursor != null && rCursor.getCount() > 0) {
                return true;
            } else {
                return false;
            }

        } finally {
            rCursor.close();
        }
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
        Cursor cursor = null;
        try {
            cursor = mResolver.query(Imps.ContactList.CONTENT_URI, CONTACT_LIST_ID_PROJECTION,
                    selection, selectionArgs1, null); // no sort order

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

        long rowId = ImApp.insertOrUpdateWord(mResolver, "cat", "Animal", "https://cdn.pixabay.com/photo/2014/03/29/09/17/cat-300572_960_720.jpg", "dd", "ddd");
        System.out.println("inserted row:" + rowId);

        //Query cat
        FetchWordProcessor fetchWordProcessor = new FetchWordProcessor(this.getmApp(), "cat");
        WordInformation wordInformation = fetchWordProcessor.fetchWord();


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

    public void processSyncContacts(List<SyncContact> contacts) {
        //delete all records from database of this kind of contacts
        //insert all contacts into database
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mApp);
        System.out.println("processing contacts");
        for (SyncContact contact : contacts) {

            buildContactUrl();
            if (!checkIfContactExists(contact)) {
                Address addressInfo = new LoopbackAddress(contact.getUserName(), contact.getAddress(), null);
                Contact newContactInfo = new Contact(addressInfo, contact.getNickName());
                newContactInfo.setPresence(new Presence(Presence.AVAILABLE, "available", null, null,
                        Presence.CLIENT_TYPE_DEFAULT));
                System.out.println("addressInfo:" + addressInfo);
                System.out.println("Adding and deleting offline contact");
                insertContactContent(newContactInfo, defaultContactListId, Imps.Contacts.TYPE_NORMAL);
            }

            //check for version updates
            if(checkIfContactUpdated(contact, preferences)) {
                //download zipfile and save to file system
                final SyncContact syncContact = contact;
                System.out.println("Downloding zip file from URL and saving into filesystem");
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        downloadFile(syncContact.getUserName(), syncContact.getVersion());
                    }
                });
                thread.start();
            }

        }
    }


    private String getPathToWordFile(final String folder) {
        String path = mApp.getFilesDir().getAbsolutePath() + File.separator + folder + File.separator + folder + ".csv";
        return path;
    }

    private void batchUpdates(String file) {
        try {
            FileInputStream fin = new FileInputStream(new File(file));
            List<String[]> rows = new CSVFile(fin).read();
            //process it....
            for (String[] sRows: rows) {
                if(sRows != null && sRows.length == 5) {
                    System.out.println("name:" + sRows[0]);
                    System.out.println("meaning:" + sRows[1]);
                    System.out.println("imageUrl:" + sRows[2]);
                    System.out.println("spName:" + sRows[3]);
                    System.out.println("spMeaning:" + sRows[4]);
                    long rowId = ImApp.insertOrUpdateWord(mResolver, sRows[0], sRows[1], sRows[2], sRows[3], sRows[4]);
                    System.out.println("inserted row:" + rowId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private class CSVFile {
        InputStream inputStream;

        public CSVFile(InputStream inputStream){
            this.inputStream = inputStream;
        }

        public List<String[]> read(){
            List<String[]> resultList = new ArrayList();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            try {
                String csvLine;
                while ((csvLine = reader.readLine()) != null) {
                    String[] row = csvLine.split(",");
                    resultList.add(row);
                }
            }
            catch (IOException ex) {
                throw new RuntimeException("Error in reading CSV file: "+ex);
            }
            finally {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    throw new RuntimeException("Error while closing input stream: "+e);
                }
            }
            return resultList;
        }
    }



    private File getDataDir(Context context) {

        String path = context.getFilesDir().getAbsolutePath() + "/";

        File file = new File(path);

        if(!file.exists()) {

            file.mkdirs();
        }

        return file;
    }

    private void downloadFile(final String fileName, final String version) {
        try {
            String url = ImApp.BASE_CONVERSATION_URL + fileName + "_" + version +  ImApp.BASE_CONVERSATION_FILE_EXT;
            System.out.println("downloading file from URL:" + url);

            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection)u.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.connect();

            InputStream is = conn.getInputStream();

            File outputDir = getDataDir(mApp);
            File outputFile = new File(outputDir, fileName + ".zip");

            FileOutputStream fos = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4096];
            int len1 = 0;

            while ((len1 = is.read(buffer)) != -1)
            {
                fos.write(buffer, 0, len1);
            }

            fos.flush();
            fos.close();
            is.close();


            unzip(outputDir, outputFile);

            String pathToWordFile = getPathToWordFile(fileName);
            batchUpdates(pathToWordFile);

            outputFile.delete();


        } catch(FileNotFoundException e) {
            return; // swallow a 404
        } catch (IOException e) {
            return; // swallow a 404
        }
    }

    private void createDirectoryIfNotExists(File directory, String dir) {
        String path = directory.getAbsolutePath() + File.separator + dir;
        File f = new File(path);

        if(!f.isDirectory()) {
            f.mkdirs();
        }
    }

    public void unzip(File directory, File outputFile) {
        try  {
            FileInputStream fin = new FileInputStream(outputFile);
            ZipInputStream zin = new ZipInputStream(fin);

            byte b[] = new byte[1024];

            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                Log.v("Decompress", "Unzipping " + ze.getName());

                if(ze.isDirectory()) {
                    createDirectoryIfNotExists(directory, ze.getName());
                } else {
                    FileOutputStream fout = new FileOutputStream(directory + File.separator + ze.getName());

                    BufferedInputStream in = new BufferedInputStream(zin);
                    BufferedOutputStream out = new BufferedOutputStream(fout);

                    int n;
                    while ((n = in.read(b,0,1024)) >= 0) {
                        out.write(b,0,n);
                    }

                    zin.closeEntry();
                    out.close();
                }
            }
            zin.close();
        } catch(Exception e) {
            Log.e("Decompress", "unzip", e);
        }

    }


    private boolean checkIfContactUpdated(SyncContact contact, SharedPreferences preferences) {
        String version = contact.getVersion();
        String lastStoredVersion = preferences.getString(contact.getUserName(), null);
        if(mApp.getmNetworkState() == NetworkConnectivityReceiver.State.CONNECTED)
        {
            if(lastStoredVersion == null || !lastStoredVersion.equals(version)) {
                //didnt find any version or updated
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(contact.getUserName(), contact.getVersion());
                editor.commit();
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private void updateContactsIfUpdated() {


    }

    private List<SyncContact> failedLoadingPosts() {
        //notify or log error
        List<SyncContact> cList = OnboardingManager.getOffLineContacts(mApp.getApplicationContext());
        return cList;
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
