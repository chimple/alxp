/*
 * Copyright (C) 2007-2008 Esmertec AG. Copyright (C) 2007-2008 The Android Open
 * Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.chimple.messenger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import net.sqlcipher.database.SQLiteDatabase;

import org.chimple.messenger.crypto.OtrAndroidKeyManagerImpl;
import org.chimple.messenger.model.ImConnection;
import org.chimple.messenger.model.ImErrorInfo;
import org.chimple.messenger.provider.Imps;
import org.chimple.messenger.provider.ImpsProvider;
import org.chimple.messenger.push.PushManager;
import org.chimple.messenger.push.model.PersistedAccount;
import org.chimple.messenger.service.Broadcaster;
import org.chimple.messenger.service.IChatSession;
import org.chimple.messenger.service.IChatSessionManager;
import org.chimple.messenger.service.IConnectionCreationListener;
import org.chimple.messenger.service.IImConnection;
import org.chimple.messenger.service.IRemoteImService;
import org.chimple.messenger.service.ImServiceConstants;
import org.chimple.messenger.service.NetworkConnectivityReceiver;
import org.chimple.messenger.service.RemoteImService;
import org.chimple.messenger.tasks.RegisterExistingAccountTask;
import org.chimple.messenger.tts.CustomTextToSpeech;
import org.chimple.messenger.tts.TextToSpeechCommunicateListener;
import org.chimple.messenger.tts.TextToSpeechEventListener;
import org.chimple.messenger.tts.TextToSpeechRecognizer;
import org.chimple.messenger.ui.ConversationDetailActivity;
import org.chimple.messenger.ui.CustomKeyboard;
import org.chimple.messenger.ui.legacy.ImPluginHelper;
import org.chimple.messenger.ui.legacy.ProviderDef;
import org.chimple.messenger.ui.legacy.adapter.ConnectionListenerAdapter;
import org.chimple.messenger.util.Debug;
import org.chimple.messenger.util.Languages;
import org.chatsecure.pushsecure.PushSecureClient;
import org.chatsecure.pushsecure.response.Account;
import org.ironrabbit.type.CustomTypefaceManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import im.zom.messenger.BuildConfig;
import im.zom.messenger.R;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.cacheword.PRNGFixes;
import info.guardianproject.iocipher.VirtualFileSystem;
import timber.log.Timber;

public class ImApp extends Application implements ICacheWordSubscriber, TextToSpeechEventListener {

    public static final String LOG_TAG = "Zom";
    public static boolean isXMPPAccountRegistered = false;
    public static boolean isXMPPAccountRegisteredInProgress = false;
    public static final String EXTRA_INTENT_SEND_TO_USER = "Send2_U";
    public static final String EXTRA_INTENT_PASSWORD = "password";

    public static final String EXTRA_INTENT_PROXY_TYPE = "proxy.type";
    public static final String EXTRA_INTENT_PROXY_HOST = "proxy.host";
    public static final String EXTRA_INTENT_PROXY_PORT = "proxy.port";

    public static final String IMPS_CATEGORY = "org.chimple.messenger.service.IMPS_CATEGORY";
    public static final String ACTION_QUIT = "org.chimple.messenger.service.QUIT";

    public static final int SMALL_AVATAR_WIDTH = 48;
    public static final int SMALL_AVATAR_HEIGHT = 48;

    public static final int DEFAULT_AVATAR_WIDTH = 196;
    public static final int DEFAULT_AVATAR_HEIGHT = 196;

    public static final String HOCKEY_APP_ID = "3cd4c5ff8b666e25466d3b8b66f31766";

    public static final String DEFAULT_TIMEOUT_CACHEWORD = "-1"; //one day

    public static final String CACHEWORD_PASSWORD_KEY = "pkey";
    public static final String CLEAR_PASSWORD_KEY = "clear_key";

    public static final String NO_CREATE_KEY = "nocreate";

    public static final String PREFERENCE_KEY_TEMP_PASS = "temppass";
    
    //ACCOUNT SETTINGS Imps defaults
    public static final String DEFAULT_XMPP_RESOURCE = "ChatSecureZom";
    public static final int DEFAULT_XMPP_PRIORITY = 20;
    public static final String DEFAULT_XMPP_OTR_MODE = "auto";

    public final static String URL_UPDATER = "https://raw.githubusercontent.com/zom/Zom-Android/master/appupdater.xml";

    public final static String ZOM_SERVICES_ADDRESS = "zombot@home.zom.im";

    public final static String BASE_CONVERSATION_URL = "http://wikitaki.org/app/";

    public final static String BASE_CONVERSATION_FILE_EXT = ".zip";

    public static boolean readyForSyncContactWhenNetworkIsAvailable = false;

    private Locale locale = null;

    public static ImApp sImApp;

    private CustomTextToSpeech tts;

    IRemoteImService mImService;

   // HashMap<Long, IImConnection> mConnections;
    MyConnListener mConnectionListener;
    HashMap<Long, ProviderDef> mProviders;

    Broadcaster mBroadcaster;

    /**
     * A queue of messages that are waiting to be sent when service is
     * connected.
     */
    ArrayList<Message> mQueue = new ArrayList<Message>();

    /** A flag indicates that we have called tomServiceStarted start the service. */
//    private boolean mServiceStarted;
    private static Context mApplicationContext;

    private Activity mCurrentActivity = null;
    public Activity getCurrentActivity(){
        return mCurrentActivity;
    }
    public void setCurrentActivity(Activity mCurrentActivity){
        this.mCurrentActivity = mCurrentActivity;
    }

    private  NetworkConnectivityReceiver.State mNetworkState;

    PushManager mPushManager;

    boolean mSupportPushReceive = false;

    private CacheWordHandler mCacheWord;

    public static Executor sThreadPoolExecutor = null;

    private boolean mThemeDark = false;

    public static final int EVENT_SERVICE_CONNECTED = 100;
    public static final int EVENT_CONNECTION_CREATED = 150;
    public static final int EVENT_CONNECTION_LOGGING_IN = 200;
    public static final int EVENT_CONNECTION_LOGGED_IN = 201;
    public static final int EVENT_CONNECTION_LOGGING_OUT = 202;
    public static final int EVENT_CONNECTION_DISCONNECTED = 203;
    public static final int EVENT_CONNECTION_SUSPENDED = 204;
    public static final int EVENT_USER_PRESENCE_UPDATED = 300;
    public static final int EVENT_UPDATE_USER_PRESENCE_ERROR = 301;

    private static final String[] PROVIDER_PROJECTION = { Imps.Provider._ID, Imps.Provider.NAME,
                                                         Imps.Provider.FULLNAME,
                                                         Imps.Provider.SIGNUP_URL, };

    private static final String[] ACCOUNT_PROJECTION = { Imps.Account._ID, Imps.Account.PROVIDER,
                                                        Imps.Account.NAME, Imps.Account.USERNAME,
                                                        Imps.Account.PASSWORD, };

    static final void log(String log) {
        Log.d(LOG_TAG, log);
    }
/**
    protected void attachBaseContext(Context base) {
                super.attachBaseContext(base);
                MultiDex.install(this);
            }*/

    @Override
    public ContentResolver getContentResolver() {
        if (mApplicationContext == null || mApplicationContext == this) {
            return super.getContentResolver();
        }

        return mApplicationContext.getContentResolver();
    }

    public static Context getAppContext() {
        return ImApp.mApplicationContext;
    }

    public NetworkConnectivityReceiver.State getmNetworkState() {
        return mNetworkState;
    }

    public void setmNetworkState(NetworkConnectivityReceiver.State mNetworkState) {
        this.mNetworkState = mNetworkState;
        if(this.mNetworkState == NetworkConnectivityReceiver.State.CONNECTED) {
            activateSuspendedRemoteXMPPAccount();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Preferences.setup(this);
        Languages.setup(MainActivity.class, R.string.use_system_default);
        Languages.setLanguage(this, Preferences.getLanguage(), false);

        sImApp = this;

        Debug.onAppStart();

        PRNGFixes.apply(); //Google's fix for SecureRandom bug: http://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html

        // load these libs up front to shorten the delay after typing the passphrase
        SQLiteDatabase.loadLibs(getApplicationContext());
        VirtualFileSystem.get().isMounted();

       // mConnections = new HashMap<Long, IImConnection>();
        ImApp.mApplicationContext = this;

        //initTrustManager();

        mBroadcaster = new Broadcaster();

        setAppTheme(null,null);

        // ChatSecure-Push needs to do initial setup as soon as Cacheword is ready
        mCacheWord = new CacheWordHandler(this, this);
        mCacheWord.connectToService();

        if (sThreadPoolExecutor == null) {
            int corePoolSize = 20;
            int maximumPoolSize = 40;
            int keepAliveTime = 60;
            BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(maximumPoolSize);
            sThreadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue);
        }

        List<Locale> locals = new ArrayList<Locale>();
        locals.add(new Locale("en", "US"));
        TextToSpeechRecognizer textToSpeechRecognizer = new TextToSpeechRecognizer(this, locals, this);
    }

    public void displayKeyBoard(final int keyboardType, final String...params) {
       if(getCurrentActivity() != null && getCurrentActivity() instanceof ConversationDetailActivity) {
           final ConversationDetailActivity conversationDetailActivity = (ConversationDetailActivity)getCurrentActivity();
//           CustomKeyboard board = conversationDetailActivity.getmConvoView().getCustomKeyBoard();
//           board.dyanamicKeyBoard(params);
           getCurrentActivity().runOnUiThread(new Runnable() {
               @Override
               public void run() {
                   conversationDetailActivity.getmConvoView().setKeyboardType(keyboardType, params);
               }
           });

       }
    }

    public boolean isThemeDark ()
    {
        return mThemeDark;
    }
    
    public void setAppTheme (Activity activity)
    {
        setAppTheme(activity, null);
    }

    public void setAppTheme (Activity activity, Toolbar toolbar)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        mThemeDark = settings.getBoolean("themeDark", false);

        if (mThemeDark)
        {
            setTheme(R.style.AppThemeDark);


            if (activity != null)
            {
                activity.setTheme(R.style.AppThemeDark);
                
            }      
      
        }
        else
        {
            setTheme(R.style.AppTheme);


            if (activity != null)
            {
                activity.setTheme(R.style.AppTheme);
               
            }
            
            
        }

        Configuration config = getResources().getConfiguration();
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        if (mImService != null)
        {
            boolean debugOn = settings.getBoolean("prefDebug", false);
            try {
                mImService.enableDebugLogging(debugOn);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void resetLanguage(Activity activity, String language) {
        if (!TextUtils.equals(language, Preferences.getLanguage())) {
            /* Set the preference after setting the locale in case something goes
             * wrong. If setting the locale causes an Exception, it should not be set in
             * the preferences, otherwise this will be stuck in a crash loop. */
            Languages.setLanguage(activity, language, true);
            Preferences.setLanguage(language);
            Languages.forceChangeLanguage(activity);

            if (language.equalsIgnoreCase("bo"))
            {
                if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    CustomTypefaceManager.loadFromAssets(activity);
                }
            }
            else
            {
                CustomTypefaceManager.setTypeface(null);
            }
        }
    }

    public synchronized void startImServiceIfNeed() {
        startImServiceIfNeed(false);
    }

    public synchronized void startImServiceIfNeed(boolean isBoot) {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG))
            log("start ImService");

        if (mImService == null) {

            Intent serviceIntent = new Intent(this, RemoteImService.class);
//        serviceIntent.putExtra(ImServiceConstants.EXTRA_CHECK_AUTO_LOGIN, isBoot);

            ImApp.mApplicationContext.startService(serviceIntent);

            mConnectionListener = new MyConnListener(new Handler());

            ImApp.mApplicationContext
                    .bindService(serviceIntent, mImServiceConn, Context.BIND_AUTO_CREATE);

        }

    }

    public boolean hasActiveConnections ()
    {
        try {
            return !mImService.getActiveConnections().isEmpty();
        }
        catch (RemoteException re)
        {
            return false;
        }

    }

    public void stopImServiceIfInactive() {

        //todo we don't wnat to do this right now
        /**
        if (!hasActiveConnections()) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                log("stop ImService because there's no active connections");

            forceStopImService();

        }*/
    }


    public void forceStopImService() {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG))
            log("stop ImService");


        if (mImServiceConn != null) {
            try {
                if (mImService != null)
                 mImService.shutdownAndLock();
            }
            catch (RemoteException re)
            {

            }
            ImApp.mApplicationContext.unbindService(mImServiceConn);

            mImService = null;
        }


        Intent serviceIntent = new Intent(this, RemoteImService.class);
        serviceIntent.putExtra(ImServiceConstants.EXTRA_CHECK_AUTO_LOGIN, true);
        ImApp.mApplicationContext.stopService(serviceIntent);


    }

    private ServiceConnection mImServiceConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                log("service connected");

            mImService = IRemoteImService.Stub.asInterface(service);
         //   fetchActiveConnections();

            synchronized (mQueue) {
                for (Message msg : mQueue) {
                    msg.sendToTarget();
                }
                mQueue.clear();
            }
            Message msg = Message.obtain(null, EVENT_SERVICE_CONNECTED);
            mBroadcaster.broadcast(msg);

            /*
            if (mKillServerOnStart)
            {
                forceStopImService();
            }*/
        }

        public void onServiceDisconnected(ComponentName className) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                log("service disconnected");

           // mConnections.clear();
            mImService = null;
        }
    };

    public boolean serviceConnected() {
        return mImService != null;
    }

    public static long insertOrUpdateWord(ContentResolver cr, String name, String meaning, String imageUrl, String spName, String spMeaning) {

        String where = Imps.Word.NAME + " = ?";
        String[] selectionArgs = new String[]{name.toLowerCase()};

        Cursor c = cr.query(Imps.Word.CONTENT_URI, null, where,
                selectionArgs, null);

        try {
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                long id = c.getLong(0);

                ContentValues values = new ContentValues(5);
                values.put(Imps.Word.NAME, name);

                if (!TextUtils.isEmpty(meaning))
                    values.put(Imps.Word.MEANING, meaning);

                if (!TextUtils.isEmpty(imageUrl))
                    values.put(Imps.Word.IMAGE_URL, imageUrl);

                if (!TextUtils.isEmpty(spName))
                    values.put(Imps.Word.SP_NAME, spName);

                if (!TextUtils.isEmpty(spMeaning))
                    values.put(Imps.Word.SP_NAME, spMeaning);

                Uri wordUri = ContentUris.withAppendedId(Imps.Word.CONTENT_URI, id);
                cr.update(wordUri, values, null, null);
                c.close();
                return id;
            } else {
                ContentValues values = new ContentValues(5);
                values.put(Imps.Word.NAME, name);
                values.put(Imps.Word.MEANING, meaning);
                values.put(Imps.Word.IMAGE_URL, imageUrl);
                values.put(Imps.Word.SP_NAME, spName);
                values.put(Imps.Word.SP_MEANING, spMeaning);

                Uri result = cr.insert(Imps.Word.CONTENT_URI, values);
                if(result != null) {
                    return ContentUris.parseId(result);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static long insertOrUpdatePhonic(ContentResolver cr, String letters, String phonetic, String word, String split, String choice1, String choice2, String choice3) {

        String where = Imps.Phonic.WORD + " = ?";
        String[] selectionArgs = new String[]{word.toLowerCase()};

        Cursor c = cr.query(Imps.Phonic.CONTENT_URI, null, where,
                selectionArgs, null);

        try {
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                long id = c.getLong(0);

                ContentValues values = new ContentValues(7);
                values.put(Imps.Phonic.LETTERS, letters);
                values.put(Imps.Phonic.PHONETIC, phonetic);
                values.put(Imps.Phonic.WORD, word);

                if (!TextUtils.isEmpty(split))
                    values.put(Imps.Phonic.SPLIT, split);

                if (!TextUtils.isEmpty(choice1))
                    values.put(Imps.Phonic.CHOICE1, choice1);

                if (!TextUtils.isEmpty(choice2))
                    values.put(Imps.Phonic.CHOICE2, choice2);

                if (!TextUtils.isEmpty(choice3))
                    values.put(Imps.Phonic.CHOICE3, choice3);

                Uri phonicUri = ContentUris.withAppendedId(Imps.Phonic.CONTENT_URI, id);
                cr.update(phonicUri, values, null, null);
                c.close();
                return id;
            } else {
                ContentValues values = new ContentValues(7);
                values.put(Imps.Phonic.LETTERS, letters);
                values.put(Imps.Phonic.PHONETIC, letters);
                values.put(Imps.Phonic.WORD, word);
                values.put(Imps.Phonic.SPLIT, split);
                values.put(Imps.Phonic.CHOICE1, choice1);
                values.put(Imps.Phonic.CHOICE2, choice2);
                values.put(Imps.Phonic.CHOICE3, choice3);

                Uri result = cr.insert(Imps.Phonic.CONTENT_URI, values);
                if(result != null) {
                    return ContentUris.parseId(result);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static long insertOrUpdateAccount(ContentResolver cr, long providerId, long accountId, String nickname, String username,
            String pw) {
        String selection = Imps.Account.PROVIDER + "=? AND (" + Imps.Account._ID + "=?" + " OR " + Imps.Account.USERNAME + "=?)";
        String[] selectionArgs = { Long.toString(providerId), Long.toString(accountId), username };

        Cursor c = cr.query(Imps.Account.CONTENT_URI, ACCOUNT_PROJECTION, selection, selectionArgs,
                null);
        if (c != null && c.moveToFirst()) {
            long id = c.getLong(c.getColumnIndexOrThrow(Imps.Account._ID));

            ContentValues values = new ContentValues(4);
            values.put(Imps.Account.PROVIDER, providerId);

            if (!TextUtils.isEmpty(nickname))
                values.put(Imps.Account.NAME, nickname);

            if (!TextUtils.isEmpty(username))
                values.put(Imps.Account.USERNAME, username);

            if (!TextUtils.isEmpty(pw))
                values.put(Imps.Account.PASSWORD, pw);

            Uri accountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, id);
            cr.update(accountUri, values, null, null);

            c.close();
            return id;
        } else {
            ContentValues values = new ContentValues(4);
            values.put(Imps.Account.PROVIDER, providerId);
            values.put(Imps.Account.NAME, nickname);
            values.put(Imps.Account.USERNAME, username);
            values.put(Imps.Account.PASSWORD, pw);

            if (pw != null && pw.length() > 0) {
                values.put(Imps.Account.KEEP_SIGNED_IN, true);
            }

            Uri result = cr.insert(Imps.Account.CONTENT_URI, values);
            if (c != null)
                c.close();
            return ContentUris.parseId(result);
        }
    }

    private void loadImProviderSettings() {

        mProviders = new HashMap<Long, ProviderDef>();
        ContentResolver cr = getContentResolver();

        String selectionArgs[] = new String[1];
        selectionArgs[0] = ImApp.IMPS_CATEGORY;

        Cursor c = cr.query(Imps.Provider.CONTENT_URI, PROVIDER_PROJECTION, Imps.Provider.CATEGORY
                                                                            + "=?", selectionArgs,
                null);
        if (c == null) {
            return;
        }

        try {
            while (c.moveToNext()) {
                long id = c.getLong(0);
                String providerName = c.getString(1);
                String fullName = c.getString(2);
                String signUpUrl = c.getString(3);

                if (mProviders == null) // mProviders has been reset
                    break;
                mProviders.put(id, new ProviderDef(id, providerName, fullName, signUpUrl));
            }
        } finally {
            c.close();
        }
    }

    public long getProviderId(String name) {
        loadImProviderSettings();
        for (ProviderDef provider : mProviders.values()) {
            if (provider.mName.equals(name)) {
                return provider.mId;
            }
        }
        return -1;
    }

    public ProviderDef getProvider(long id) {
        loadImProviderSettings();
        return mProviders.get(id);
    }

    public IImConnection createConnection(long providerId, long accountId) throws RemoteException {

        if (mImService == null) {
            // Service hasn't been connected or has died.
            return null;
        }

        IImConnection conn = mImService.createConnection(providerId, accountId);

        return conn;
    }

    public void activateSuspendedRemoteXMPPAccount() {
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            isXMPPAccountRegistered = preferences.getBoolean("isXMPPAccountRegistered", false);


            if(mDefaultAccountId != -1 && !isXMPPAccountRegistered && !isXMPPAccountRegisteredInProgress) {
                IImConnection connection = getConnection(mDefaultProviderId, mDefaultAccountId);
                new RegisterExistingAccountTask(this).execute(mDefaultNickname, mDefaultUsername,""+mDefaultProviderId, ""+mDefaultAccountId, mActiveAccountPassword);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public IImConnection getConnection(long providerId,long accountId) {

        try {
            if (mImService != null) {
                IImConnection im = mImService.getConnection(providerId);

                if (im != null) {

                    im.getState();

                } else {
                    im = createConnection(providerId, accountId);

                }

                return im;
            }
            else
                return null;
        }
        catch (RemoteException re)
        {
            return null;
        }
    }


    public Collection<IImConnection> getActiveConnections() {

        try {
            return mImService.getActiveConnections();
        }
        catch (RemoteException re)
        {
            return null;
        }
    }

    public void callWhenServiceConnected(Handler target, Runnable callback) {
        Message msg = Message.obtain(target, callback);
        if (serviceConnected() && msg != null) {
            msg.sendToTarget();
        } else {
            startImServiceIfNeed();
            synchronized (mQueue) {
                mQueue.add(msg);
            }
        }
    }

    public static void deleteAccount (ContentResolver resolver, long accountId, long providerId)
    {
        Uri accountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
        resolver.delete(accountUri, null, null);

        Uri providerUri = ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, providerId);
        resolver.delete(providerUri, null, null);

        Uri.Builder builder = Imps.Contacts.CONTENT_URI_CONTACTS_BY.buildUpon();
        ContentUris.appendId(builder, providerId);
        ContentUris.appendId(builder, accountId);
        resolver.delete(builder.build(), null, null);



    }

    public void registerForBroadcastEvent(int what, Handler target) {
        mBroadcaster.request(what, target, what);
    }

    public void unregisterForBroadcastEvent(int what, Handler target) {
        mBroadcaster.cancelRequest(what, target, what);
    }

    public void registerForConnEvents(Handler handler) {
        mBroadcaster.request(EVENT_CONNECTION_CREATED, handler, EVENT_CONNECTION_CREATED);
        mBroadcaster.request(EVENT_CONNECTION_LOGGING_IN, handler, EVENT_CONNECTION_LOGGING_IN);
        mBroadcaster.request(EVENT_CONNECTION_LOGGED_IN, handler, EVENT_CONNECTION_LOGGED_IN);
        mBroadcaster.request(EVENT_CONNECTION_LOGGING_OUT, handler, EVENT_CONNECTION_LOGGING_OUT);
        mBroadcaster.request(EVENT_CONNECTION_SUSPENDED, handler, EVENT_CONNECTION_SUSPENDED);
        mBroadcaster.request(EVENT_CONNECTION_DISCONNECTED, handler, EVENT_CONNECTION_DISCONNECTED);
        mBroadcaster.request(EVENT_USER_PRESENCE_UPDATED, handler, EVENT_USER_PRESENCE_UPDATED);
        mBroadcaster.request(EVENT_UPDATE_USER_PRESENCE_ERROR, handler,
                EVENT_UPDATE_USER_PRESENCE_ERROR);
    }

    public void unregisterForConnEvents(Handler handler) {
        mBroadcaster.cancelRequest(EVENT_CONNECTION_CREATED, handler, EVENT_CONNECTION_CREATED);
        mBroadcaster.cancelRequest(EVENT_CONNECTION_LOGGING_IN, handler,
                EVENT_CONNECTION_LOGGING_IN);
        mBroadcaster.cancelRequest(EVENT_CONNECTION_LOGGED_IN, handler, EVENT_CONNECTION_LOGGED_IN);
        mBroadcaster.cancelRequest(EVENT_CONNECTION_LOGGING_OUT, handler,
                EVENT_CONNECTION_LOGGING_OUT);
        mBroadcaster.cancelRequest(EVENT_CONNECTION_SUSPENDED, handler, EVENT_CONNECTION_SUSPENDED);
        mBroadcaster.cancelRequest(EVENT_CONNECTION_DISCONNECTED, handler,
                EVENT_CONNECTION_DISCONNECTED);
        mBroadcaster.cancelRequest(EVENT_USER_PRESENCE_UPDATED, handler,
                EVENT_USER_PRESENCE_UPDATED);
        mBroadcaster.cancelRequest(EVENT_UPDATE_USER_PRESENCE_ERROR, handler,
                EVENT_UPDATE_USER_PRESENCE_ERROR);
    }

    void broadcastConnEvent(int what, long providerId, ImErrorInfo error) {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            log("broadcasting connection event " + what + ", provider id " + providerId);
        }
        android.os.Message msg = android.os.Message.obtain(null, what, (int) (providerId >> 32),
                (int) providerId, error);
        mBroadcaster.broadcast(msg);
    }

    public void dismissChatNotification(long providerId, String username) {
        if (mImService != null) {
            try {
                mImService.dismissChatNotification(providerId, username);
            } catch (RemoteException e) {
            }
        }
    }

    /**
    private void fetchActiveConnections() {
        if (mImService != null)
        {
            try {
                // register the listener before fetch so that we won't miss any connection.
                mImService.addConnectionCreatedListener(mConnCreationListener);
                synchronized (mConnections) {
                    for (IBinder binder : (List<IBinder>) mImService.getActiveConnections()) {
                        IImConnection conn = IImConnection.Stub.asInterface(binder);
                        long providerId = conn.getProviderId();
                        if (!mConnections.containsKey(providerId)) {
                            mConnections.put(providerId, conn);
                            conn.registerConnectionListener(mConnectionListener);
                     }
                    }
                }
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "fetching active connections", e);
            }
        }
    }*/

    private final IConnectionCreationListener mConnCreationListener = new IConnectionCreationListener.Stub() {
        public void onConnectionCreated(IImConnection conn) throws RemoteException {
            long providerId = conn.getProviderId();
             conn.registerConnectionListener(mConnectionListener);

            /**
            synchronized (mConnections) {
                if (!mConnections.containsKey(providerId)) {
                    mConnections.put(providerId, conn);
                    conn.registerConnectionListener(mConnectionListener);
                }
            }*/
            broadcastConnEvent(EVENT_CONNECTION_CREATED, providerId, null);
        }
    };

    @SuppressLint("NewApi")
    private void setTTSListener()
    {
        if (Build.VERSION.SDK_INT >= 15)
        {
            int listenerResult =
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener()
                    {
                        @Override
                        public void onDone(String utteranceId)
                        {
                            Log.d(LOG_TAG, "TTS done");
                            if(tts.getCommunicateListener() != null) {
                                tts.getCommunicateListener().wordSpeakingEnded();
                            }
                        }

                        @Override
                        public void onError(String utteranceId)
                        {
                            Log.e(LOG_TAG, "TTS error");
                        }

                        @Override
                        public void onStart(String utteranceId)
                        {
                            Log.d(LOG_TAG, "TTS start");
                            if(tts.getCommunicateListener() != null) {
                                tts.getCommunicateListener().wordSpeakingStarted();
                            }
                        }
                    });
            if (listenerResult != TextToSpeech.SUCCESS)
            {
                Log.e(LOG_TAG, "failed to add utterance progress listener");
            }
        }
        else
        {
            int listenerResult =
                    tts.setOnUtteranceCompletedListener(
                            new TextToSpeech.OnUtteranceCompletedListener()
                            {
                                @Override
                                public void onUtteranceCompleted(String utteranceId)
                                {
                                    Log.d(LOG_TAG, "TTS done");
                                }
                            });
            if (listenerResult != TextToSpeech.SUCCESS)
            {
                Log.e(LOG_TAG, "failed to add utterance completed listener");
            }
        }
    }

    private void onDone(final String utteranceId)
    {
    }

    @Override
    public void onSuccessfulInitiated(CustomTextToSpeech tts) {
        this.tts = tts;
        setTTSListener();
    }

    @Override
    public void onDownloadRequiredForLanguageData(List<Locale> missingLocals) {
        TextToSpeechRecognizer.installLanguageData(this);
    }

    @Override
    public void onDownloadNotCompletedForLanguageData() {

    }

    @Override
    public void onErrorToInitialize() {

    }

    private final class MyConnListener extends ConnectionListenerAdapter {
        public MyConnListener(Handler handler) {
            super(handler);
        }

        @Override
        public void onConnectionStateChange(IImConnection conn, int state, ImErrorInfo error) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                log("onConnectionStateChange(" + state + ", " + error + ")");
            }

            try {

                //fetchActiveConnections();

                int what = -1;
                long providerId = conn.getProviderId();
                switch (state) {
                case ImConnection.LOGGED_IN:
                    what = EVENT_CONNECTION_LOGGED_IN;
                    break;

                case ImConnection.LOGGING_IN:
                    what = EVENT_CONNECTION_LOGGING_IN;
                    break;

                case ImConnection.LOGGING_OUT:
                    // NOTE: if this logic is changed, the logic in ImConnectionAdapter.ConnectionAdapterListener must be changed to match
                    what = EVENT_CONNECTION_LOGGING_OUT;

                    break;

                case ImConnection.DISCONNECTED:
                    // NOTE: if this logic is changed, the logic in ImConnectionAdapter.ConnectionAdapterListener must be changed to match
                    what = EVENT_CONNECTION_DISCONNECTED;
               //     mConnections.remove(providerId);
                    // stop the service if there isn't an active connection anymore.
                    stopImServiceIfInactive();

                    break;

                case ImConnection.SUSPENDED:
                    what = EVENT_CONNECTION_SUSPENDED;
                    break;
                }
                if (what != -1) {
                    broadcastConnEvent(what, providerId, error);
                }
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "onConnectionStateChange", e);
            }
        }

        @Override
        public void onUpdateSelfPresenceError(IImConnection connection, ImErrorInfo error) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                log("onUpdateUserPresenceError(" + error + ")");
            }
            try {
                long providerId = connection.getProviderId();
                broadcastConnEvent(EVENT_UPDATE_USER_PRESENCE_ERROR, providerId, error);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "onUpdateUserPresenceError", e);
            }
        }

        @Override
        public void onSelfPresenceUpdated(IImConnection connection) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                log("onUserPresenceUpdated");

            try {
                long providerId = connection.getProviderId();
                broadcastConnEvent(EVENT_USER_PRESENCE_UPDATED, providerId, null);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "onUserPresenceUpdated", e);
            }
        }
    }

    public IChatSession getChatSession(long providerId, long accountId, String remoteAddress) {

        IImConnection conn = getConnection(providerId,accountId);

        IChatSessionManager chatSessionManager = null;
        if (conn != null) {
            try {
                chatSessionManager = conn.getChatSessionManager();
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "error in getting ChatSessionManager", e);
            }
        }

        if (chatSessionManager != null) {
            try {
                return chatSessionManager.getChatSession(remoteAddress);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "error in getting ChatSession", e);
            }
        }

        return null;
    }

    public void maybeInit(Activity activity) {
        startImServiceIfNeed();
        setAppTheme(activity,null);
        ImPluginHelper.getInstance(this).loadAvailablePlugins();
    }

    public void checkForCrashes(final Activity activity) {
        CrashManager.register(activity, ImApp.HOCKEY_APP_ID, new CrashManagerListener() {
            @Override
            public String getDescription() {
                return Debug.getTrail(activity);
            }
        });
    }

    public boolean setDefaultAccount (long providerId, long accountId)
    {

        final Uri uri = Imps.Provider.CONTENT_URI_WITH_ACCOUNT;
        String[] PROVIDER_PROJECTION = {
                Imps.Provider._ID,
                Imps.Provider.ACTIVE_ACCOUNT_ID,
                Imps.Provider.ACTIVE_ACCOUNT_USERNAME,
                Imps.Provider.ACTIVE_ACCOUNT_NICKNAME,
                Imps.Provider.ACTIVE_ACCOUNT_PW

        };

        final Cursor cursorProviders = getContentResolver().query(uri, PROVIDER_PROJECTION,
                Imps.Provider.ACTIVE_ACCOUNT_ID + "=" + accountId
                        + " AND " + Imps.Provider.CATEGORY + "=?"
                        + " AND " + Imps.Provider.ACTIVE_ACCOUNT_USERNAME + " NOT NULL" /* selection */,
                new String[]{ImApp.IMPS_CATEGORY} /* selection args */,
                Imps.Provider.DEFAULT_SORT_ORDER);

        if (cursorProviders != null && cursorProviders.getCount() > 0) {
            cursorProviders.moveToFirst();
            mDefaultProviderId = cursorProviders.getLong(0);
            mDefaultAccountId = cursorProviders.getLong(1);
            mDefaultUsername = cursorProviders.getString(2);
            mDefaultNickname = cursorProviders.getString(3);
            mActiveAccountPassword = cursorProviders.getString(4);

            Cursor pCursor = getContentResolver().query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(mDefaultProviderId)}, null);

            Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                    pCursor, getContentResolver(), mDefaultProviderId, false /* don't keep updated */, null /* no handler */);

            mDefaultUsername = mDefaultUsername + '@' + settings.getDomain();
            mDefaultOtrFingerprint = OtrAndroidKeyManagerImpl.getInstance(this).getLocalFingerprint(mDefaultUsername);

            settings.close();
            cursorProviders.close();

            return true;
        }


        if (cursorProviders != null)
            cursorProviders.close();

        return false;
    }

    public boolean initAccountInfo ()
    {
        if (mDefaultProviderId == -1 || mDefaultAccountId == -1) {

            final Uri uri = Imps.Provider.CONTENT_URI_WITH_ACCOUNT;
            String[] PROVIDER_PROJECTION = {
                    Imps.Provider._ID,
                    Imps.Provider.ACTIVE_ACCOUNT_ID,
                    Imps.Provider.ACTIVE_ACCOUNT_USERNAME,
                    Imps.Provider.ACTIVE_ACCOUNT_NICKNAME,
                    Imps.Provider.ACTIVE_ACCOUNT_PW,

            };

            final Cursor cursorProviders = getContentResolver().query(uri, PROVIDER_PROJECTION,
                    Imps.Provider.CATEGORY + "=?" + " AND " + Imps.Provider.ACTIVE_ACCOUNT_USERNAME + " NOT NULL" /* selection */,
                    new String[]{ImApp.IMPS_CATEGORY} /* selection args */,
                    Imps.Provider.DEFAULT_SORT_ORDER);

            if (cursorProviders != null && cursorProviders.getCount() > 0) {
                cursorProviders.moveToFirst();
                mDefaultProviderId = cursorProviders.getLong(0);
                mDefaultAccountId = cursorProviders.getLong(1);
                mDefaultUsername = cursorProviders.getString(2);
                mDefaultNickname = cursorProviders.getString(3);
                mActiveAccountPassword = cursorProviders.getString(4);

                Cursor pCursor = getContentResolver().query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(mDefaultProviderId)}, null);

                Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                        pCursor, getContentResolver(), mDefaultProviderId, false /* don't keep updated */, null /* no handler */);

                mDefaultUsername = mDefaultUsername + '@' + settings.getDomain();
                mDefaultOtrFingerprint = OtrAndroidKeyManagerImpl.getInstance(this).getLocalFingerprint(mDefaultUsername);

                settings.close();
                cursorProviders.close();

                return true;
            }


            if (cursorProviders != null)
                cursorProviders.close();
        }

        return false;

    }

    private long mDefaultProviderId = -1;
    private long mDefaultAccountId = -1;
    private String mDefaultUsername = null;
    private String mDefaultOtrFingerprint = null;
    private String mDefaultNickname = null;
    private String mActiveAccountPassword = null;

    public String getDefaultUsername ()
    {
        return mDefaultUsername;
    }

    public String getDefaultNickname ()
    {
        return mDefaultNickname;
    }

    public String getDefaultOtrKey ()
    {
        return mDefaultOtrFingerprint;
    }

    public long getDefaultProviderId ()
    {
        return mDefaultProviderId;
    }

    public long getDefaultAccountId ()
    {
        return mDefaultAccountId;
    }

    public String getActiveAccontPassword ()
    {
        return mActiveAccountPassword;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Languages.setLanguage(this, Preferences.getLanguage(),true);

    }

    public void setupChatSecurePush() {
        // Setup logging for ChatSecure-Push SDK
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        Timber.d("SetupChatSecurePush");

        mPushManager = new PushManager(this);

        if (mSupportPushReceive) {

            PersistedAccount chatSecurePushAccount = mPushManager.getPersistedAccount();
            if (chatSecurePushAccount == null) {
                Log.d(LOG_TAG, "No ChatSecure-Push Account is persisted. Creating new");
            } else {
                Log.d(LOG_TAG, "ChatSecure-Push Account is persisted with username: " + chatSecurePushAccount.username);
            }

            // Use the existing account credentials if available, else a new random username & password
            final String username = isCspAccountValid(chatSecurePushAccount, mPushManager.getProviderUrl()) ?
                    chatSecurePushAccount.username :
                    UUID.randomUUID().toString().substring(0, 30); // ChatSecure-Push usernames are 30 characters max

            final String password = isCspAccountValid(chatSecurePushAccount, mPushManager.getProviderUrl()) ?
                    chatSecurePushAccount.pasword :
                    UUID.randomUUID().toString();

            final Object authLock = new Object();
            final AtomicBoolean authenticated = new AtomicBoolean();

            // Continue trying to authenticate until we have success
            // Our free Heroku plan sometimes gives ya a SocketTimeout
            while (!authenticated.get()) {

                PushSecureClient.RequestCallback<Account> authCallback = new PushSecureClient.RequestCallback<Account>() {
                    @Override
                    public void onSuccess(@NonNull Account response) {
                        Log.d(LOG_TAG, "Registered ChatSecure-Push account!");
                        if (mCacheWord != null) {
                            mCacheWord.disconnectFromService();
                            mCacheWord = null;
                        }
                        authenticated.set(true);
                        synchronized (authLock) {
                            authLock.notify();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Log.e(LOG_TAG, "Failed to register ChatSecure-Push account!", t);
                        synchronized (authLock) {
                            authLock.notify();
                        }
                    }
                };

                // authenticateAccount will persist the account to our secure database if auth is successful
                mPushManager.authenticateAccount(username, password, authCallback);

                synchronized (authLock) {
                    try {
                        authLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public PushManager getPushManager() {
        return mPushManager;
    }

    /**
     * Reports whether the persisted ChatSecure-Push account is valid.
     *
     * @param account              the persisted ChatSecure-Push account
     * @param requestedProviderUrl the URL describing the desired ChatSecure-Push server instance
     *                             where the user's account should be registered
     * @return true if the given account is valid, false if a new account should be registered.
     */
    private static boolean isCspAccountValid(PersistedAccount account,
                                             @NonNull String requestedProviderUrl) {

        return account != null && account.providerUrl.equals(requestedProviderUrl);
    }

    @Override
    public void onCacheWordUninitialized() {
        // unused
    }

    @Override
    public void onCacheWordLocked() {
        // unused
    }

    public void onCacheWordOpened() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "Awaiting ImpsProvider ready");
                // Wait for ImpsProvider to initialize : it listens to onCacheWordOpened as well...
                ImpsProvider.awaitDataReady();
                Log.d(LOG_TAG, "ImpsProvider ready");
                // setupChatSecurePush will disconnect the CacheWordHandler when it's done
                setupChatSecurePush();
            }
        }).start();
    }


    public void speakOut(String word, Locale locale, TextToSpeechCommunicateListener communicateListener) {
        try {
            if(tts != null) {
                tts.setLanguage(locale);
                tts.setCommunicateListener(communicateListener);
                Bundle params = new Bundle();
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");
                tts.speak(word, TextToSpeech.QUEUE_FLUSH, params, "UniqueID");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void speakOut(String word, Locale locale) {
        try {
//            if (Build.VERSION.SDK_INT >= 22) {
//                if(tts != null && tts.getAvailableLanguages().contains(locale)) {
//                    tts.setLanguage(locale);
//                    tts.speak(word, TextToSpeech.QUEUE_FLUSH, null);
//                }
//            } else {
                if(tts != null) {
                    tts.setLanguage(locale);
                    tts.speak(word, TextToSpeech.QUEUE_FLUSH, null);
                }
            //}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
