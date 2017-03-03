package org.chimple.messenger.crypto;

// Originally: package com.zadov.beem;

import org.chimple.messenger.ImApp;
import org.chimple.messenger.plugin.xmpp.XmppAddress;
import org.chimple.messenger.push.PushManager;
import org.chimple.messenger.push.WhitelistTokenTlvHandler;
import org.chimple.messenger.push.model.PersistedPushToken;
import org.chimple.messenger.ui.legacy.SmpResponseActivity;
import org.chimple.messenger.model.Contact;
import org.chimple.messenger.model.Message;
import org.chimple.messenger.service.adapters.ImConnectionAdapter;
import org.chimple.messenger.service.ImServiceConstants;
import org.chimple.messenger.service.RemoteImService;
import org.chimple.messenger.util.Debug;
import org.chatsecure.pushsecure.PushSecureClient;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import net.java.otr4j.OtrEngineImpl;
import net.java.otr4j.OtrEngineListener;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrKeyManager;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.session.OtrSm;
import net.java.otr4j.session.OtrSm.OtrSmEngineHost;
import net.java.otr4j.session.Session;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
import net.java.otr4j.session.TLV;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

/*
 * OtrChatManager keeps track of the status of chats and their OTR stuff
 */
public class OtrChatManager implements OtrEngineListener, OtrSmEngineHost {

    //the singleton instance
    private static OtrChatManager mInstance;

    private OtrEngineHostImpl mOtrEngineHost;
    private OtrEngineImpl mOtrEngine;
    private Hashtable<String, SessionID> mSessions;
    private Hashtable<String, OtrSm> mOtrSms;

    private Context mContext;

    // ChatSecure-Push
    private PushManager mPushManager;
    private Hashtable<String, WhitelistTokenTlvHandler> mWhitelistTokenHandlers;
    private HashSet<String> mWhitelistTokenExchangedSessions;

    private final static String TAG = "OtrChatManager";

    private OtrChatManager(int otrPolicy, RemoteImService imService, OtrAndroidKeyManagerImpl otrKeyManager) throws Exception {

        mContext = (Context)imService;

        mOtrEngineHost = new OtrEngineHostImpl(new OtrPolicyImpl(otrPolicy),
                mContext, otrKeyManager, imService);

        mOtrEngine = new OtrEngineImpl(mOtrEngineHost);
        mOtrEngine.addOtrEngineListener(this);

        mSessions = new Hashtable<String, SessionID>();
        mOtrSms = new Hashtable<String, OtrSm>();

        // Use the Application-managed PushManager which has a push account already authenticated
        mPushManager = ((ImApp) mContext.getApplicationContext()).getPushManager();
        mWhitelistTokenHandlers = new Hashtable<>();
        mWhitelistTokenExchangedSessions = new HashSet<>();

    }


    public static synchronized OtrChatManager getInstance(int otrPolicy, RemoteImService imService, OtrAndroidKeyManagerImpl otrKeyManager)
            throws Exception {
        if (mInstance == null) {
            mInstance = new OtrChatManager(otrPolicy, imService,otrKeyManager);
        }

        return mInstance;
    }

    public static OtrChatManager getInstance()
    {
        return mInstance;
    }

    public static void endAllSessions() {
        if (mInstance == null) {
            return;
        }
        Collection<SessionID> sessionIDs = mInstance.mSessions.values();
        for (SessionID sessionId : sessionIDs) {
            mInstance.endSession(sessionId);
        }
    }

    public static void endSessionsForAccount(Contact localUserContact) {
        if (mInstance == null) {
            return;
        }
        String localUserId = localUserContact.getAddress().getBareAddress();
        
        Enumeration<String> sKeys = mInstance.mSessions.keys();
        
        while (sKeys.hasMoreElements())
        {
            String sKey = sKeys.nextElement();
            if (sKey.contains(localUserId))
            {
                SessionID sessionId = mInstance.mSessions.get(sKey);
                
                if (sessionId != null)
                    mInstance.endSession(sessionId);
            }
        }
    }

    public void addOtrEngineListener(OtrEngineListener oel) {
        mOtrEngine.addOtrEngineListener(oel);

    }

    public void setPolicy(int otrPolicy) {
        mOtrEngineHost.setSessionPolicy(new OtrPolicyImpl(otrPolicy));
    }

    public OtrKeyManager getKeyManager() {
        return mOtrEngineHost.getKeyManager();
    }

    public static String processResource(String userId) {
        String[] splits = userId.split("/", 2);
        if (splits.length > 1)
            return splits[1];
        else
            return "UNKNOWN";
    }

    private final static String SESSION_TYPE_XMPP = "XMPP";

    public SessionID getSessionId(String localUserId, String remoteUserId) {

        //if (!remoteUserId.contains("/"))
        //   Log.w(ImApp.LOG_TAG,"resource is not set: " + remoteUserId);

       // boolean stripResource = !remoteUserId.startsWith("group");
        //SessionID sIdTemp = new SessionID(localUserId, XmppAddress.stripResource(remoteUserId), SESSION_TYPE_XMPP);
        SessionID sIdTemp = new SessionID(localUserId, remoteUserId, SESSION_TYPE_XMPP);
        SessionID sessionId = mSessions.get(sIdTemp.toString());

        if (sessionId == null)
        {
            sIdTemp = new SessionID(localUserId, XmppAddress.stripResource(remoteUserId), SESSION_TYPE_XMPP);
            sessionId = mSessions.get(sIdTemp.toString());
            if (sessionId != null)
                return sessionId;
        }

        if (sessionId == null)
        {
         // or we didn't have a session yet.
            sessionId = sIdTemp;
            mSessions.put(sessionId.toString(), sessionId);
        }
        else if ((!sessionId.getRemoteUserId().equals(remoteUserId)) &&
                        remoteUserId.contains("/")) {
            // Remote has changed (either different presence, or from generic JID to specific presence),
            // Create or replace sessionId with one that is specific to the new presence.

            sessionId = sIdTemp;
            mSessions.put(sessionId.toString(), sessionId);

            if (Debug.DEBUG_ENABLED)
                Log.d(ImApp.LOG_TAG,"getting new otr session id: " + sessionId);

        }

        return sessionId;
    }

    /**
     * Tell if the session represented by a local user account and a remote user
     * account is currently encrypted or not.
     *
     * @param localUserId
     * @param remoteUserId
     * @return state
     */
    public SessionStatus getSessionStatus(String localUserId, String remoteUserId) {
        SessionID sessionId = getSessionId(localUserId, remoteUserId);
        if (sessionId == null)
            return null;

        return mOtrEngine.getSessionStatus(sessionId);

    }

    public SessionStatus getSessionStatus(SessionID sessionId) {

        return mOtrEngine.getSessionStatus(sessionId);

    }

    /**
    public void refreshSession(String localUserId, String remoteUserId) {
        try {
            mOtrEngine.refreshSession(getSessionId(localUserId, remoteUserId));
        } catch (OtrException e) {
            OtrDebugLogger.log("refreshSession", e);
        }
    }*/

    /**
     * Start a new OTR encryption session for the chat session represented by a
     * local user address and a remote user address.
     *
     * @param localUserId i.e. the account of the user of this phone
     * @param remoteUserId i.e. the account that this user is talking to
     */
    private SessionID startSession(String localUserId, String remoteUserId) throws Exception {

        if (!remoteUserId.contains("/"))
            throw new Exception("can't start session without JabberID: " + localUserId);

        SessionID sessionId = getSessionId(localUserId, remoteUserId);

        try {

            mOtrEngine.startSession(sessionId);

            return sessionId;

        } catch (OtrException e) {
            OtrDebugLogger.log("startSession", e);

            showError(sessionId, "Unable to start OTR session: " + e.getLocalizedMessage());

        }

        return null;
    }


    /**
     * Start a new OTR encryption session for the chat session represented by a
     * {@link SessionID}.
     *
     * @param sessionId the {@link SessionID} of the OTR session
     */
    public SessionID startSession(SessionID sessionId) {

        try {

            mOtrEngine.startSession(sessionId);
            
            return sessionId;

        } catch (OtrException e) {
            OtrDebugLogger.log("startSession", e);

            showError(sessionId,"Unable to start OTR session: " + e.getLocalizedMessage());

        }

        return null;
    }




    public void endSession(SessionID sessionId) {

        try {

            mOtrEngine.endSession(sessionId);
            mSessions.remove(sessionId.toString());

        } catch (OtrException e) {
            OtrDebugLogger.log("endSession", e);
        }
    }

    public void endSession(String localUserId, String remoteUserId) {

        SessionID sessionId = getSessionId(localUserId, remoteUserId);
        endSession(sessionId);

    }

    public void status(String localUserId, String remoteUserId) {
        mOtrEngine.getSessionStatus(getSessionId(localUserId, remoteUserId)).toString();
    }

    public String decryptMessage(String localUserId, String remoteUserId, String msg, List<TLV> tlvs) throws OtrException {

        String plain = null;

        SessionID sessionId = getSessionId(localUserId, remoteUserId);
       // OtrDebugLogger.log("session status: " + mOtrEngine.getSessionStatus(sessionId));

        if (mOtrEngine != null && sessionId != null) {

            mOtrEngineHost.putSessionResource(sessionId, processResource(remoteUserId));

            plain = mOtrEngine.transformReceiving(sessionId, msg, tlvs);

            OtrSm otrSm = mOtrSms.get(sessionId.toString());

            if (otrSm != null) {
                List<TLV> smTlvs = otrSm.getPendingTlvs();
                if (smTlvs != null) {
                    String encrypted = mOtrEngine.transformSending(sessionId, "", smTlvs);
                    mOtrEngineHost.injectMessage(sessionId, encrypted);
                }
            }

            //not null, but empty so make it null!
            if (TextUtils.isEmpty(plain))
                return null;
        }

        return plain;
    }

    public boolean transformSending(Message message) {
        return transformSending(message, false, null);
    }

    public boolean transformSending(Message message, boolean isResponse, byte[] data) {
        String localUserId = message.getFrom().getAddress();
        String remoteUserId = message.getTo().getAddress();
        String body = message.getBody();

        SessionID sessionId = getSessionId(localUserId, remoteUserId);

        if (mOtrEngine != null && sessionId != null) {
            SessionStatus sessionStatus = mOtrEngine.getSessionStatus(sessionId);
            if (data != null && sessionStatus != SessionStatus.ENCRYPTED) {
                // Cannot send data without OTR, so start a session and drop message.
                // Message will be resent by caller when session is encrypted.
                startSession(sessionId);
                OtrDebugLogger.log("auto-start OTR on data send request");
                return false;
            }

            OtrDebugLogger.log("session status: " + sessionStatus);

            try {
                OtrPolicy sessionPolicy = getSessionPolicy(sessionId);

                if (sessionStatus == SessionStatus.PLAINTEXT && sessionPolicy.getRequireEncryption())
                {
                    startSession(sessionId);
                    return false;
                }
                if (sessionStatus != SessionStatus.PLAINTEXT || sessionPolicy.getRequireEncryption()) {
                    body = mOtrEngine.transformSending(sessionId, body, isResponse, data);

                    if (!message.getTo().getAddress().contains("/"))
                        message.setTo(mOtrEngineHost.appendSessionResource(sessionId, message.getTo()));

                } else if (sessionStatus == SessionStatus.PLAINTEXT && sessionPolicy.getAllowV2()
                           && sessionPolicy.getSendWhitespaceTag()) {
                    // Work around asmack not sending whitespace tag for auto discovery
                    body += " \t  \t\t\t\t \t \t \t   \t \t  \t   \t\t  \t ";

                }
            } catch (OtrException e) {
                OtrDebugLogger.log("error encrypting", e);
                return false;
            }
        }

        message.setBody(body);
        
        return true;
    }

    @Override
    public void sessionStatusChanged(final SessionID sessionID) {
        SessionStatus sStatus = mOtrEngine.getSessionStatus(sessionID);

        OtrDebugLogger.log("session status changed: " + sStatus);

        final Session session = mOtrEngine.getSession(sessionID);
        OtrSm otrSm = mOtrSms.get(sessionID.toString());

        WhitelistTokenTlvHandler tokenTlvHandler = mWhitelistTokenHandlers.get(sessionID.toString());

        if (sStatus == SessionStatus.ENCRYPTED) {

            PublicKey remoteKey = mOtrEngine.getRemotePublicKey(sessionID);
            mOtrEngineHost.storeRemoteKey(sessionID, remoteKey);

            if (otrSm == null) {
                // SMP handler - make sure we only add this once per session!
                otrSm = new OtrSm(session, mOtrEngineHost.getKeyManager(),
                        sessionID, OtrChatManager.this);
                session.addTlvHandler(otrSm);

                mOtrSms.put(sessionID.toString(), otrSm);
            }

            if (tokenTlvHandler == null) {
                // ChatSecure-Push Whitelist Token Handler - One per session
                tokenTlvHandler = new WhitelistTokenTlvHandler(mPushManager, sessionID,
                        new WhitelistTokenTlvHandler.TlvSender() {

                            @Override
                            public void onSendRequested(@NonNull TLV tlv, @NonNull SessionID sessionId) {
                                ArrayList<TLV> tlvList = new ArrayList<>(1);
                                tlvList.add(tlv);
                                String encrypted;
                                try {
                                    encrypted = mOtrEngine.transformSending(sessionId, "", tlvList);
                                    mOtrEngineHost.injectMessage(sessionId, encrypted);

                                    // We only need to perform the Whitelist Token TLV Exchange once per-session
                                    // After we've responded to this session's TLV exchange, remove TLV handler
                                    WhitelistTokenTlvHandler tokenTlvHandler = mWhitelistTokenHandlers.remove(sessionId.toString());
                                    Session session = mOtrEngine.getSession(sessionId);
                                    session.removeTlvHandler(tokenTlvHandler);

                                } catch (OtrException e) {
                                    Log.d(TAG, "Failed to encrypt outbound Whitelist Token TLV");
                                }
                            }
                        });
                session.addTlvHandler(tokenTlvHandler);
                mWhitelistTokenHandlers.put(sessionID.toString(), tokenTlvHandler);

                // Ensure we have a ChatSecure-Push Whitelist Token available
                // to send to this Session's participant when the first message is sent
                /**
                 * /// we don't use tokens yet since we don't receive pushes
                mPushManager.createReceivingWhitelistTokenForPeer(
                        PushManager.stripJabberIdResource(sessionID.getLocalUserId()),
                        PushManager.stripJabberIdResource(sessionID.getRemoteUserId()),
                        new PushSecureClient.RequestCallback<PersistedPushToken>() {
                            @Override
                            public void onSuccess(@NonNull PersistedPushToken response) {

                                if (Debug.DEBUG_ENABLED)
                                    Log.d(TAG, "Prepared push whitelist token for " + sessionID.getRemoteUserId());
                                // the token has already been persisted by pushManager
                            }

                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                if (Debug.DEBUG_ENABLED)
                                    Log.e(TAG, "Failed to prepare push whitelist token for " + sessionID.getRemoteUserId(), t);
                            }
                        });
                 */
            }



        } else if (sStatus == SessionStatus.PLAINTEXT) {
            if (otrSm != null) {
                session.removeTlvHandler(otrSm);
                mOtrSms.remove(sessionID.toString());
            }

            if (tokenTlvHandler != null) {
                session.removeTlvHandler(tokenTlvHandler);
                mWhitelistTokenHandlers.remove(sessionID.toString());
            }

            mOtrEngineHost.removeSessionResource(sessionID);


        } else if (sStatus == SessionStatus.FINISHED) {
            // Do nothing.  The user must take affirmative action to
            // restart or end the session, so that they don't send
            // plaintext by mistake.
        }

    }

    public boolean isRemoteKeyVerified (String userId)
    {
        return mOtrEngineHost.isRemoteKeyVerified(userId);
    }

    public String getRemoteKeyFingerprint (String userId)
    {
        return mOtrEngineHost.getRemoteKeyFingerprint(userId);
    }

    public boolean hasRemoteKeyFingerprint (String userId)
    {
        return mOtrEngineHost.hasRemoteKeyFingerprint(userId);
    }

    public String getLocalKeyFingerprint(String localUserId) {
        return mOtrEngineHost.getLocalKeyFingerprint(localUserId);
    }

    @Override
    public void injectMessage(SessionID sessionID, String msg) {

        mOtrEngineHost.injectMessage(sessionID, msg);
    }

    @Override
    public void showWarning(SessionID sessionID, String warning) {

        mOtrEngineHost.showWarning(sessionID, warning);

    }

    @Override
    public void showError(SessionID sessionID, String error) {
        mOtrEngineHost.showError(sessionID, error);


    }

    @Override
    public OtrPolicy getSessionPolicy(SessionID sessionID) {

        return mOtrEngineHost.getSessionPolicy(sessionID);
    }

    @Override
    public KeyPair getKeyPair(SessionID sessionID) {
        return mOtrEngineHost.getKeyPair(sessionID);
    }

    @Override
    public void askForSecret(SessionID sessionID, String question) {

        Intent dialog = new Intent(mContext.getApplicationContext(), SmpResponseActivity.class);
        dialog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        dialog.putExtra("q", question);
        dialog.putExtra("sid", sessionID.getRemoteUserId());//yes "sid" = remoteUserId in this case - see SMPResponseActivity
        ImConnectionAdapter connection = mOtrEngineHost.findConnection(sessionID);
        if (connection == null) {
            OtrDebugLogger.log("Could ask for secret - no connection for " + sessionID.toString());
            return;
        }

        dialog.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, connection.getProviderId());

        mContext.getApplicationContext().startActivity(dialog);

    }

    public void respondSmp(SessionID sessionID, String secret) throws OtrException {
        OtrSm otrSm = mOtrSms.get(sessionID.toString());

        List<TLV> tlvs;

        if (otrSm == null) {
            showError(sessionID, "Could not respond to verification because conversation is not encrypted");
            return;
        }

        tlvs = otrSm.initRespondSmp(null, secret, false);
        String encrypted = mOtrEngine.transformSending(sessionID, "", tlvs);
        mOtrEngineHost.injectMessage(sessionID, encrypted);
    }

    public void initSmp(SessionID sessionID, String question, String secret) throws OtrException {
        OtrSm otrSm = mOtrSms.get(sessionID.toString());

        List<TLV> tlvs;

        if (otrSm == null) {
            showError(sessionID, "Could not perform verification because conversation is not encrypted");
            return;
        }

        tlvs = otrSm.initRespondSmp(question, secret, true);
        String encrypted = mOtrEngine.transformSending(sessionID, "", tlvs);
        mOtrEngineHost.injectMessage(sessionID, encrypted);
    }

    public void abortSmp(SessionID sessionID) throws OtrException {
        OtrSm otrSm = mOtrSms.get(sessionID.toString());

        if (otrSm == null)
            return;

        List<TLV> tlvs = otrSm.abortSmp();
        String encrypted = mOtrEngine.transformSending(sessionID, "", tlvs);
        mOtrEngineHost.injectMessage(sessionID, encrypted);
    }

    /**
     * Create a message body describing the ChatSecure-Push Whitelist Token Exchange.
     * See <a href="https://github.com/ChatSecure/ChatSecure-Push-Server/wiki/Chat-Client-Implementation-Notes#json-whitelist-token-exchange">JSON Whitelist Token Exchange</a>
     *
     * @param message         A {@link Message} providing the 'to' & 'from' addresses, as well as
     *                        any message body text (this is currently unused by the ChatSecure-Push
     *                        spec).
     * @param whitelistTokens An Array of one or more ChatSecure-Push Whitelist tokens
     */
    public boolean transformPushWhitelistTokenSending(@NonNull Message message,
                                                      @NonNull String[] whitelistTokens) {

        String localUserId = message.getFrom().getAddress();
        String remoteUserId = message.getTo().getAddress();
        String body = message.getBody();

        SessionID sessionId = getSessionId(localUserId, remoteUserId);

        try {

            TLV tokenTlv = mPushManager.createWhitelistTokenExchangeTlvWithToken(whitelistTokens, null);

            List<TLV> tlvs = new ArrayList<>(1);
            tlvs.add(tokenTlv);

            if (mOtrEngine != null && sessionId != null) {
                SessionStatus sessionStatus = mOtrEngine.getSessionStatus(sessionId);
                if (sessionStatus != SessionStatus.ENCRYPTED) {
                    // Cannot send Whitelist token without OTR.
                    // TODO: Is it possible to Postpone-send a TLV message?
                    OtrDebugLogger.log("Could not send ChatSecure-Push Whitelist Token TLV. Session not encrypted.");
                    return false;
                }
                OtrDebugLogger.log("session status: " + sessionStatus);

                body = mOtrEngine.transformSending(sessionId, body, tlvs);
                message.setTo(mOtrEngineHost.appendSessionResource(sessionId, message.getTo()));
            }

            message.setBody(body);

            return true;

        } catch (UnsupportedEncodingException e) {

            Log.e(ImApp.LOG_TAG, "Failed to craft ChatSecure-Push Whitelist Token TLV", e);
            return false;
        } catch (OtrException e) {
            OtrDebugLogger.log("error encrypting", e);
            return false;
        }
    }

    /**
     * Begins the ChatSecure-Push Whitelist Token Exchange if not yet performed for the
     * given {@param sessionID}.
     */
    public void maybeBeginPushWhitelistTokenExchange(@NonNull final SessionID sessionID) {
        if (mWhitelistTokenExchangedSessions.contains(sessionID.toString())) return;

        mWhitelistTokenExchangedSessions.add(sessionID.toString());
        try {
            mPushManager.createWhitelistTokenExchangeTlv(
                    PushManager.stripJabberIdResource(sessionID.getLocalUserId()),
                    PushManager.stripJabberIdResource(sessionID.getRemoteUserId()),
                    new PushSecureClient.RequestCallback<TLV>() {
                        @Override
                        public void onSuccess(@NonNull TLV response) {

                            try {
                                ArrayList<TLV> outboundTlvs = new ArrayList<>();
                                outboundTlvs.add(response);
                                String encrypted = mOtrEngine.transformSending(sessionID, "", outboundTlvs);
                                mOtrEngineHost.injectMessage(sessionID, encrypted);
                                if (Debug.DEBUG_ENABLED)
                                    Log.d(TAG, "Began Push Whitelist Token TLV Exchange");
                            } catch (OtrException e) {
                                Log.e(TAG, "Failed to encrypt outbound Whitelist Token TLV");
                                mWhitelistTokenExchangedSessions.remove(sessionID.toString());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Throwable t) {
                            Log.e(TAG, "Failed to obtain Whitelist Token", t);
                            mWhitelistTokenExchangedSessions.remove(sessionID.toString());
                        }
                    }, null);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Failed to begin Push Whitelist Token Exchange", e);
            mWhitelistTokenExchangedSessions.remove(sessionID.toString());
        }
    }

    /**
     * Send a ChatSecure-Push "Knock" Push Message to the remote peer in the
     * given {@param sessionID}.
     */
    public void sendKnockPushMessage(@NonNull final SessionID sessionID) {

        mPushManager.sendPushMessageToPeer(
                PushManager.stripJabberIdResource(sessionID.getLocalUserId()),
                PushManager.stripJabberIdResource(sessionID.getRemoteUserId()),
                new PushSecureClient.RequestCallback<org.chatsecure.pushsecure.response.Message>() {
                    @Override
                    public void onSuccess(@NonNull org.chatsecure.pushsecure.response.Message response) {
                        if (Debug.DEBUG_ENABLED)
                            Log.d(TAG, "Sent push message to " + sessionID.getRemoteUserId());
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        if (Debug.DEBUG_ENABLED)
                            Log.d(TAG, "Failed to send push message to " + sessionID.getRemoteUserId());
                    }
                });

        try {
            mOtrEngine.clearSession(sessionID);
            mOtrEngineHost.removeSessionResource(sessionID);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Send a ChatSecure-Push "Knock" Push Message to the remote peer in the
     * given {@param sessionID}.
     */
    public boolean canDoKnockPushMessage(@NonNull final SessionID sessionID) {
        return mPushManager.hasPersistedWhitelistToken(
                PushManager.stripJabberIdResource(sessionID.getRemoteUserId()),
                PushManager.stripJabberIdResource(sessionID.getLocalUserId()));
    }
}
