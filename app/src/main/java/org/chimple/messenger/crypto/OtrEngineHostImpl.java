package org.chimple.messenger.crypto;

import org.chimple.messenger.model.Address;
import org.chimple.messenger.model.Message;
import org.chimple.messenger.plugin.xmpp.XmppAddress;
import org.chimple.messenger.service.adapters.ChatSessionAdapter;
import org.chimple.messenger.service.adapters.ChatSessionManagerAdapter;
import org.chimple.messenger.service.adapters.ImConnectionAdapter;
import org.chimple.messenger.service.RemoteImService;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Hashtable;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrKeyManager;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
import android.content.Context;
import android.os.RemoteException;

/*
 * OtrEngineHostImpl is the connects this app and the OtrEngine
 * http://code.google.com/p/otr4j/wiki/QuickStart
 */
public class OtrEngineHostImpl implements OtrEngineHost {

    private OtrPolicy mPolicy;

    private OtrAndroidKeyManagerImpl mOtrKeyManager;

    private Context mContext;

    private Hashtable<SessionID, String> mSessionResources;

    private RemoteImService mImService;

    public OtrEngineHostImpl(OtrPolicy policy, Context context, OtrAndroidKeyManagerImpl otrKeyManager, RemoteImService imService) throws IOException {
        mPolicy = policy;
        mContext = context;

        mSessionResources = new Hashtable<SessionID, String>();

        mOtrKeyManager = otrKeyManager;

        mImService = imService;


    }

    public void putSessionResource(SessionID session, String resource) {
        mSessionResources.put(session, resource);
    }

    public void removeSessionResource(SessionID session) {
        mSessionResources.remove(session);
    }

    public Address appendSessionResource(SessionID session, Address to) {

        String resource = mSessionResources.get(session);
        if (resource != null)
            return new XmppAddress(to.getBareAddress() + '/' + resource);
        else
            return to;

    }

    public ImConnectionAdapter findConnection(SessionID session) {

        return mImService.getConnection(Address.stripResource(session.getLocalUserId()));
    }

    public OtrKeyManager getKeyManager() {
        return mOtrKeyManager;
    }

    public void storeRemoteKey(SessionID sessionID, PublicKey remoteKey) {
        mOtrKeyManager.savePublicKey(sessionID, remoteKey);
    }

    public boolean isRemoteKeyVerified(String userId) {
        return mOtrKeyManager.isVerified(userId);
    }

    public boolean isRemoteKeyVerified(SessionID sessionID) {
        return mOtrKeyManager.isVerified(sessionID);
    }

    public String getLocalKeyFingerprint(SessionID sessionID) {
        return mOtrKeyManager.getLocalFingerprint(sessionID);
    }

    public String getLocalKeyFingerprint(String userId) {
        return mOtrKeyManager.getLocalFingerprint(userId);
    }

    public String getRemoteKeyFingerprint(SessionID sessionID) {
        return mOtrKeyManager.getRemoteFingerprint(sessionID);
    }

    public String getRemoteKeyFingerprint(String userId) {
        return mOtrKeyManager.getRemoteFingerprint(userId);
    }

    public boolean hasRemoteKeyFingerprint (String userid)
    {
        return mOtrKeyManager.hasRemoteFingerprint(userid);
    }

    public KeyPair getKeyPair(SessionID sessionID) {
        KeyPair kp = null;
        kp = mOtrKeyManager.loadLocalKeyPair(sessionID);

        if (kp == null) {
            mOtrKeyManager.generateLocalKeyPair(sessionID);
            kp = mOtrKeyManager.loadLocalKeyPair(sessionID);
        }
        return kp;
    }

    public OtrPolicy getSessionPolicy(SessionID sessionID) {
        return mPolicy;
    }

    public void setSessionPolicy(OtrPolicy policy) {
        mPolicy = policy;
    }

    public void injectMessage(SessionID sessionID, String text) {
        OtrDebugLogger.log(sessionID.toString() + ": injecting message: " + text);

        ImConnectionAdapter connection = findConnection(sessionID);
        if (connection != null)
        {
            ChatSessionManagerAdapter chatSessionManagerAdapter = (ChatSessionManagerAdapter) connection
                    .getChatSessionManager();
            ChatSessionAdapter chatSessionAdapter = (ChatSessionAdapter) chatSessionManagerAdapter
                    .getChatSession(sessionID.getRemoteUserId());

            if (chatSessionAdapter != null)
            {
                String body = text;

                if (body == null)
                    body = ""; //don't allow null messages, only empty ones!

                Message msg = new Message(body);
                Address to = new XmppAddress(sessionID.getRemoteUserId());
                msg.setTo(to);

                if (!to.getAddress().contains("/")) {
                    //always send OTR messages to a resource
                    msg.setTo(appendSessionResource(sessionID, to));
                }

                // msg ID is set by plugin
                chatSessionManagerAdapter.getChatSessionManager().sendMessageAsync(chatSessionAdapter.getAdaptee(), msg);

            }
            else {
                OtrDebugLogger.log(sessionID.toString() + ": could not find chatSession");

            }
        }
        else
        {
            OtrDebugLogger.log(sessionID.toString() + ": could not find ImConnection");

        }


    }

    public void showError(SessionID sessionID, String error) {
        OtrDebugLogger.log(sessionID.toString() + ": ERROR=" + error);

      //  Toast.makeText(mContext, "ERROR: " + error, Toast.LENGTH_SHORT).show();

    }

    public void showWarning(SessionID sessionID, String warning) {
        OtrDebugLogger.log(sessionID.toString() + ": WARNING=" + warning);

    }


}
