/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.awesomeapp.messenger.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

//import com.bumptech.glide.Glide;

import org.awesomeapp.messenger.provider.Imps;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.awesomeapp.messenger.service.IChatSession;
import info.guardianproject.otr.app.im.R;

import org.awesomeapp.messenger.service.IChatSessionManager;
import org.awesomeapp.messenger.util.SecureMediaStore;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.util.SystemServices;

public class ConversationDetailActivity extends AppCompatActivity {

    private long mChatId = -1;
    private String mAddress = null;
    private String mName = null;

    private ConversationView mConvoView = null;

    MediaRecorder mMediaRecorder = null;
    File mAudioFilePath = null;

    private ImApp mApp;

    private AppBarLayout appBarLayout;
    private CoordinatorLayout mRootLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        setContentView(R.layout.awesome_activity_detail);

        Intent intent = getIntent();
        mApp = (ImApp)getApplication();

        mChatId = intent.getLongExtra("id", -1);
        mAddress = intent.getStringExtra("address");
        mName = intent.getStringExtra("nickname");

        mConvoView = new ConversationView(this);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(mConvoView.getTitle());

        appBarLayout = (AppBarLayout)findViewById(R.id.appbar);
        mRootLayout = (CoordinatorLayout)findViewById(R.id.main_content);

        appBarLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                expandToolbar();
            }
        });

        processIntent(getIntent());

        collapseToolbar();
    }

    private void processIntent(Intent intent)
    {

        mApp = (ImApp)getApplication();

        mChatId = intent.getLongExtra("id", -1);
        mAddress = intent.getStringExtra("address");
        mName = intent.getStringExtra("nickname");

        mConvoView.bindChat(mChatId, mAddress, mName);

        loadBackdrop();

        CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(mConvoView.getTitle());
    }

    public void collapseToolbar(){

        appBarLayout.setExpanded(false);
    }

    public void expandToolbar(){

        appBarLayout.setExpanded(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mConvoView.setSelected(true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mConvoView.setSelected(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        processIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_add_person:
                showAddContact();
                return true;
            case R.id.menu_end_conversation:
                mConvoView.closeChatSession(true);
                finish();
                return true;
            case R.id.menu_verify_or_view:
                mConvoView.showVerifyDialog();
                return true;
            case R.id.menu_group_info:
                mConvoView.showGroupInfo();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadBackdrop() {
        final ImageView imageView = (ImageView) findViewById(R.id.backdrop);

        if (mConvoView.getHeader()!=null)
            imageView.setImageDrawable(mConvoView.getHeader());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (mConvoView.isGroupChat())
        {
            getMenuInflater().inflate(R.menu.menu_conversation_detail_group, menu);
        }
        else {
            getMenuInflater().inflate(R.menu.menu_conversation_detail, menu);
        }

        return true;
    }

    void showAddContact ()
    {
        Intent intent = new Intent(this, ContactsPickerActivity.class);
        startActivityForResult(intent, REQUEST_PICK_CONTACTS);
    }

    void startImagePicker() {
        startActivityForResult(getPickImageChooserIntent(), REQUEST_SEND_IMAGE);

    }

    /**
     * Create a chooser intent to select the source to get image from.<br/>
     * The source can be camera's (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br/>
     * All possible sources are added to the intent chooser.
     */
    public Intent getPickImageChooserIntent() {


        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = getPackageManager();

        // collect all gallery intents
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            allIntents.add(intent);
        }

        // the main intent is the last in the list (fucking android) so pickup the useless one
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);

        // Create a chooser from the main intent
        Intent chooserIntent = Intent.createChooser(mainIntent, getString(R.string.choose_photos));

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }

    Uri mLastPhoto = null;

    void startPhotoTaker() {

        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),  "cs_" + new Date().getTime() + ".jpg");
        mLastPhoto = Uri.fromFile(photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                mLastPhoto);

        // start the image capture Intent
        startActivityForResult(intent, REQUEST_TAKE_PICTURE);
    }


    void startFilePicker() {
        Intent selectFile = new Intent(Intent.ACTION_GET_CONTENT);
        Intent intentChooser = Intent.createChooser(selectFile, "Select File");

        if (intentChooser != null)
            startActivityForResult(Intent.createChooser(selectFile, "Select File"), REQUEST_SEND_FILE);
    }

    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public void handleSendDelete( Uri contentUri, String defaultType, boolean delete, boolean resizeImage, boolean importContent) {
        try {

            // import
            SystemServices.FileInfo info = SystemServices.getFileInfoFromURI(this, contentUri);

            if (info.type == null)
                info.type = defaultType;

            String sessionId = mConvoView.getChatId()+"";

            Uri vfsUri;
            if (resizeImage)
                vfsUri = SecureMediaStore.resizeAndImportImage(this, sessionId, contentUri, info.type);
            else if (importContent) {

                if (contentUri.getScheme() == null || contentUri.getScheme().equals("assets"))
                    vfsUri = SecureMediaStore.importContent(sessionId, info.path,getResources().getAssets().open(info.path));
                else
                    vfsUri = SecureMediaStore.importContent(sessionId, info.path);
            }
            else
            {
                vfsUri = contentUri;
            }

            // send
            boolean sent = handleSendData(vfsUri, info.type);
            if (!sent) {
                // not deleting if not sent
                return;
            }
            // autu delete
            if (delete) {
                boolean deleted = delete(contentUri);
                if (!deleted) {
                    throw new IOException("Error deleting " + contentUri);
                }
            }
        } catch (Exception e) {
            //  Toast.makeText(this, "Error sending file", Toast.LENGTH_LONG).show(); // TODO i18n
            Log.e(ImApp.LOG_TAG, "error sending file", e);
        }
    }

    private boolean delete(Uri uri) {
        if (uri.getScheme().equals("content")) {
            int deleted = getContentResolver().delete(uri,null,null);
            return deleted == 1;
        }
        if (uri.getScheme().equals("file")) {
            java.io.File file = new java.io.File(uri.toString().substring(5));
            return file.delete();
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {

        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_PICK_CONTACTS) {

                ArrayList<String> invitees = new ArrayList<String>();

                String username = resultIntent.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);

                if (username != null)
                    invitees.add(username);
                else
                    invitees = resultIntent.getStringArrayListExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAMES);

                mConvoView.inviteContacts(invitees);

            }
            if (requestCode == REQUEST_SEND_IMAGE) {
                Uri uri = resultIntent.getData() ;

                if( uri == null ) {
                    return ;
                }

                /**
                if (uri.getHost().equals("com.google.android.apps.photos.contentprovider"))
                {

                    try {
                        String uriActual = URLDecoder.decode(uri.getPath(), "UTF-8");
                        uriActual = uriActual.substring(uriActual.indexOf("content://"));
                        uri = Uri.parse(uriActual);
                    }
                    catch (Exception e)
                    {
                        Log.e(ImApp.LOG_TAG,"error parsing photos app URI",e);
                    }

                }*/


                boolean deleteFile = false;
                boolean resizeImage = true;
                boolean importContent = true;
                handleSendDelete(uri, "image/jpeg", deleteFile, resizeImage, importContent);
            }
            else if (requestCode == REQUEST_SEND_FILE || requestCode == REQUEST_SEND_AUDIO) {
                Uri uri = resultIntent.getData() ;

                if( uri == null ) {
                    return;
                }
                boolean deleteFile = false;
                boolean resizeImage = false;
                boolean importContent = false;

                handleSendDelete(uri, null, deleteFile, resizeImage, importContent);
            }
            else if (requestCode == REQUEST_TAKE_PICTURE)
            {
                if (mLastPhoto != null) {
                    boolean deleteFile = true;
                    boolean resizeImage = true;
                    boolean importContent = true;

                    handleSendDelete(mLastPhoto,"image/jpeg", deleteFile, resizeImage, importContent);
                    mLastPhoto = null;
                }

            }



        }
    }

    public boolean handleSendData(Uri uri, String mimeType) {
        try {
            SystemServices.FileInfo info = SystemServices.getFileInfoFromURI(this, uri);

            if (mimeType != null)
                info.type = mimeType;

            //if (info != null && info.path != null && SecureMediaStore.exists(info.path))

            IChatSession session = mConvoView.getChatSession();

            if (session != null) {
                if (info.type == null)
                    if (mimeType != null)
                        info.type = mimeType;
                    else
                        info.type = "application/octet-stream";

                String offerId = UUID.randomUUID().toString();
                boolean canSend = session.offerData(offerId, info.path, info.type );

                if (canSend) {
                    int type = mConvoView.isOtrSessionVerified() ? Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED : Imps.MessageType.OUTGOING_ENCRYPTED;
                    Imps.insertMessageInDb(
                            getContentResolver(), false, session.getId(), true, null, uri.toString(),
                            System.currentTimeMillis(), type,
                            0, offerId, info.type);
                    return true; // sent
                }
                else
                {
                    return false;
                }
            }

        } catch (RemoteException e) {
            Log.e(ImApp.LOG_TAG,"error sending file",e);
        }
        return false; // was not sent
    }

    boolean mIsAudioRecording = false;

    public boolean isAudioRecording ()
    {
        return mIsAudioRecording;
    }

    public void startAudioRecording ()
    {
        mMediaRecorder = new MediaRecorder();

        mAudioFilePath = new File(getFilesDir(),"audiotemp.m4a");

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setAudioChannels(1);
        mMediaRecorder.setAudioEncodingBitRate(22050);
        mMediaRecorder.setAudioSamplingRate(64000);
        mMediaRecorder.setOutputFile(mAudioFilePath.getAbsolutePath());

        try {
            mIsAudioRecording = true;
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        }
        catch (Exception e)
        {
            Log.e(ImApp.LOG_TAG,"couldn't start audio",e);
        }
    }

    public int getAudioAmplitude ()
    {
        return mMediaRecorder.getMaxAmplitude();
    }

    public void stopAudioRecording (boolean send)
    {
        if (mMediaRecorder != null && mAudioFilePath != null && mIsAudioRecording) {

            try {
                mMediaRecorder.stop();

                mMediaRecorder.reset();
                mMediaRecorder.release();

                if (send) {
                    Uri uriAudio = Uri.fromFile(mAudioFilePath);
                    boolean deleteFile = true;
                    boolean resizeImage = false;
                    boolean importContent = true;
                    handleSendDelete(uriAudio, "audio/mp4", deleteFile, resizeImage, importContent);
                } else {
                    mAudioFilePath.delete();
                }
            }
            catch (IllegalStateException ise)
            {
                Log.e(ImApp.LOG_TAG,"error stopping audio recording",ise);
            }

            mIsAudioRecording = false;
        }

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if (mLastPhoto != null)
            savedInstanceState.putString("lastphoto", mLastPhoto.toString());

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.

        String lastPhotoPath =  savedInstanceState.getString("lastphoto");
        if (lastPhotoPath != null)
            mLastPhoto = Uri.parse(lastPhotoPath);
    }

    public static final int REQUEST_PICK_CONTACTS = RESULT_FIRST_USER + 1;
    public static final int REQUEST_SEND_IMAGE = REQUEST_PICK_CONTACTS + 1;
    public static final int REQUEST_SEND_FILE = REQUEST_SEND_IMAGE + 1;
    public static final int REQUEST_SEND_AUDIO = REQUEST_SEND_FILE + 1;
    public static final int REQUEST_TAKE_PICTURE = REQUEST_SEND_AUDIO + 1;
    public static final int REQUEST_SETTINGS = REQUEST_TAKE_PICTURE + 1;
    public static final int REQUEST_TAKE_PICTURE_SECURE = REQUEST_SETTINGS + 1;
    public static final int REQUEST_ADD_CONTACT = REQUEST_TAKE_PICTURE_SECURE + 1;
}
