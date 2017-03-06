package org.chimple.messenger.plugin.xmpp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import org.chimple.messenger.ImApp;
import org.chimple.messenger.crypto.TorProxyInfo;
import org.chimple.messenger.model.Address;
import org.chimple.messenger.model.ChatGroup;
import org.chimple.messenger.model.ChatGroupManager;
import org.chimple.messenger.model.ChatSession;
import org.chimple.messenger.model.ChatSessionManager;
import org.chimple.messenger.model.Contact;
import org.chimple.messenger.model.ContactList;
import org.chimple.messenger.model.ContactListListener;
import org.chimple.messenger.model.ContactListManager;
import org.chimple.messenger.model.ImConnection;
import org.chimple.messenger.model.ImEntity;
import org.chimple.messenger.model.ImErrorInfo;
import org.chimple.messenger.model.ImException;
import org.chimple.messenger.model.Invitation;
import org.chimple.messenger.model.Message;
import org.chimple.messenger.model.Presence;
import org.chimple.messenger.provider.Imps;
import org.chimple.messenger.provider.ImpsErrorInfo;
import org.chimple.messenger.service.IChatSession;
import org.chimple.messenger.service.adapters.ChatSessionAdapter;
import org.chimple.messenger.ui.legacy.DatabaseUtils;
import org.chimple.messenger.util.Debug;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PresenceListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.DefaultExtensionElement;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.address.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.ChatStateListener;
import org.jivesoftware.smackx.chatstates.ChatStateManager;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.commands.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.disco.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.hoxt.packet.AbstractHttpOverXmpp;
import org.jivesoftware.smackx.iqlast.packet.LastActivity;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.muc.SubjectUpdatedListener;
import org.jivesoftware.smackx.muc.provider.MUCAdminProvider;
import org.jivesoftware.smackx.muc.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.muc.provider.MUCUserProvider;
import org.jivesoftware.smackx.offline.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.offline.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.privacy.PrivacyListManager;
import org.jivesoftware.smackx.privacy.packet.PrivacyItem;
import org.jivesoftware.smackx.privacy.provider.PrivacyProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.search.UserSearch;
import org.jivesoftware.smackx.sharedgroups.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.si.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.vcardtemp.provider.VCardProvider;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.provider.DataFormProvider;
import org.jivesoftware.smackx.xhtmlim.provider.XHTMLExtensionProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import im.zom.messenger.R;


public class XmppConnection extends ImConnection {

    private static final String DISCO_FEATURE = "http://jabber.org/protocol/disco#info";
    final static String TAG = "ZomXMPP";
    private final static boolean PING_ENABLED = true;

    private XmppContactListManager mContactListManager;
    private Contact mUser;
    private boolean mUseTor;

    // watch out, this is a different XMPPConnection class than XmppConnection! ;)
    // Synchronized by executor thread
    private XMPPTCPConnection mConnection;
    private XmppStreamHandler mStreamHandler;
    private ChatManager mChatManager;

    private Roster mRoster;

    private XmppChatSessionManager mSessionManager;
    private XMPPTCPConnectionConfiguration.Builder mConfig;

    // True if we are in the process of reconnecting.  Reconnection is retried once per heartbeat.
    // Synchronized by executor thread.
    private boolean mNeedReconnect;

    private boolean mRetryLogin;
    private ThreadPoolExecutor mExecutor;
    private Timer mTimerPresence;

    private ProxyInfo mProxyInfo = null;

    private long mAccountId = -1;
    private long mProviderId = -1;

    private boolean mIsGoogleAuth = false;

    private final static String SSLCONTEXT_TYPE = "TLS";

    private static SSLContext sslContext;
    private MemorizingTrustManager mMemTrust;

    private final static int SOTIMEOUT = 1000 * 120;

    private PingManager mPingManager;

    private String mUsername;
    private String mPassword;
    private String mResource;
    private int mPriority;

    private int mGlobalId;
    private static int mGlobalCount;

    private SecureRandom rndForTorCircuits = null;

    // Maintains a sequence counting up to the user configured heartbeat interval
    private int heartbeatSequence = 0;

    private LinkedList<String> qAvatar = new LinkedList <String>();

    private LinkedList<org.jivesoftware.smack.packet.Presence> qPresence = new LinkedList<org.jivesoftware.smack.packet.Presence>();
    private LinkedList<org.jivesoftware.smack.packet.Stanza> qPacket = new LinkedList<org.jivesoftware.smack.packet.Stanza>();

    private final static String DEFAULT_CONFERENCE_SERVER = "conference.zom.im";

    private final static String PRIVACY_LIST_DEFAULT = "defaultprivacylist";

    public XmppConnection(Context context) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        super(context);

        synchronized (XmppConnection.class) {
            mGlobalId = mGlobalCount++;
        }

        Debug.onConnectionStart();

        SmackConfiguration.setDefaultPacketReplyTimeout(SOTIMEOUT);

        // Create a single threaded executor.  This will serialize actions on the underlying connection.
        createExecutor();

        addProviderManagerExtensions();

        XmppStreamHandler.addExtensionProviders();
       // DeliveryReceipts.addExtensionProviders();

       // ServiceDiscoveryManager.setIdentityName("ChatSecure");
       // ServiceDiscoveryManager.setIdentityType("phone");
    }

    public void initUser(long providerId, long accountId) throws ImException
    {
        ContentResolver contentResolver = mContext.getContentResolver();

        Cursor cursor = contentResolver.query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(providerId)}, null);

        if (cursor == null)
            throw new ImException("unable to query settings");

        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                cursor, contentResolver, providerId, false, null);

        mProviderId = providerId;
        mAccountId = accountId;
        mUser = makeUser(providerSettings, contentResolver);
        mUseTor = providerSettings.getUseTor();

        providerSettings.close();
    }

    private synchronized Contact makeUser(Imps.ProviderSettings.QueryMap providerSettings, ContentResolver contentResolver) {

        Contact contactUser = null;

        String nickname = Imps.Account.getNickname(contentResolver, mAccountId);
        String userName = Imps.Account.getUserName(contentResolver, mAccountId);
        String domain = providerSettings.getDomain();
        String xmppName = userName + '@' + domain + '/' + providerSettings.getXmppResource();
        contactUser = new Contact(new XmppAddress(xmppName), nickname);

        return contactUser;
    }

    private void createExecutor() {
       mExecutor = new ThreadPoolExecutor(1, 1, 1L, TimeUnit.SECONDS,
              new LinkedBlockingQueue<Runnable>());

    }

    private boolean execute(Runnable runnable) {

        if (mExecutor == null)
            createExecutor (); //if we disconnected, will need to recreate executor here, because join() made it null

        try {
            mExecutor.execute(runnable);
        } catch (RejectedExecutionException ex) {
            return false;
        }
        return true;
    }

    // Execute a runnable only if we are idle
    private boolean executeIfIdle(Runnable runnable) {
        if (mExecutor.getActiveCount() + mExecutor.getQueue().size() == 0) {
            return execute(runnable);
       }

       return false;
    }

    // This runs in executor thread, and since there is only one such thread, we will definitely
    // succeed in shutting down the executor if we get here.
    public void join() {
        final ExecutorService executor = mExecutor;
        mExecutor = null;
        // This will send us an interrupt, which we will ignore.  We will terminate
        // anyway after the caller is done.  This also drains the executor queue.
        if (executor != null)
            executor.shutdownNow();
    }

    // For testing
    boolean joinGracefully() throws InterruptedException {
        final ExecutorService executor = mExecutor;
        mExecutor = null;
        // This will send us an interrupt, which we will ignore.  We will terminate
        // anyway after the caller is done.  This also drains the executor queue.
        if (executor != null) {
            executor.shutdown();
            return executor.awaitTermination(1, TimeUnit.SECONDS);
        }

        return false;
    }

    public void sendPacket(org.jivesoftware.smack.packet.Stanza packet) {
        qPacket.add(packet);
    }

    void postpone(final org.jivesoftware.smack.packet.Stanza packet) {
        if (packet instanceof org.jivesoftware.smack.packet.Message) {
            boolean groupChat = ((org.jivesoftware.smack.packet.Message) packet).getType().equals( org.jivesoftware.smack.packet.Message.Type.groupchat);
            ChatSession session = findOrCreateSession(packet.getTo(), groupChat);
            if (session != null)
                session.onMessagePostponed(packet.getStanzaId());
        }
    }


    private boolean mLoadingAvatars = false;

    private void loadVCardsAsync ()
    {
        if (!mLoadingAvatars)
        {
            execute(new AvatarLoader());
        }
    }

    private class AvatarLoader implements Runnable
    {
        @Override
        public void run () {

            mLoadingAvatars = true;

            ContentResolver resolver = mContext.getContentResolver();

            try
            {
                while (qAvatar.size()>0)
                {

                    loadVCard (resolver, qAvatar.poll());

                }
            }
            catch (Exception e) {}

            mLoadingAvatars = false;
        }
    }

    private boolean loadVCard (ContentResolver resolver, String jid)
    {
        try {
                debug(TAG, "loading vcard for: " + jid);

                VCardManager vCardManager = VCardManager.getInstanceFor(mConnection);
                VCard vCard = vCardManager.loadVCard(jid);

                Contact contact = mContactListManager.getContact(jid);

                if (!TextUtils.isEmpty(vCard.getNickName()))
                {
                    if (!vCard.getNickName().equals(contact.getName()))
                    {
                        contact.setName(vCard.getNickName());
                        mContactListManager.doSetContactName(contact.getAddress().getBareAddress(), contact.getName());
                       // mContactListManager.doAddContactToListAsync(contact, getContactListManager().getDefaultContactList(), false);
                    }

                }



                    // If VCard is loaded, then save the avatar to the personal folder.
                String avatarHash = vCard.getAvatarHash();

                if (avatarHash != null)
                {
                    byte[] avatarBytes = vCard.getAvatar();

                    if (avatarBytes != null)
                    {

                        debug(TAG, "found avatar image in vcard for: " + jid);
                        debug(TAG, "start avatar length: " + avatarBytes.length);

                        int width = ImApp.DEFAULT_AVATAR_WIDTH;
                        int height = ImApp.DEFAULT_AVATAR_HEIGHT;

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.length,options);
                        options.inSampleSize = DatabaseUtils.calculateInSampleSize(options, width, height);
                        options.inJustDecodeBounds = false;

                        Bitmap b = BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.length,options);
                        b = Bitmap.createScaledBitmap(b, ImApp.DEFAULT_AVATAR_WIDTH, ImApp.DEFAULT_AVATAR_HEIGHT, false);

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        b.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                        byte[] avatarBytesCompressed = stream.toByteArray();

                        debug(TAG, "compressed avatar length: " + avatarBytesCompressed.length);

                        DatabaseUtils.insertAvatarBlob(resolver, Imps.Avatars.CONTENT_URI, mProviderId, mAccountId, avatarBytesCompressed, avatarHash, XmppAddress.stripResource(jid));

                        // int providerId, int accountId, byte[] data, String hash,String contact
                        return true;
                    }
                }



        } catch (Exception e) {

            debug(TAG, "err loading vcard: " + e.toString());

            if (e.getMessage() != null)
            {
                String streamErr = e.getMessage();

                if (streamErr != null && (streamErr.contains("404") || streamErr.contains("503")))
                {
                    return false;
                }
            }

        }

        return false;
    }

    @Override
    protected void doUpdateUserPresenceAsync(Presence presence) {
        org.jivesoftware.smack.packet.Presence packet = makePresencePacket(presence);

        sendPacket(packet);
        mUserPresence = presence;
        notifyUserPresenceUpdated();
    }

    private org.jivesoftware.smack.packet.Presence makePresencePacket(Presence presence) {
        String statusText = presence.getStatusText();
        org.jivesoftware.smack.packet.Presence.Type type = org.jivesoftware.smack.packet.Presence.Type.available;
        org.jivesoftware.smack.packet.Presence.Mode mode = org.jivesoftware.smack.packet.Presence.Mode.available;
        int priority = mPriority;
        final int status = presence.getStatus();
        if (status == Presence.AWAY) {
            priority = 10;
            mode = org.jivesoftware.smack.packet.Presence.Mode.away;
        } else if (status == Presence.IDLE) {
            priority = 15;
            mode = org.jivesoftware.smack.packet.Presence.Mode.away;
        } else if (status == Presence.DO_NOT_DISTURB) {
            priority = 5;
            mode = org.jivesoftware.smack.packet.Presence.Mode.dnd;
        } else if (status == Presence.OFFLINE) {
            priority = 0;
            type = org.jivesoftware.smack.packet.Presence.Type.unavailable;
            statusText = "Offline";
        }

        // The user set priority is the maximum allowed
        if (priority > mPriority)
            priority = mPriority;

        org.jivesoftware.smack.packet.Presence packet = new org.jivesoftware.smack.packet.Presence(
                type, statusText, priority, mode);

        try {
            byte[] avatar = DatabaseUtils.getAvatarBytesFromAddress(mContext.getContentResolver(), mUser.getAddress().getBareAddress(), 256, 256);
            if (avatar != null) {
                VCardTempXUpdatePresenceExtension vcardExt = new VCardTempXUpdatePresenceExtension(avatar);
                packet.addExtension(vcardExt);
            }
        }
        catch (Exception e)
        {
            debug(TAG,"error upading presence with avatar hash",e);
        }

        return packet;
    }

    @Override
    public int getCapability() {

        return ImConnection.CAPABILITY_SESSION_REESTABLISHMENT | ImConnection.CAPABILITY_GROUP_CHAT;
    }

    private XmppChatGroupManager mChatGroupManager = null;

    @Override
    public synchronized ChatGroupManager getChatGroupManager() {

        if (mChatGroupManager == null)
            mChatGroupManager = new XmppChatGroupManager();

        return mChatGroupManager;
    }

    public class XmppChatGroupManager extends ChatGroupManager
    {

        private Hashtable<String,MultiUserChat> mMUCs = new Hashtable<String,MultiUserChat>();

        public MultiUserChat getMultiUserChat (String chatRoomJid)
        {
            return mMUCs.get(chatRoomJid);
        }
        
        public void reconnectAll ()
        {
            Enumeration<MultiUserChat> eMuc = mMUCs.elements();
            while (eMuc.hasMoreElements())
            {
                MultiUserChat muc = eMuc.nextElement();
                if (!muc.isJoined())
                {
                    try {
                        //muc.join(muc.getNickname());
                        muc.join(muc.getNickname());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public String getDefaultGroupChatService ()
        {
            try {
                // Create a MultiUserChat using a Connection for a room
                MultiUserChatManager mucMgr = MultiUserChatManager.getInstanceFor(mConnection);
                Collection<String> servers = mucMgr.getServiceNames();

                //just grab the first one
                for (String server : servers)
                    return server;
            }
            catch (Exception xe)
            {
                //unable to find conference server
                return DEFAULT_CONFERENCE_SERVER;
            }

            return DEFAULT_CONFERENCE_SERVER;
        }

        @Override
        public boolean createChatGroupAsync(String chatRoomJid, String subject, String nickname) throws Exception {

            if (mConnection == null || getState() != ImConnection.LOGGED_IN)
                return false;
            
            RoomInfo roomInfo = null;

            // Create a MultiUserChat using a Connection for a room
            MultiUserChatManager mucMgr = MultiUserChatManager.getInstanceFor(mConnection);

            if (chatRoomJid.endsWith("@"))
            {
                 //let's add a host to that!
                Collection<String> servers = mucMgr.getServiceNames();

                if (servers.iterator().hasNext())
                    chatRoomJid += servers.iterator().next();
                else
                {
                    chatRoomJid += DEFAULT_CONFERENCE_SERVER;
                }
             }

            Address address = new XmppAddress (chatRoomJid);

            String[] parts = chatRoomJid.split("@");
            String room = parts[0];
            String server = parts[1];

            if (TextUtils.isEmpty(subject))
                subject = room;

            if (TextUtils.isEmpty(nickname))
                nickname = mUsername;

            try {

                MultiUserChat muc = mucMgr.getMultiUserChat(chatRoomJid);
                boolean mucCreated = false;

                try
                {
                    // Create the room
                    DiscussionHistory history = new DiscussionHistory();
                    history.setMaxStanzas(20);
                    long timeout = 30*1000;//30 seconds
                    mucCreated = muc.createOrJoin(nickname, null, history, timeout);
                }
                catch (Exception iae)
                {

                    if (iae.getMessage().contains("Creation failed"))
                    {
                        //some server's don't return the proper 201 create code, so we can just assume the room was created!
                    }
                    else
                    {

                        throw iae;

                    }
                }

                ChatGroup chatGroup = mGroups.get(chatRoomJid);

                if (chatGroup == null) {
                    chatGroup = new ChatGroup(address, subject, this);
                    mGroups.put(chatRoomJid, chatGroup);
                }

                mMUCs.put(chatRoomJid, muc);

                try {
                    Form form = muc.getConfigurationForm();
                    Form submitForm = form.createAnswerForm();

                    for (FormField field : form.getFields()) {
                        if (!(field.getType() == FormField.Type.hidden) && field.getVariable() != null) {
                            submitForm.setDefaultAnswer(field.getVariable());
                        }
                    }

                    // Sets the new owner of the room
                    if (submitForm.getField("muc#roomconfig_roomowners") != null) {
                        List owners = new ArrayList();
                        owners.add(mUser.getAddress().getBareAddress());
                        submitForm.setAnswer("muc#roomconfig_roomowners", owners);
                    }

                    if (submitForm.getField("muc#roomconfig_roomname") != null)
                        submitForm.setAnswer("muc#roomconfig_roomname", subject);

                    if (submitForm.getField("muc#roomconfig_roomdesc") != null)
                        submitForm.setAnswer("muc#roomconfig_roomdesc", subject);

                    if (submitForm.getField("muc#roomconfig_changesubject") != null)
                        submitForm.setAnswer("muc#roomconfig_changesubject", true);

                    if (submitForm.getField("muc#roomconfig_anonymity") != null)
                        submitForm.setAnswer("muc#roomconfig_anonymity", "nonanonymous");

                    if (submitForm.getField("muc#roomconfig_publicroom") != null)
                        submitForm.setAnswer("muc#roomconfig_publicroom", false);

                    if (submitForm.getField("muc#roomconfig_persistentroom") != null)
                        submitForm.setAnswer("muc#roomconfig_persistentroom", true);

                    if (submitForm.getField("muc#roomconfig_whois") != null)
                        submitForm.setAnswer("muc#roomconfig_whois", Arrays.asList("anyone"));

//                      if (submitForm.getField("muc#roomconfig_historylength") != null)
//                          submitForm.setAnswer("muc#roomconfig_historylength", 0);

//                        if (submitForm.getField("muc#maxhistoryfetch") != null)
//                          submitForm.setAnswer("muc#maxhistoryfetch", 0);

                    if (submitForm.getField("muc#roomconfig_enablelogging") != null)
                        submitForm.setAnswer("muc#roomconfig_enablelogging", false);

//                        if (submitForm.getField("muc#maxhistoryfetch") != null)
//                            submitForm.setAnswer("muc#maxhistoryfetch", 0);

                    muc.sendConfigurationForm(submitForm);

                    if (TextUtils.isEmpty(muc.getSubject()))
                        muc.changeSubject(subject);
                    else
                        chatGroup.setName(muc.getSubject());

                } catch (XMPPException xe) {
                    debug(TAG, "(ignoring) got an error configuring MUC room: " + xe.getLocalizedMessage());

                }

                List<String> mucOccupant = muc.getOccupants();

                for (String occupantAddress : mucOccupant) {

                    Occupant occupant = muc.getOccupant(occupantAddress);
                    XmppAddress xa = new XmppAddress(occupant.getJid());
                    Contact mucContact = new Contact(xa,xa.getResource());
                    org.jivesoftware.smack.packet.Presence presence = muc.getOccupantPresence(occupant.getJid());
                    if (presence != null) {
                        ExtensionElement packetExtension = presence.getExtension("x", "vcard-temp:x:update");
                        if (packetExtension != null) {
                            DefaultExtensionElement o = (DefaultExtensionElement) packetExtension;
                            String hash = o.getValue("photo");
                            if (hash != null) {
                                boolean hasMatches = DatabaseUtils.doesAvatarHashExist(mContext.getContentResolver(), Imps.Avatars.CONTENT_URI, chatGroup.getAddress().getAddress(), hash);
                                if (!hasMatches) //we must reload
                                    qAvatar.push(chatGroup.getAddress().getAddress());
                            }
                            else
                            {
                                //no avatar, so update it since it will be small!
                                qAvatar.push(chatGroup.getAddress().getAddress());
                            }
                        }
                    }
                  //  Presence p = new Presence(parsePresence(presence), presence.getStatus(), null, null, Presence.CLIENT_TYPE_DEFAULT);
                  //  mucContact.setPresence(p);
                    chatGroup.addMemberAsync(mucContact);
                }


                addMucListeners(muc);

                return true;

            } catch (XMPPException e) {

                debug(TAG,"error creating MUC",e);
                return false;
            }


        }

        @Override
        public void deleteChatGroupAsync(ChatGroup group) {

            String chatRoomJid = group.getAddress().getAddress();

            if (mMUCs.containsKey(chatRoomJid))
            {
                MultiUserChat muc = mMUCs.get(chatRoomJid);

                try {
                    //muc.destroy("", null);

                    mMUCs.remove(chatRoomJid);

                } catch (Exception e) {
                    debug(TAG,"error destroying MUC",e);
                }

            }

        }

        @Override
        protected void addGroupMemberAsync(ChatGroup group, Contact contact) {

         //   inviteUserAsync(group, contact);
        //we already have invite, so... what is this?

        }

        @Override
        protected void removeGroupMemberAsync(ChatGroup group, Contact contact) {


            String chatRoomJid = group.getAddress().getAddress();

            if (mMUCs.containsKey(chatRoomJid))
            {
                MultiUserChat muc = mMUCs.get(chatRoomJid);
                try {
                    String reason = "";
                    muc.kickParticipant(contact.getName(),reason);
                  //  muc.kickParticipant(chatRoomJid, contact.getAddress().getBareAddress());
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }


        @Override
        public void joinChatGroupAsync(Address address, String subject) {

            String chatRoomJid = address.getBareAddress();
            String[] parts = chatRoomJid.split("@");
            String room = parts[0];
            String server = parts[1];
            String nickname = mUser.getName();//.split("@")[0];

            try {

                // Create a MultiUserChat using a Connection for a room

                MultiUserChatManager mucMgr = MultiUserChatManager.getInstanceFor(mConnection);

                MultiUserChat muc = mucMgr.getMultiUserChat(chatRoomJid);

                muc.join(nickname);

                if (TextUtils.isEmpty(subject))
                    subject = room;

                ChatGroup chatGroup = mGroups.get(chatRoomJid);

                if (chatGroup == null) {
                    chatGroup = new ChatGroup(address, subject, this);
                    mGroups.put(chatRoomJid, chatGroup);
                }

                mMUCs.put(chatRoomJid, muc);

                List<String> mucOccupant = muc.getOccupants();

                for (String occupant : mucOccupant) {
                    XmppAddress xa = new XmppAddress(occupant);
                    Contact mucContact = new Contact(xa,xa.getResource());
                    org.jivesoftware.smack.packet.Presence presence = muc.getOccupantPresence(occupant);
                    Presence p = new Presence(parsePresence(presence), null, null, null, Presence.CLIENT_TYPE_MOBILE);
                    mucContact.setPresence(p);
                    chatGroup.addMemberAsync(mucContact);
                }

                addMucListeners(muc);



            } catch (Exception e) {
                debug(TAG,"error joining MUC",e);
            }

        }

        private void addMucListeners (MultiUserChat muc)
        {

            muc.addSubjectUpdatedListener(new SubjectUpdatedListener() {

                @Override
                public void subjectUpdated(String subject, String from) {

                    XmppAddress xa = new XmppAddress(from);
                    MultiUserChat muc = mChatGroupManager.getMultiUserChat(xa.getBareAddress());
                    ChatGroup chatGroup = mChatGroupManager.getChatGroup(xa);
                    chatGroup.setName(subject);

                }

            });

            muc.addParticipantStatusListener(new ParticipantStatusListener() {
                @Override
                public void joined(String participant) {

                    XmppAddress xa = new XmppAddress(participant);
                    MultiUserChat muc = mChatGroupManager.getMultiUserChat(xa.getBareAddress());
                    ChatGroup chatGroup = mChatGroupManager.getChatGroup(xa);
                    Contact mucContact = new Contact(xa, xa.getResource());
                    Presence p = new Presence(Imps.Presence.AVAILABLE, null, null, null, Presence.CLIENT_TYPE_MOBILE);
                    org.jivesoftware.smack.packet.Presence presence = muc.getOccupantPresence(participant);
                    if (presence != null) {
                        ExtensionElement packetExtension = presence.getExtension("x", "vcard-temp:x:update");
                        if (packetExtension != null) {
                            DefaultExtensionElement o = (DefaultExtensionElement) packetExtension;
                            String hash = o.getValue("photo");
                            if (hash != null) {
                                boolean hasMatches = DatabaseUtils.doesAvatarHashExist(mContext.getContentResolver(), Imps.Avatars.CONTENT_URI, chatGroup.getAddress().getAddress(), hash);
                                if (!hasMatches) //we must reload
                                    qAvatar.push(chatGroup.getAddress().getAddress());
                            }
                        }
                    }
                    mucContact.setPresence(p);
                    chatGroup.addMemberAsync(mucContact);

                }

                @Override
                public void left(String participant) {

                    XmppAddress xa = new XmppAddress(participant);
                    MultiUserChat muc = mChatGroupManager.getMultiUserChat(xa.getBareAddress());
                    ChatGroup chatGroup = mChatGroupManager.getChatGroup(xa);
                    Contact mucContact = new Contact(xa, xa.getResource());
                    chatGroup.removeMemberAsync(mucContact);

                }

                @Override
                public void kicked(String participant, String actor, String reason) {

                }

                @Override
                public void voiceGranted(String participant) {

                }

                @Override
                public void voiceRevoked(String participant) {

                }

                @Override
                public void banned(String participant, String actor, String reason) {

                }

                @Override
                public void membershipGranted(String participant) {

                }

                @Override
                public void membershipRevoked(String participant) {

                }

                @Override
                public void moderatorGranted(String participant) {

                }

                @Override
                public void moderatorRevoked(String participant) {

                }

                @Override
                public void ownershipGranted(String participant) {

                }

                @Override
                public void ownershipRevoked(String participant) {

                }

                @Override
                public void adminGranted(String participant) {

                }

                @Override
                public void adminRevoked(String participant) {

                }

                @Override
                public void nicknameChanged(String participant, String newNickname) {
                   // XmppAddress xa = new XmppAddress(participant);
                   // Contact mucContact = new Contact(xa, xa.getResource());
                   // mucContact.setName(newNickname);
                }
            });

            muc.addParticipantListener(new PresenceListener() {
                @Override
                public void processPresence(org.jivesoftware.smack.packet.Presence presence) {

                    XmppAddress xa = new XmppAddress(presence.getFrom());
                    Contact mucContact = new Contact(xa, xa.getResource());
                    Presence p = new Presence(parsePresence(presence), presence.getStatus(), null, null, Presence.CLIENT_TYPE_DEFAULT);
                    mucContact.setPresence(p);

                }
            });

        }

        @Override
        public void leaveChatGroupAsync(ChatGroup group) {
            String chatRoomJid = group.getAddress().getBareAddress();

            if (mMUCs.containsKey(chatRoomJid))
            {
                MultiUserChat muc = mMUCs.get(chatRoomJid);
                try {
                    muc.leave();
                }
                catch (SmackException.NotConnectedException nce)
                {
                    Log.e(ImApp.LOG_TAG,"not connected error trying to leave group",nce);

                }

                mMUCs.remove(chatRoomJid);

            }

        }

        @Override
        public void inviteUserAsync(final ChatGroup group, final Contact invitee) {

            execute(new Runnable () {

                public void run() {
                    String chatRoomJid = group.getAddress().getAddress();

                    if (mMUCs.containsKey(chatRoomJid)) {
                        MultiUserChat muc = mMUCs.get(chatRoomJid);

                        String reason = group.getName(); //no reason for now
                        try {
                            muc.invite(invitee.getAddress().getAddress(), reason);
                            muc.grantMembership(invitee.getAddress().getAddress());
                        } catch (Exception nce) {
                            Log.e(ImApp.LOG_TAG, "not connected error trying to add invite", nce);

                        }

                    }
                }
            });

        }

        @Override
        public void acceptInvitationAsync(Invitation invitation) {

            Address addressGroup = invitation.getGroupAddress();

            joinChatGroupAsync (addressGroup,invitation.getReason());

        }

        @Override
        public void rejectInvitationAsync(Invitation invitation) {

            Address addressGroup = invitation.getGroupAddress();

            String reason = ""; // no reason for now

            MultiUserChatManager mucMgr = MultiUserChatManager.getInstanceFor(mConnection);
            try {

                    mucMgr.decline(addressGroup.getAddress(), invitation.getSender().getAddress(), reason);

                }
                catch (SmackException.NotConnectedException nce)
                {
                    Log.e(ImApp.LOG_TAG,"not connected error trying to reject invite",nce);
                }
        }

    };

    @Override
    public synchronized ChatSessionManager getChatSessionManager() {

        if (mSessionManager == null)
            mSessionManager = new XmppChatSessionManager();

        return mSessionManager;
    }

    @Override
    public synchronized XmppContactListManager getContactListManager() {

        if (mContactListManager == null)
            mContactListManager = new XmppContactListManager();

        return mContactListManager;
    }

    @Override
    public Contact getLoginUser() {
        return mUser;
    }

    @Override
    public Map<String, String> getSessionContext() {
        // Empty state for now (but must have at least one key)
        return Collections.singletonMap("state", "empty");
    }

    @Override
    public int[] getSupportedPresenceStatus() {
        return new int[] { Presence.AVAILABLE, Presence.AWAY, Presence.IDLE, Presence.OFFLINE,
                           Presence.DO_NOT_DISTURB, };
    }

    @Override
    public boolean isUsingTor() {
        return mUseTor;
    }

    @Override
    public void loginAsync(long accountId, String passwordTemp, long providerId, boolean retry) {

        mAccountId = accountId;
        mPassword = passwordTemp;
        mProviderId = providerId;
        mRetryLogin = retry;

        ContentResolver contentResolver = mContext.getContentResolver();

        if (mPassword == null)
            mPassword = Imps.Account.getPassword(contentResolver, mAccountId);

        mIsGoogleAuth = false;// mPassword.startsWith(GTalkOAuth2.NAME);

        if (mIsGoogleAuth)
        {
            mPassword = mPassword.split(":")[1];
        }

        Cursor cursor = contentResolver.query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(mProviderId)}, null);

        if (cursor == null)
            return;

        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                cursor, contentResolver, mProviderId, false, null);

        if (mUser == null)
            mUser = makeUser(providerSettings, contentResolver);

        providerSettings.close();

        execute(new Runnable() {
            @Override
            public void run() {
                do_login();
            }
        });
    }

    private void loginSync(long accountId, String passwordTemp, long providerId, boolean retry) {

        mAccountId = accountId;
        mPassword = passwordTemp;
        mProviderId = providerId;
        mRetryLogin = retry;

        ContentResolver contentResolver = mContext.getContentResolver();

        if (mPassword == null)
            mPassword = Imps.Account.getPassword(contentResolver, mAccountId);

        mIsGoogleAuth = false;// mPassword.startsWith(GTalkOAuth2.NAME);

        if (mIsGoogleAuth)
        {
            mPassword = mPassword.split(":")[1];
        }

        Cursor cursor = contentResolver.query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(mProviderId)}, null);

        if (cursor == null)
            return;

        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                cursor, contentResolver, mProviderId, false, null);

        if (mUser == null)
            mUser = makeUser(providerSettings, contentResolver);

        providerSettings.close();

        do_login();


    }

    // Runs in executor thread
    private void do_login() {

        /*
        if (mConnection != null) {
            setState(getState(), new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER,
                    "still trying..."));
            return;
        }*/

        ContentResolver contentResolver = mContext.getContentResolver();

        Cursor cursor = contentResolver.query(Imps.ProviderSettings.CONTENT_URI, new String[]{Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE}, Imps.ProviderSettings.PROVIDER + "=?", new String[]{Long.toString(mProviderId)}, null);

        if (cursor == null)
            return; //not going to work

        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                cursor, contentResolver, mProviderId, false, null);


        // providerSettings is closed in initConnection();
        String userName = Imps.Account.getUserName(contentResolver, mAccountId);

        String defaultStatus = null;

        mNeedReconnect = true;
        setState(LOGGING_IN, null);

        mUserPresence = new Presence(Presence.AVAILABLE, defaultStatus, Presence.CLIENT_TYPE_MOBILE);

        try {
            if (userName == null || userName.length() == 0)
                throw new Exception("empty username not allowed");

            initConnectionAndLogin(providerSettings, userName);

            setState(LOGGED_IN, null);
            debug(TAG, "logged in");
            mNeedReconnect = false;


        } catch (XMPPException e) {
            debug(TAG, "exception thrown on connection",e);


            ImErrorInfo info = new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER, e.getMessage());
            mRetryLogin = true; // our default behavior is to retry

            if (mConnection != null && mConnection.isConnected() && (!mConnection.isAuthenticated())) {

                if (mIsGoogleAuth)
                {
                    debug (TAG, "google failed; may need to refresh");

                    String newPassword = null;//refreshGoogleToken (userName, mPassword,providerSettings.getDomain());

                    if (newPassword != null)
                        mPassword = newPassword;

                    mRetryLogin = true;


                }
                else
                {
                    debug(TAG, "not authorized - will not retry");
                    info = new ImErrorInfo(ImErrorInfo.INVALID_USERNAME, "invalid user/password");
                    setState(SUSPENDED, info);
                    mRetryLogin = false;
                    mNeedReconnect = false;

                }
            }


            if (mRetryLogin && getState() != SUSPENDED) {
                debug(TAG, "will retry");
                setState(LOGGING_IN, info);
                maybe_reconnect();

            } else {
               //debug(TAG, "will not retry"); //WE MUST ALWAYS RETRY!
               // disconnect();
               // disconnected(info);
            }


        } catch (Exception e) {

            debug(TAG, "login failed",e);
            mRetryLogin = true;
            mNeedReconnect = true;

            debug(TAG, "will retry");
            ImErrorInfo info = new ImErrorInfo(ImErrorInfo.UNKNOWN_ERROR, "keymanagement exception");
            setState(LOGGING_IN, info);

        }
        finally {
            providerSettings.close();
            
            if (!cursor.isClosed())
                cursor.close();
        }

    }

    /**
    private String refreshGoogleToken (String userName, String expiredToken, String domain)
    {
        
        //invalidate our old one, that is locally cached
        android.accounts.AccountManager.get(mContext.getApplicationContext()).invalidateAuthToken("com.google", expiredToken);
        //request a new one
        String newToken = null;//GTalkOAuth2.getGoogleAuthToken(userName + '@' + domain, mContext.getApplicationContext());

        if (newToken != null)
        {
            //now store the new one, for future use until it expires
        ///    ImApp.insertOrUpdateAccount(mContext.getContentResolver(), mProviderId, userName,
           //         GTalkOAuth2.NAME + ':' + newToken );
        }

        return newToken;

    }*/

    // TODO shouldn't setProxy be handled in Imps/settings?
    public void setProxy(String type, String host, int port) {
        if (type == null) {
            mProxyInfo = ProxyInfo.forNoProxy();
        } else {

            ProxyInfo.ProxyType pType = ProxyInfo.ProxyType.valueOf(type);
            String username = null;
            String password = null;

            if (type.equals(TorProxyInfo.PROXY_TYPE) //socks5
                    && host.equals(TorProxyInfo.PROXY_HOST) //127.0.0.1
                    && port == TorProxyInfo.PROXY_PORT) //9050
            {
                //if the proxy is for Orbot/Tor then generate random usr/pwd to isolate Tor streams
                if (rndForTorCircuits == null)
                    rndForTorCircuits = new SecureRandom();

                username = rndForTorCircuits.nextInt(100000)+"";
                password = rndForTorCircuits.nextInt(100000)+"";

            }

            mProxyInfo = new ProxyInfo(pType, host, port, username, password);

        }
    }

    /**
    public void initConnection(XMPPTCPConnection connection, Contact user, int state) {
        mConnection = connection;
        mRoster = Roster.getInstanceFor(mConnection);
        mRoster.setRosterLoadedAtLogin(true);
        mUser = user;
        setState(state, null);
    }*/

    private void initConnectionAndLogin (Imps.ProviderSettings.QueryMap providerSettings,String userName) throws IOException, SmackException, XMPPException, KeyManagementException, NoSuchAlgorithmException, IllegalStateException, RuntimeException
    {
        Roster.SubscriptionMode subMode = Roster.SubscriptionMode.manual;//Roster.SubscriptionMode.accept_all;//load this from a preference

        Debug.onConnectionStart(); //only activates if Debug TRUE is set, so you can leave this in!

        initConnection(providerSettings, userName);

        //disable compression based on statement by Ge0rg
        // mConfig.setCompressionEnabled(false);

        if (mConnection.isConnected() && mConnection.isSecureConnection())
        {

            mResource = providerSettings.getXmppResource();

            mRoster = Roster.getInstanceFor(mConnection);
            mRoster.setRosterLoadedAtLogin(true);
            mRoster.setSubscriptionMode(subMode);

            mChatManager = ChatManager.getInstanceFor(mConnection);

            mPingManager = PingManager.getInstanceFor(mConnection) ;

            mConnection.login(mUsername, mPassword, mResource);
            
            String fullJid = mConnection.getUser();
            XmppAddress xa = new XmppAddress(fullJid);

            if (mUser == null)
                mUser = makeUser(providerSettings,mContext.getContentResolver());

            mStreamHandler.notifyInitialLogin();
            initServiceDiscovery();

            sendPresencePacket();

//            getContactListManager().listenToRoster(mRoster);

            MultiUserChatManager.getInstanceFor(mConnection).addInvitationListener(new InvitationListener() {
                @Override
                public void invitationReceived(XMPPConnection conn, MultiUserChat muc, String inviter, String reason, String password, org.jivesoftware.smack.packet.Message message) {

                    getChatGroupManager().acceptInvitationAsync(muc.getRoom());
                    XmppAddress xa = new XmppAddress(muc.getRoom());

                    mChatGroupManager.joinChatGroupAsync(xa,reason);

                    ChatSession session = mSessionManager.findSession(xa.getAddress());

                    //create a session
                    if (session == null) {
                        ImEntity participant = findOrCreateParticipant(xa.getBareAddress(), true);

                        if (participant != null)
                            session = mSessionManager.createChatSession(participant,false);

                        if (session != null)
                            ((ChatGroup)session.getParticipant()).setName(reason);
                    }





                }


            });

            execute(new Runnable ()
            {
                public void run ()
                {
                    sendVCard();
                }
            });

        }
        else
        {
            //throw some meaningful error message here
            throw new SmackException("Unable to securely conenct to server");
        }

        

    }

    private void sendVCard ()
    {

        try {
            String jid = mUser.getAddress().getBareAddress();

            VCardManager vCardManager = VCardManager.getInstanceFor(mConnection);
            VCard vCard = null;

            try {
                vCard = vCardManager.loadVCard(jid);
            }
            catch (Exception e){
                debug(TAG,"error loading vcard",e);

            }

            boolean setAvatar = true;

            if (vCard == null) {
                vCard = new VCard();
                vCard.setJabberId(jid);
                setAvatar = true;
            }
            else if (vCard.getAvatarHash() != null)
            {
                setAvatar = !DatabaseUtils.doesAvatarHashExist(mContext.getContentResolver(),  Imps.Avatars.CONTENT_URI, mUser.getAddress().getBareAddress(), vCard.getAvatarHash());

            }

            vCard.setNickName(mUser.getName());

            if (setAvatar) {
                byte[] avatar = DatabaseUtils.getAvatarBytesFromAddress(mContext.getContentResolver(), mUser.getAddress().getBareAddress(), ImApp.DEFAULT_AVATAR_WIDTH, ImApp.DEFAULT_AVATAR_HEIGHT);
                if (avatar != null) {
                    vCard.setAvatar(avatar, "image/jpeg");
                }
            }

            if (mConnection != null && mConnection.isConnected() && mConnection.isAuthenticated()) {
                debug(TAG, "Saving VCard for: " + mUser.getAddress().getAddress());
                vCardManager.saveVCard(vCard);
            }
        }
        catch (Exception e)
        {
            debug(TAG,"error saving vcard",e);
        }
    }

    // Runs in executor thread
    private void initConnection(Imps.ProviderSettings.QueryMap providerSettings, String userName) throws NoSuchAlgorithmException, KeyManagementException, XMPPException, SmackException, IOException  {

        boolean allowPlainAuth = false;//never! // providerSettings.getAllowPlainAuth();
        boolean requireTls = true;// providerSettings.getRequireTls(); //always!
        boolean doDnsSrv = providerSettings.getDoDnsSrv();
       // boolean tlsCertVerify = providerSettings.getTlsCertVerify();

       // boolean useSASL = true;//!allowPlainAuth;
        boolean useTor = providerSettings.getUseTor();
        String domain = providerSettings.getDomain();

        mPriority = providerSettings.getXmppResourcePrio();
        int serverPort = providerSettings.getPort();
        String server = providerSettings.getServer();
        if ("".equals(server))
            server = null;

        /**
         * //need to move this to the new NetCipher BroadcastReceiver API
        try {
            //if Orbot is on and running, we should use it
            if (OrbotHelper.isOrbotInstalled(mContext) && OrbotHelper.isOrbotRunning(mContext)
                    && (server != null && (!doDnsSrv)))
                useTor = true;
        }
        catch (Exception e)
        {
            debug(TAG,"There was an error checking Orbot: " + e.getMessage());
        }*/

        debug(TAG, "TLS required? " + requireTls);

        if (useTor) {
            setProxy(TorProxyInfo.PROXY_TYPE, TorProxyInfo.PROXY_HOST,
                    TorProxyInfo.PROXY_PORT);
        }
        else
        {
            setProxy(null, null, -1);
        }

        if (mProxyInfo == null)
            mProxyInfo = ProxyInfo.forNoProxy();

        // If user did not specify a server, and SRV requested then lookup SRV
        if (doDnsSrv) {

            //java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
            //java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");

            debug(TAG, "(DNS SRV) resolving: " + domain);
            List<HostAddress> listHosts = DNSUtil.resolveXMPPDomain(domain, null);
            server = listHosts.get(0).getFQDN();
            serverPort = listHosts.get(0).getPort();

            debug(TAG, "(DNS SRV) resolved: " + domain + "=" + server + ":" + serverPort);


        }

        if (server != null && server.contains("google.com"))
        {
            mUsername = userName + '@' + domain;
        }
        else if (domain.contains("gmail.com"))
        {
            mUsername = userName + '@' + domain;
        }
        else if (mIsGoogleAuth)
        {
            mUsername = userName + '@' + domain;
        }
        else
        {
            mUsername = userName;
        }


        if (serverPort == 0) //if serverPort is set to 0 then use 5222 as default
            serverPort = 5222;

        mConfig = XMPPTCPConnectionConfiguration.builder();

        mConfig.setServiceName(domain);
        mConfig.setPort(serverPort);

        // No server requested and SRV lookup wasn't requested or returned nothing - use domain
        if (server == null)
            mConfig.setHost(domain);
        else
            mConfig.setHost(server);

        mConfig.setDebuggerEnabled(Debug.DEBUG_ENABLED);

        //mConfig.setSASLAuthenticationEnabled(useSASL);


        // Android has no support for Kerberos or GSSAPI, so disable completely
        SASLAuthentication.unregisterSASLMechanism("KERBEROS_V4");
        SASLAuthentication.unregisterSASLMechanism("GSSAPI");

        /**
        SASLAuthentication.registerSASLMechanism( GTalkOAuth2.NAME, GTalkOAuth2.class );

        if (mIsGoogleAuth) //if using google auth enable sasl
            SASLAuthentication.supportSASLMechanism( GTalkOAuth2.NAME, 0);
        else if (domain.contains("google.com")||domain.contains("gmail.com")) //if not google auth, disable if doing direct google auth
            SASLAuthentication.unsupportSASLMechanism( GTalkOAuth2.NAME);
            */

        if (allowPlainAuth)
            SASLAuthentication.unBlacklistSASLMechanism("PLAIN");

        SASLAuthentication.unBlacklistSASLMechanism("DIGEST-MD5");

        if (mMemTrust == null)
            mMemTrust = new MemorizingTrustManager(mContext);

        if (sslContext == null)
        {

            sslContext = SSLContext.getInstance(SSLCONTEXT_TYPE);

            SecureRandom secureRandom = new java.security.SecureRandom();
             sslContext.init(null, MemorizingTrustManager.getInstanceList(mContext), secureRandom);

            if (Build.VERSION.SDK_INT >= 20) {

                sslContext.getDefaultSSLParameters().setCipherSuites(XMPPCertPins.SSL_IDEAL_CIPHER_SUITES_API_20);

            }
            else
            {
                sslContext.getDefaultSSLParameters().setCipherSuites(XMPPCertPins.SSL_IDEAL_CIPHER_SUITES);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mConfig.setKeystoreType("AndroidCAStore");
                mConfig.setKeystorePath(null);
            } else {
                mConfig.setKeystoreType("BKS");
                String path = System.getProperty("javax.net.ssl.trustStore");
                if (path == null)
                    path = System.getProperty("java.home") + File.separator + "etc"
                            + File.separator + "security" + File.separator
                            + "cacerts.bks";
                mConfig.setKeystorePath(path);
            }


        }


        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= 16){
            // Enable TLS1.2 and TLS1.1 on supported versions of android
            // http://stackoverflow.com/questions/16531807/android-client-server-on-tls-v1-2

            mConfig.setEnabledSSLProtocols(new String[] { "TLSv1.2", "TLSv1.1", "TLSv1" });
            sslContext.getDefaultSSLParameters().setProtocols(new String[] { "TLSv1.2", "TLSv1.1", "TLSv1" });

        }

        if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH){
            mConfig.setEnabledSSLCiphers(XMPPCertPins.SSL_IDEAL_CIPHER_SUITES);
        }

        mConfig.setCustomSSLContext(sslContext);
        mConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
        mConfig.setHostnameVerifier(
                mMemTrust.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()));


        mConfig.setSendPresence(true);

        XMPPTCPConnection.setUseStreamManagementDefault(true);

        mConnection = new XMPPTCPConnection(mConfig.build());


        //debug(TAG,"is secure connection? " + mConnection.isSecureConnection());
        //debug(TAG,"is using TLS? " + mConnection.isUsingTLS());

        mConnection.addAsyncStanzaListener(new StanzaListener() {

            @Override
            public void processPacket(Stanza stanza) {

                debug(TAG, "receive message: " + stanza.getFrom() + " to " + stanza.getTo());

                org.jivesoftware.smack.packet.Message smackMessage = (org.jivesoftware.smack.packet.Message) stanza;

                handleMessage(smackMessage);

                String msg_xml = smackMessage.toXML().toString();

                try {
                    handleChatState(smackMessage.getFrom(), msg_xml);
                }
                catch (RemoteException re)
                {
                    //no worries
                }
            }
        }, new StanzaTypeFilter(org.jivesoftware.smack.packet.Message.class));

        mConnection.addAsyncStanzaListener(new StanzaListener() {

            @Override
            public void processPacket(Stanza packet) {

                org.jivesoftware.smack.packet.Presence presence = (org.jivesoftware.smack.packet.Presence) packet;
                qPresence.push(presence);

            }
        }, new StanzaTypeFilter(org.jivesoftware.smack.packet.Presence.class));

        if (mTimerPackets == null)
            initPacketProcessor();
        
        if (mTimerPresence == null)
            initPresenceProcessor ();
        
        ConnectionListener connectionListener = new ConnectionListener() {
            /**
             * Called from smack when connect() is fully successful
             *
             * This is called on the executor thread while we are in reconnect()
             */
            @Override
            public void reconnectionSuccessful() {
                if (mStreamHandler == null || !mStreamHandler.isResumePending()) {
                    debug(TAG, "Reconnection success");
                    onReconnectionSuccessful();
                    mRoster = Roster.getInstanceFor(mConnection);
                } else {
                    debug(TAG, "Ignoring reconnection callback due to pending resume");
                }
            }

            @Override
            public void reconnectionFailed(Exception e) {
                // We are not using the reconnection manager
             //   throw new UnsupportedOperationException();
                execute(new Runnable() {

                    public void run() {

                        mNeedReconnect = true;
                        setState(LOGGING_IN,
                                new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network error"));
                        reconnect();

                    }

                });
            }

            @Override
            public void reconnectingIn(int seconds) {
                // // We are not using the reconnection manager
                // throw new UnsupportedOperationException();
            }

            @Override
            public void connectionClosedOnError(final Exception e) {
                /*
                 * This fires when:
                 * - Packet reader or writer detect an error
                 * - Stream compression failed
                 * - TLS fails but is required
                 * - Network error
                 * - We forced a socket shutdown
                 */
                debug(TAG, "reconnect on error: " + e.getMessage());
                if (e.getMessage().contains("conflict")) {


                    execute(new Runnable() {
                        @Override
                        public void run() {
                           // disconnect();
                            disconnected(new ImErrorInfo(ImpsErrorInfo.ALREADY_LOGGED,
                                  "logged in from another location"));
                        }
                    });

                } else if (!mNeedReconnect) {

                    execute(new Runnable() {

                        public void run() {
                            if (getState() == LOGGED_IN)
                            {
                                mNeedReconnect = true;
                                setState(LOGGING_IN,
                                      new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network error"));
                                reconnect();
                            }
                        }

                    });


                }
            }

            @Override
            public void connected(XMPPConnection connection) {
                debug(TAG, "connected");
            }

            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                debug(TAG, "authenticated");

            }

            @Override
            public void connectionClosed() {

                debug(TAG, "connection closed");

                /*
                 * This can be called in these cases:
                 * - Connection is shutting down
                 *   - because we are calling disconnect
                 *     - in do_logout
                 *
                 * - NOT
                 *   - because server disconnected "normally"
                 *   - we were trying to log in (initConnection), but are failing
                 *   - due to network error
                 *   - due to login failing
                 */

                //if the state is logged in, we should try to reconnect!
                if (getState() == LOGGED_IN)
                {
                    execute(new Runnable() {

                        public void run() {

                            mNeedReconnect = true;
                            setState(LOGGING_IN,
                                    new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network error"));
                            reconnect();

                        }

                    });
                }
            }
        };

        mConnection.addConnectionListener(connectionListener);
        mStreamHandler = new XmppStreamHandler(mConnection, connectionListener);
        Exception xmppConnectException = null;
        AbstractXMPPConnection conn = mConnection.connect();

    }

    private void handleMessage (org.jivesoftware.smack.packet.Message smackMessage) {

        String body = smackMessage.getBody();
        boolean isGroupMessage = smackMessage.getType() == org.jivesoftware.smack.packet.Message.Type.groupchat;

        if (smackMessage.getError() != null) {
            //  smackMessage.getError().getCode();
            String error = "Error " + smackMessage.getError() + " (" + smackMessage.getError().getCondition() + "): " + smackMessage.getError().getConditionText();
            debug(TAG, error);
            return;
        }


        if (body == null) {
            Collection<org.jivesoftware.smack.packet.Message.Body> mColl = smackMessage.getBodies();
            for (org.jivesoftware.smack.packet.Message.Body bodyPart : mColl) {
                String msg = bodyPart.getMessage();
                if (msg != null) {
                    body = msg;
                    break;
                }
            }

        }

        ChatSession session = findOrCreateSession(smackMessage.getFrom(), isGroupMessage);

        if (session != null) //not subscribed so don't do anything
        {

            DeliveryReceipt drIncoming = (DeliveryReceipt) smackMessage.getExtension("received", DeliveryReceipt.NAMESPACE);

            if (drIncoming != null) {

                debug(TAG, "got delivery receipt for " + drIncoming.getId());

                if (session != null)
                    session.onMessageReceipt(drIncoming.getId());

            }

            if (body != null && session != null) {

                Message rec = new Message(body);

                rec.setTo(new XmppAddress(smackMessage.getTo()));
                rec.setFrom(new XmppAddress(smackMessage.getFrom()));
                rec.setDateTime(new Date());

                rec.setID(smackMessage.getStanzaId());

                rec.setType(Imps.MessageType.INCOMING);

                // Detect if this was said by us, and mark message as outgoing
                if (isGroupMessage) {

                    if (TextUtils.isEmpty(rec.getFrom().getResource()))
                    {
                        return; //do nothing if there is no resource since that is a system message
                    }
                    else if (rec.getFrom().getResource().equals(rec.getTo().getUser())) {
                        //rec.setType(Imps.MessageType.OUTGOING);
                        Occupant oc = mChatGroupManager.getMultiUserChat(rec.getFrom().getBareAddress()).getOccupant(rec.getFrom().getAddress());
                        if (oc != null && oc.getJid().equals(mUser.getAddress().getAddress()))
                            return; //do nothing if it is from us
                    }

                }

                setPresence(smackMessage.getFrom(),Presence.AVAILABLE);

                boolean good = session.onReceiveMessage(rec);

                if (smackMessage.getExtension("request", DeliveryReceipt.NAMESPACE) != null) {
                    if (good) {
                        debug(TAG, "sending delivery receipt");
                        // got XEP-0184 request, send receipt
                        sendReceipt(smackMessage);
                        session.onReceiptsExpected(true);
                    } else {
                        debug(TAG, "not sending delivery receipt due to processing error");
                    }

                } else {
                    //no request for delivery receipt

                    session.onReceiptsExpected(false);
                }

            }


        }
    }

    private void sendPresencePacket() {        
        qPacket.add(makePresencePacket(mUserPresence));        
    }

    private void sendReceipt(org.jivesoftware.smack.packet.Message msg) {
        debug(TAG, "sending XEP-0184 ack to " + msg.getFrom() + " id=" + msg.getPacketID());
        org.jivesoftware.smack.packet.Message ack = new org.jivesoftware.smack.packet.Message(
                msg.getFrom(), msg.getType());
        ack.addExtension(new DeliveryReceipt(msg.getStanzaId()));
        sendPacket(ack);
    }



    public X509TrustManager getDummyTrustManager ()
    {

        return new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };


    }


    protected int parsePresence(org.jivesoftware.smack.packet.Presence presence) {

        int type = Imps.Presence.AVAILABLE;
        org.jivesoftware.smack.packet.Presence.Mode rmode = presence.getMode();
        org.jivesoftware.smack.packet.Presence.Type rtype = presence.getType();

        if (rmode == org.jivesoftware.smack.packet.Presence.Mode.away || rmode == org.jivesoftware.smack.packet.Presence.Mode.xa)
            type = Imps.Presence.AWAY;
        else if (rmode == org.jivesoftware.smack.packet.Presence.Mode.dnd)
            type = Imps.Presence.DO_NOT_DISTURB;
        else if (rtype == org.jivesoftware.smack.packet.Presence.Type.unavailable || rtype ==  org.jivesoftware.smack.packet.Presence.Type.error)
            type = Imps.Presence.OFFLINE;
        else if (rtype ==  org.jivesoftware.smack.packet.Presence.Type.unsubscribed)
            type = Imps.Presence.OFFLINE;
        
        return type;
    }

    // We must release resources here, because we will not be reused
    void disconnected(ImErrorInfo info) {
        debug(TAG, "disconnected");
        join();
        setState(DISCONNECTED, info);
    }


    @Override
    public void logoutAsync() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                do_logout();
            }
        }).start();

    }

    // Force immediate logout
    public void logout() {
        logoutAsync();
    }

    // Usually runs in executor thread, unless called from logout()
    private void do_logout() {
        setState(LOGGING_OUT, null);
        disconnect();
        disconnected(null);
    }

    // Runs in executor thread
    private void disconnect() {

        clearPing();

        try {
            mConnection.disconnect();
        } catch (Throwable th) {
            // ignore
        }
        mConnection = null;
        mNeedReconnect = false;
        mRetryLogin = false;
    }

    @Override
    public void reestablishSessionAsync(Map<String, String> sessionContext) {
        execute(new Runnable() {
            @Override
            public void run() {
                if (getState() == SUSPENDED) {
                    debug(TAG, "reestablish");
                    mNeedReconnect = false;
                    setState(LOGGING_IN, null);
                    maybe_reconnect();
                }
            }
        });
    }

    @Override
    public void suspend() {
        execute(new Runnable() {
            @Override
            public void run() {
                debug(TAG, "suspend");
                setState(SUSPENDED, null);
                mNeedReconnect = false;
                clearPing();
                // Do not try to reconnect anymore if we were asked to suspend

                if (mStreamHandler != null)
                    mStreamHandler.quickShutdown();

            }
        });
    }

    private ChatSession findOrCreateSession(String address, boolean groupChat) {

        ChatSession session = mSessionManager.findSession(XmppAddress.stripResource(address));

        //create a session if this it not groupchat
        if (session == null && (!groupChat)) {
            ImEntity participant = findOrCreateParticipant(address, groupChat);

            if (participant != null) {
                session = mSessionManager.createChatSession(participant, false);

                /**
                ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
                Chat chat= chatManager.createChat(participant.getAddress().getAddress(), new ChatStateListener() {
                    @Override
                    public void stateChanged(Chat chat, ChatState state) {
                        switch (state){
                            case active:
                                Log.d("state","active");
                                break;
                            case composing:
                                Log.d("state","composing");
                                break;
                            case paused:
                                Log.d("state","paused");
                                break;
                            case inactive:
                                Log.d("state","inactive");
                                break;
                            case gone:
                                Log.d("state","gone");
                                break;
                        }
                    }

                    @Override
                    public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {
                        Log.d("processMessage","processMessage");
                    }
                });**/

            }

        }

        /**
        try {
            if (session != null)
                session.setSubscribed(!mContactListManager.isBlocked(address));
        }
        catch (ImException ime)
        {
            ime.printStackTrace();
        }*/

        return session;
    }

    synchronized ImEntity findOrCreateParticipant(String address, boolean isGroupChat) {
        ImEntity participant = null;

        if (isGroupChat) {
            Address xmppAddress = new XmppAddress(XmppAddress.stripResource(address));
            participant = mChatGroupManager.getChatGroup(xmppAddress);

            if (participant == null) {
                try {
                    mChatGroupManager.createChatGroupAsync(address, xmppAddress.getUser(), mUser.getName());
                    participant = mChatGroupManager.getChatGroup(xmppAddress);
                } catch (Exception e) {
                    Log.w(TAG, "unable to join group chat: " + e.toString());
                    return null;
                }
            }
        } else
        {
           return mContactListManager.getContact(address);

        }

        return participant;
    }

    Contact findOrCreateContact(String address) {
        return (Contact) findOrCreateParticipant(address, false);
    }

    private Contact makeContact(String address) {

        Contact contact = null;

        //load from roster if we don't have the contact
        RosterEntry rEntry = null;

        if (mConnection != null)
            rEntry = mRoster.getEntry(address);

        if (rEntry != null)
        {
            XmppAddress xAddress = new XmppAddress(address);

            String name = rEntry.getName();
            if (name == null)
                name = xAddress.getUser();

            contact = new Contact(xAddress, name);
        }
        else
        {
            XmppAddress xAddress = new XmppAddress(address);

            contact = new Contact(xAddress, xAddress.getUser());
        }

        return contact;
    }

    private final class XmppChatSessionManager extends ChatSessionManager {
        @Override
        public void sendMessageAsync(ChatSession session, Message message) {

            MultiUserChat muc = ((XmppChatGroupManager)getChatGroupManager()).getMultiUserChat(message.getTo().getAddress());

            org.jivesoftware.smack.packet.Message msgXmpp = null;

            if (muc != null)
            {
                msgXmpp = muc.createMessage();
            }
            else
            {
                msgXmpp = new org.jivesoftware.smack.packet.Message(
                        message.getTo().getAddress(), org.jivesoftware.smack.packet.Message.Type.chat);
                msgXmpp.addExtension(new DeliveryReceiptRequest());
                
                Contact contact = mContactListManager.getContact(message.getTo().getBareAddress());
                
                if (contact != null && contact.getPresence() !=null && (!contact.getPresence().isOnline()))
                    requestPresenceRefresh(message.getTo().getBareAddress());
                
            }
            
            if (message.getFrom() == null)
                msgXmpp.setFrom(mUser.getAddress().getAddress());
            else
                msgXmpp.setFrom(message.getFrom().getAddress());
            
            msgXmpp.setBody(message.getBody());

            if (message.getID() != null)
                msgXmpp.setStanzaId(message.getID());
            else
                message.setID(msgXmpp.getStanzaId());
            
            sendPacket(msgXmpp);            

        }

        ChatSession findSession(String address) {

            ChatSession result = mSessions.get(address);

         //   if (result == null)
           //     result = mSessions.get(XmppAddress.stripResource(address));

            return result;
        }

        @Override
        public ChatSession createChatSession(ImEntity participant, boolean isNewSession) {

          //  requestPresenceRefresh(participant.getAddress().getAddress());
            
            ChatSession session = super.createChatSession(participant,isNewSession);

         //   mSessions.put(Address.stripResource(participant.getAddress().getAddress()),session);
            return session;
        }

    }

    
    private void requestPresenceRefresh (String address)
    {
        org.jivesoftware.smack.packet.Presence p = new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.error);
        p.setFrom(address);
        qPresence.push(p);
    }

    public class XmppContactListManager extends ContactListManager {

        @Override
        protected void setListNameAsync(final String name, final ContactList list) {
            execute(new Runnable() {
                @Override
                public void run() {
                    do_setListName(name, list);
                }
            });
        }

        // Runs in executor thread
        private void do_setListName(String name, ContactList list) {
            debug(TAG, "set list name");
            //mRoster.getGroup(list.getName()).setName(name);
            try {
                mRoster.getGroup(list.getName()).setName(name);
                notifyContactListNameUpdated(list, name);
            }
            catch (Exception e)
            {}
        }

        @Override
        public String normalizeAddress(String address) {
            return Address.stripResource(address);
        }

        @Override
        public void loadContactListsAsync() {

            execute(new Runnable() {
                @Override
                public void run() {
                    do_loadContactLists();

                }
            });

        }

        // For testing
        /*
        public void loadContactLists() {
            do_loadContactLists();
        }*/

        /**
         * Create new list of contacts from roster entries.
         *
         * Runs in executor thread
         **
         * @return contacts from roster which were not present in skiplist.
         */
        /*
        private Collection<Contact> fillContacts(Collection<RosterEntry> entryIter,
                Set<String> skipList) {

            Roster roster = mConnection.getRoster();

            Collection<Contact> contacts = new ArrayList<Contact>();
            for (RosterEntry entry : entryIter) {

                String address = entry.getUser();
                if (skipList != null && !skipList.add(address))
                    continue;

                String name = entry.getName();
                if (name == null)
                    name = address;

                XmppAddress xaddress = new XmppAddress(address);

                org.jivesoftware.smack.packet.Presence presence = roster.getPresence(address);

                String status = presence.getStatus();
                String resource = null;

                Presence p = new Presence(parsePresence(presence), status,
                        null, null, Presence.CLIENT_TYPE_DEFAULT);

                String from = presence.getFrom();
                if (from != null && from.lastIndexOf("/") > 0) {
                    resource = from.substring(from.lastIndexOf("/") + 1);

                    if (resource.indexOf('.')!=-1)
                        resource = resource.substring(0,resource.indexOf('.'));

                    p.setResource(resource);
                }

                Contact contact = mContactListManager.getContact(xaddress.getBareAddress());

                if (contact == null)
                    contact = new Contact(xaddress, name);

                contact.setPresence(p);

                contacts.add(contact);


            }
            return contacts;
        }
         */

        // Runs in executor thread
        private void do_loadContactLists() {

            debug(TAG, "load contact lists");


            //Set<String> seen = new HashSet<String>();

            // This group will also contain all the unfiled contacts.  We will create it locally if it
            // does not exist.
            /*
            String generalGroupName = mContext.getString(R.string.buddies);

            for (Iterator<RosterGroup> giter = roster.getGroups().iterator(); giter.hasNext();) {

                RosterGroup group = giter.next();

                debug(TAG, "loading group: " + group.getName() + " size:" + group.getEntryCount());

                Collection<Contact> contacts = fillContacts(group.getEntries(), null);

                if (group.getName().equals(generalGroupName) && roster.getUnfiledEntryCount() > 0) {
                    Collection<Contact> unfiled = fillContacts(roster.getUnfiledEntries(), null);
                    contacts.addAll(unfiled);
                }

                XmppAddress groupAddress = new XmppAddress(group.getName());
                ContactList cl = new ContactList(groupAddress, group.getName(), group
                        .getName().equals(generalGroupName), contacts, this);

                notifyContactListCreated(cl);

                notifyContactsPresenceUpdated(contacts.toArray(new Contact[contacts.size()]));
            }

            Collection<Contact> contacts;
            if (roster.getUnfiledEntryCount() > 0) {
                contacts = fillContacts(roster.getUnfiledEntries(), null);
            } else {
                contacts = new ArrayList<Contact>();
            }

            ContactList cl = getContactList(generalGroupName);
                cl = new ContactList(groupAddress, group.getName(), group
                        .getName().equals(generalGroupName), contacts, this);

            // We might have already created the Buddies contact list above
            if (cl == null) {
                cl = new ContactList(mUser.getAddress(), generalGroupName, true, contacts, this);
                notifyContactListCreated(cl);

                notifyContactsPresenceUpdated(contacts.toArray(new Contact[contacts.size()]));
            }
             */

            //since we don't show lists anymore, let's just load all entries together


            ContactList cl;

            try {
                cl = getDefaultContactList();
            } catch (ImException e1) {
                debug(TAG,"couldn't read default list");
                cl = null;
            }

            if (cl == null)
            {
                String generalGroupName = mContext.getString(R.string.buddies);

                Collection<Contact> contacts = new ArrayList<Contact>();
                XmppAddress groupAddress = new XmppAddress(generalGroupName);

                cl = new ContactList(groupAddress,generalGroupName, true, contacts, this);
                cl.setDefault(true);
                mDefaultContactList = cl;
                notifyContactListCreated(cl);
            }

            if (mConnection != null) {

                for (RosterEntry rEntry : mRoster.getEntries()) {
                    String address = rEntry.getUser();
                    String name = rEntry.getName();

                    if (mUser.getAddress().getBareAddress().equals(address)) //don't load a roster for yourself
                        continue;

                    Contact contact = getContact(address);

                    if (contact == null) {
                        XmppAddress xAddr = new XmppAddress(address);

                        if (name == null || name.length() == 0)
                            name = xAddr.getUser();

                        contact = new Contact(xAddr, name);

                    }

                    if (!cl.containsContact(contact)) {
                        try {
                            cl.addExistingContact(contact);
                        } catch (ImException e) {
                            debug(TAG, "could not add contact to list: " + e.getLocalizedMessage());
                        }
                    }


                    int subStatus = subStatus = Imps.ContactsColumns.SUBSCRIPTION_STATUS_NONE;
                    if (rEntry.getStatus() == RosterPacket.ItemStatus.SUBSCRIPTION_PENDING)
                        subStatus = Imps.ContactsColumns.SUBSCRIPTION_STATUS_SUBSCRIBE_PENDING;

                    int subType = 0;
                    if (rEntry.getType() == RosterPacket.ItemType.both)
                        subType = Imps.ContactsColumns.SUBSCRIPTION_TYPE_BOTH;
                    else if (rEntry.getType() == RosterPacket.ItemType.none)
                        subType = Imps.ContactsColumns.SUBSCRIPTION_TYPE_NONE;
                    else if (rEntry.getType() == RosterPacket.ItemType.to) {
                        subType = Imps.ContactsColumns.SUBSCRIPTION_TYPE_TO;
                    }
                    else if (rEntry.getType() == RosterPacket.ItemType.from) {
                        subType = Imps.ContactsColumns.SUBSCRIPTION_TYPE_FROM;
                    }
                    else if (rEntry.getType() == RosterPacket.ItemType.remove)
                        subType = Imps.ContactsColumns.SUBSCRIPTION_TYPE_REMOVE;


                    try {

                        mContactListManager.getSubscriptionRequestListener().onSubScriptionChanged(contact, mProviderId, mAccountId, subStatus, subType);
                    }
                    catch (RemoteException re)
                    {

                    }


                }
            }

            notifyContactListLoaded(cl);
            notifyContactListsLoaded();

        }

     // Runs in executor thread
        public void addContactsToList(Collection<String> addresses) {

            debug(TAG, "add contacts to lists");


            ContactList cl;

            try {
                cl = mContactListManager.getDefaultContactList();
            } catch (ImException e1) {
                debug(TAG,"couldn't read default list");
                cl = null;
            }

            if (cl == null)
            {
                String generalGroupName = mContext.getString(R.string.buddies);

                Collection<Contact> contacts = new ArrayList<Contact>();
                XmppAddress groupAddress = new XmppAddress(generalGroupName);

                cl = new ContactList(groupAddress,generalGroupName, true, contacts, this);

                notifyContactListCreated(cl);
            }

            for (String address : addresses)
            {

                if (mUser.getAddress().getBareAddress().equals(address)) //don't load a roster for yourself
                    continue;

                Contact contact = getContact(address);

                if (contact == null)
                {
                    XmppAddress xAddr = new XmppAddress(address);

                    contact = new Contact(xAddr,xAddr.getUser());

                }

                //org.jivesoftware.smack.packet.Presence p = roster.getPresence(contact.getAddress().getBareAddress());
                //qPresence.push(p);

                if (!cl.containsContact(contact))
                {
                    try {
                        cl.addExistingContact(contact);
                    } catch (ImException e) {
                        debug(TAG,"could not add contact to list: " + e.getLocalizedMessage());
                    }
                }

            }

            notifyContactListLoaded(cl);
            notifyContactListsLoaded();

        }

        /*
         * iterators through a list of contacts to see if there were any Presence
         * notifications sent before the contact was loaded
         */
        /*
        private void processQueuedPresenceNotifications (Collection<Contact> contacts)
        {

        	Roster roster = mConnection.getRoster();

        	//now iterate through the list of queued up unprocessed presence changes
        	for (Contact contact : contacts)
        	{

        		String address = parseAddressBase(contact.getAddress().getFullName());

        		org.jivesoftware.smack.packet.Presence presence = roster.getPresence(address);

        		if (presence != null)
        		{
        			debug(TAG, "processing queued presence: " + address + " - " + presence.getStatus());

        			unprocdPresence.remove(address);

        			contact.setPresence(new Presence(parsePresence(presence), presence.getStatus(), null, null, Presence.CLIENT_TYPE_DEFAULT));

        			Contact[] updatedContact = {contact};
        			notifyContactsPresenceUpdated(updatedContact);	
        		}



        	}
        }*/

        /**
        public void listenToRoster(final Roster roster) {

            roster.addRosterListener(rListener);
        }


        RosterListener rListener = new RosterListener() {


            @Override
            public void presenceChanged(org.jivesoftware.smack.packet.Presence presence) {
                
                qPresence.push(presence);
                 
            }

            @Override
            public void entriesUpdated(Collection<String> addresses) {

                
                for (String address :addresses)
                {

                    requestPresenceRefresh(address);
                    
                }
            }

            @Override
            public void entriesDeleted(Collection<String> addresses) {

                ContactList cl;
                try {
                    cl = mContactListManager.getDefaultContactList();

                    for (String address : addresses) {
                        requestPresenceRefresh(address);

                        Contact contact = new Contact(new XmppAddress(address),address);
                        mContactListManager.notifyContactListUpdated(cl, ContactListListener.LIST_CONTACT_REMOVED, contact);
                    }
                    

                } catch (ImException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            @Override
            public void entriesAdded(Collection<String> addresses) {

                try
                {
                    if (mContactListManager.getState() == LISTS_LOADED)
                    {
                        
                        for (String address : addresses)
                        {
        
                            Contact contact = getContact(address);
                            
                            requestPresenceRefresh(address);

                            if (contact == null)
                            {
                                XmppAddress xAddr = new XmppAddress(address);
                                contact = new Contact(xAddr,xAddr.getUser());
        
                            }
        
                            try
                            {
                                ContactList cl = mContactListManager.getDefaultContactList();
                                if (!cl.containsContact(contact))
                                    cl.addExistingContact(contact);
                                 
                            }
                            catch (Exception e)
                            {
                                debug(TAG,"could not add contact to list: " + e.getLocalizedMessage());

                            }
                        
        
                        }
                        
                    }
                }
                catch (Exception e)
                {
                    Log.d(TAG,"error adding contacts",e);
                }
            }
        };**/


        @Override
        protected ImConnection getConnection() {
            return XmppConnection.this;
        }

        @Override
        protected void doRemoveContactFromListAsync(Contact contact, ContactList list) {
            // FIXME synchronize this to executor thread
            if (mConnection == null)
                return;
            
            String address = contact.getAddress().getAddress();

            //otherwise, send unsub message and delete from local contact database
            org.jivesoftware.smack.packet.Presence presence = new org.jivesoftware.smack.packet.Presence(
                    org.jivesoftware.smack.packet.Presence.Type.unsubscribe);            
            presence.setTo(address);
            sendPacket(presence);
            
            presence = new org.jivesoftware.smack.packet.Presence(
                    org.jivesoftware.smack.packet.Presence.Type.unsubscribed);            
            presence.setTo(address);
            sendPacket(presence);
            
            try {
                RosterEntry entry = mRoster.getEntry(address);
                RosterGroup group = mRoster.getGroup(list.getName());

                if (group == null) {
                    debug(TAG, "could not find group " + list.getName() + " in roster");
                    if (mRoster != null)
                        mRoster.removeEntry(entry);

                }
                else
                {
                    group.removeEntry(entry);
                    entry = mRoster.getEntry(address);
                    // Remove from Roster if this is the last group
                    if (entry != null && entry.getGroups().size() <= 1)
                        mRoster.removeEntry(entry);

                }

            } catch (Exception e) {
                debug(TAG, "remove entry failed: " + e.getMessage());
                throw new RuntimeException(e);
            }


            notifyContactListUpdated(list, ContactListListener.LIST_CONTACT_REMOVED, contact);
        }

        @Override
        protected void doDeleteContactListAsync(ContactList list) {
            // TODO delete contact list
            debug(TAG, "delete contact list " + list.getName());

        }

        @Override
        protected void doCreateContactListAsync(String name, Collection<Contact> contacts,
                boolean isDefault) {
            debug(TAG, "create contact list " + name + " default " + isDefault);
        }

        @Override
        protected void doBlockContactAsync(String address, boolean block) {

            blockContact(address, block);
        }

        @Override
        protected void doAddContactToListAsync(Contact contact, ContactList list, boolean autoSubscribedPresence) throws ImException {
            debug(TAG, "add contact to " + list.getName());

            if (!list.containsContact(contact)) {
                try {
                    list.addExistingContact(contact);
                } catch (ImException e) {
                    debug(TAG, "could not add contact to list: " + e.getLocalizedMessage());
                }
            }

            if (mConnection.isConnected())
            {
                RosterEntry rEntry;

                String[] groups = new String[] { list.getName() };

                RosterPacket.ItemStatus status = null;
                RosterPacket.ItemType type = RosterPacket.ItemType.none;

                if (autoSubscribedPresence)
                {
                    status = RosterPacket.ItemStatus.subscribe;
                    type = RosterPacket.ItemType.both;
                }
                else
                {
                    status = RosterPacket.ItemStatus.SUBSCRIPTION_PENDING;
                    type = RosterPacket.ItemType.to;
                }

                try {
                    rEntry = mRoster.getEntry(contact.getAddress().getBareAddress());
                    RosterGroup rGroup = mRoster.getGroup(list.getName());

                    if (rGroup == null)
                    {
                        if (rEntry == null) {

                            addRosterEntry(contact.getAddress().getBareAddress(), contact.getName(), null, status, type);
                            rEntry = mRoster.getEntry(contact.getAddress().getBareAddress());
                        }

                    }
                    else if (rEntry == null)
                    {
                        addRosterEntry(contact.getAddress().getBareAddress(), contact.getName(), groups, status, type);
                        rEntry = mRoster.getEntry(contact.getAddress().getBareAddress());
                    }


                } catch (XMPPException e) {

                    debug(TAG,"error updating remote roster",e);
                    throw new ImException("error updating remote roster");
                } catch (Exception e) {
                    String msg = "Not logged in to server while updating remote roster";
                    debug(TAG, msg, e);
                    throw new ImException(msg);
                }

                if (autoSubscribedPresence) {


                    //i want your presence
                    org.jivesoftware.smack.packet.Presence reqSubscribe = new org.jivesoftware.smack.packet.Presence(
                            org.jivesoftware.smack.packet.Presence.Type.subscribe);
                    reqSubscribe.setTo(contact.getAddress().getBareAddress());
                    sendPacket(reqSubscribe);

                    findOrCreateSession(contact.getAddress().getBareAddress(), false).setSubscribed(true);

                }

                do_loadContactLists();
            }

            notifyContactListUpdated(list, ContactListListener.LIST_CONTACT_ADDED, contact);


        }

        private void addRosterEntry (String user, String name, String[] groups, RosterPacket.ItemStatus status, RosterPacket.ItemType type) throws SmackException, XMPPException.XMPPErrorException
        {
            // Create and send roster entry creation packet.
            RosterPacket rosterPacket = new RosterPacket();
            rosterPacket.setType(IQ.Type.set);
            RosterPacket.Item item = new RosterPacket.Item(user, name);
            item.setItemStatus(status);
            item.setItemType(type);
            if (groups != null) {
                for (String group : groups) {
                    if (group != null && group.trim().length() > 0) {
                        item.addGroupName(group);
                    }
                }
            }
            rosterPacket.addRosterItem(item);
            mConnection.createPacketCollectorAndSend(rosterPacket).nextResultOrThrow();

            /**
            // Create a presence subscription packet and send.
            org.jivesoftware.smack.packet.Presence presencePacket = new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.subscribe);
            presencePacket.setTo(user);
            mConnection.sendStanza(presencePacket);
             */
        }

        public boolean blockContact(String blockContact, boolean doBlock) {


            PrivacyItem item=new PrivacyItem(PrivacyItem.Type.jid,blockContact, false, 7);
            PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(mConnection);

            if (privacyManager != null) {
                List<PrivacyItem> list = new ArrayList<PrivacyItem>();
                list.add(item);

                try {
                    privacyManager.updatePrivacyList(PRIVACY_LIST_DEFAULT, list);
                    privacyManager.setActiveListName(PRIVACY_LIST_DEFAULT);
                    return true;
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            return false;
        }

        @Override
        public void declineSubscriptionRequest(Contact contact) {
            debug(TAG, "decline subscription");
            org.jivesoftware.smack.packet.Presence response = new org.jivesoftware.smack.packet.Presence(
                    org.jivesoftware.smack.packet.Presence.Type.unsubscribed);
            response.setTo(contact.getAddress().getBareAddress());
            sendPacket(response);

            try { mRoster.reload(); }
            catch (Exception e){}

            try {
                mContactListManager.getSubscriptionRequestListener().onSubscriptionDeclined(contact, mProviderId, mAccountId);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            findOrCreateSession(contact.getAddress().getBareAddress(), false).setSubscribed(false);

        }

        @Override
        public void approveSubscriptionRequest(final Contact contact) {


            org.jivesoftware.smack.packet.Presence response = new org.jivesoftware.smack.packet.Presence(
                    org.jivesoftware.smack.packet.Presence.Type.subscribed);
            response.setTo(contact.getAddress().getBareAddress());
            sendPacket(response);

            response = new org.jivesoftware.smack.packet.Presence(
                    org.jivesoftware.smack.packet.Presence.Type.subscribe);
            response.setTo(contact.getAddress().getBareAddress());
            sendPacket(response);


//            try { mRoster.reload(); }
//            catch (Exception e){}

            try
            {
                //doAddContactToListAsync(contact, getContactListManager().getDefaultContactList());
                mContactListManager.getSubscriptionRequestListener().onSubscriptionApproved(contact, mProviderId, mAccountId);


            }
            catch (RemoteException e) {
                debug (TAG, "error responding to subscription approval: " + e.getLocalizedMessage());

            }

            findOrCreateSession(contact.getAddress().getBareAddress(), false).setSubscribed(true);

            sendPresencePacket();
            requestPresenceRefresh(contact.getAddress().getBareAddress());
            qAvatar.push(contact.getAddress().getAddress());
        }

        @Override
        public Contact[] createTemporaryContacts(String[] addresses) {
            // debug(TAG, "create temporary " + address);

            Contact[] contacts = new Contact[addresses.length];

            int i = 0;

            for (String address : addresses)
            {
                contacts[i++] = makeContact(address);
            }

            notifyContactsPresenceUpdated(contacts);
            return contacts;
        }

        @Override
        protected void doSetContactName(String address, String name) throws ImException {
            RosterEntry entry = mRoster.getEntry(address);
            // confirm entry still exists
            if (entry == null) {
                return;
            }
            // set name
            try {
                entry.setName(name);
            }
            catch (Exception e)
            {
                throw new ImException(e.toString());
            }
        }
    }

    public void sendHeartbeat(final long heartbeatInterval) {
        // Don't let heartbeats queue up if we have long running tasks - only
        // do the heartbeat if executor is idle.
        boolean success = executeIfIdle(new Runnable() {
            @Override
            public void run() {
                debug(TAG, "heartbeat state = " + getState());
                doHeartbeat(heartbeatInterval);
            }
        });

        if (!success) {
            debug(TAG, "failed to schedule heartbeat state = " + getState());
        }
    }

    // Runs in executor thread
    public void doHeartbeat(long heartbeatInterval) {
        heartbeatSequence++;

        if (getState() == SUSPENDED) {
            debug(TAG, "heartbeat during suspend");
            return;
        }

        if (mConnection == null && mRetryLogin) {
            debug(TAG, "reconnect with login");
            do_login();
            return;
        }

        if (mConnection == null)
            return;

        if (mNeedReconnect) {
            reconnect();
        } else if (!mConnection.isConnected() && getState() == LOGGED_IN) {
            // Smack failed to tell us about a disconnect
            debug(TAG, "reconnect on unreported state change");
            setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network disconnected"));
            force_reconnect();
        } else if (getState() == LOGGED_IN) {
            if (PING_ENABLED) {
                // Check ping on every heartbeat.  checkPing() will return true immediately if we already checked.
                if (!mPingSuccess) {
                    debug(TAG, "reconnect on ping failed: " + mUser.getAddress().getAddress());
                    setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network timeout"));
                    maybe_reconnect();
                } else {
                    // Send pings only at intervals configured by the user
                    if (heartbeatSequence >= heartbeatInterval) {
                        heartbeatSequence = 0;
                        debug(TAG, "ping");
                        sendPing();
                    }
                }
            }
        }
    }

    private void clearPing() {
        debug(TAG, "clear ping");
        heartbeatSequence = 0;
    }


    boolean mPingSuccess = true;
    // Runs in executor thread
    private void sendPing() {

        try {

            mPingSuccess = mPingManager.pingMyServer() ;
       ;}
        catch (Exception e)
        {
            mPingSuccess = false;
        }


    }


    @Override
    public void networkTypeChanged() {

        super.networkTypeChanged();

        execute(new Runnable() {
            @Override
            public void run() {
               // if (mState == SUSPENDED || mState == SUSPENDING) {
                    debug(TAG, "network type changed");
                    mNeedReconnect = false;
                    setState(LOGGING_IN, null);
                    reconnect();
                //}
            }
        });

    }

    /*
     * Force a shutdown and reconnect, unless we are already reconnecting.
     *
     * Runs in executor thread
     */
    private void force_reconnect() {
        debug(TAG, "force_reconnect mNeedReconnect=" + mNeedReconnect + " state=" + getState()
                + " connection?=" + (mConnection != null));

        if (mConnection == null)
            return;
        if (mNeedReconnect)
            return;

        mNeedReconnect = true;

        try {
            if (mConnection != null && mConnection.isConnected()) {
                mStreamHandler.quickShutdown();
            }
        } catch (Exception e) {
            Log.w(TAG, "problem disconnecting on force_reconnect: " + e.getMessage());
        }

        reconnect();
    }

    /*
     * Reconnect unless we are already in the process of doing so.
     *
     * Runs in executor thread.
     */
    private void maybe_reconnect() {
        debug(TAG, "maybe_reconnect mNeedReconnect=" + mNeedReconnect + " state=" + getState()
                + " connection?=" + (mConnection != null));

        // This is checking whether we are already in the process of reconnecting.  If we are,
        // doHeartbeat will take care of reconnecting.
        if (mNeedReconnect)
            return;

        if (getState() == SUSPENDED)
            return;

        if (mConnection == null)
            return;

        mNeedReconnect = true;
        reconnect();
    }

    /*
     * Retry connecting
     *
     * Runs in executor thread
     */
    private void reconnect() {
        if (getState() == SUSPENDED) {
            debug(TAG, "reconnect during suspend, ignoring");
            return;
        }

        if (mConnection != null) {
            // It is safe to ask mConnection whether it is connected, because either:
            // - We detected an error using ping and called force_reconnect, which did a shutdown
            // - Smack detected an error, so it knows it is not connected
            // so there are no cases where mConnection can be confused about being connected here.
            // The only left over cases are reconnect() being called too many times due to errors
            // reported multiple times or errors reported during a forced reconnect.

            // The analysis above is incorrect in the case where Smack loses connectivity
            // while trying to log in.  This case is handled in a future heartbeat
            // by checking ping responses.
            clearPing();
            if (mConnection.isConnected()) {
                debug(TAG,"reconnect while already connected, assuming good: " + mConnection);
                mNeedReconnect = false;
                setState(LOGGED_IN, null);
                return;
            }
            debug(TAG, "reconnect");

            try {
                if (mStreamHandler.isResumePossible()) {
                    // Connect without binding, will automatically trigger a resume
                    debug(TAG, "mStreamHandler resume");
                    mConnection.connect();
                    initServiceDiscovery();
                } else {
                    debug(TAG, "reconnection on network change failed: " + mUser.getAddress().getAddress());

                    mConnection = null;
                    mNeedReconnect = true;
                    setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, null));

                    while (mNeedReconnect)
                    {
                        do_login();
                        
                        if (mNeedReconnect)
                            try { Thread.sleep(3000);}
                            catch (Exception e){}
                    }
                    
                }
            } catch (Exception e) {
                if (mStreamHandler != null)
                    mStreamHandler.quickShutdown();

                mConnection = null;
                debug(TAG, "reconnection attempt failed", e);
                // Smack incorrectly notified us that reconnection was successful, reset in case it fails
                mNeedReconnect = false;
                setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));

                //while (mNeedReconnect)
                  //  do_login();

            }
        } else {
            mNeedReconnect = false;
            mConnection = null;
            debug(TAG, "reconnection on network change failed");

            setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR,
                    "reconnection on network change failed"));

            //while (mNeedReconnect)
              //  do_login();

        }
    }

    @Override
    protected void setState(int state, ImErrorInfo error) {
        debug(TAG, "setState to " + state);
        super.setState(state, error);
        
        if (state == LOGGED_IN)
        {
            //update and send new presence packet out
            mUserPresence = new Presence(Presence.AVAILABLE, "", Presence.CLIENT_TYPE_MOBILE);
            sendPresencePacket();

            if (mSessionManager != null) {
                //request presence of remote contact for all existing sessions
                for (ChatSessionAdapter session : mSessionManager.getAdapter().getActiveChatSessions()) {
                    requestPresenceRefresh(session.getAddress());
                }
            }

            mChatGroupManager.reconnectAll();
        }
    }    
    
    public void debug(String tag, String msg) {
        //  if (Log.isLoggable(TAG, Log.DEBUG)) {
        if (Debug.DEBUG_ENABLED) {
            Log.d(tag, "" + mGlobalId + " : " + msg);
        }
    }

    public void debug(String tag, String msg, Exception e) {
        if (Debug.DEBUG_ENABLED) {
            Log.e(tag, "" + mGlobalId + " : " + msg, e);
        }
    }

    /*
    @Override
    public void handle(Callback[] arg0) throws IOException {

        for (Callback cb : arg0) {
            debug(TAG, cb.toString());
        }

    }*/

    /*
    public class MySASLDigestMD5Mechanism extends SASLMechanism
    {

        public MySASLDigestMD5Mechanism(SASLAuthentication saslAuthentication)
        {
            super(saslAuthentication);
        }

        protected void authenticate()
            throws IOException, XMPPException
        {
            String mechanisms[] = {
                getName()
            };
            java.util.Map props = new HashMap();
            sc = Sasl.createSaslClient(mechanisms, null, "xmpp", hostname, props, this);
            super.authenticate();
        }

        public void authenticate(String username, String host, String password)
            throws IOException, XMPPException
        {
            authenticationId = username;
            this.password = password;
            hostname = host;
            String mechanisms[] = {
                getName()
            };
            java.util.Map props = new HashMap();
            sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, this);
            super.authenticate();
        }

        public void authenticate(String username, String host, CallbackHandler cbh)
            throws IOException, XMPPException
        {
            String mechanisms[] = {
                getName()
            };
            java.util.Map props = new HashMap();
            sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, cbh);
            super.authenticate();
        }

        protected String getName()
        {
            return "DIGEST-MD5";
        }

        public void challengeReceived(String challenge)
            throws IOException
        {
            //StringBuilder stanza = new StringBuilder();
            byte response[];
            if(challenge != null)
                response = sc.evaluateChallenge(Base64.decode(challenge));
            else
                //response = sc.evaluateChallenge(null);
                response = sc.evaluateChallenge(new byte[0]);
            //String authenticationText = "";
            Packet responseStanza;
            //if(response != null)
            //{
                //authenticationText = Base64.encodeBytes(response, 8);
                //if(authenticationText.equals(""))
                    //authenticationText = "=";

                if (response == null){
                    responseStanza = new Response();
                } else {
                    responseStanza = new Response(Base64.encodeBytes(response,Base64.DONT_BREAK_LINES));
                }
            //}
            //stanza.append("<response xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
            //stanza.append(authenticationText);
            //stanza.append("</response>");
            //getSASLAuthentication().send(stanza.toString());
            getSASLAuthentication().send(responseStanza);
        }
    }
     */
    private void initServiceDiscovery() {
        debug(TAG, "init service discovery");
        // register connection features
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mConnection);

        if (!sdm.includesFeature(DISCO_FEATURE))
            sdm.addFeature(DISCO_FEATURE);
        if (!sdm.includesFeature(DeliveryReceipt.NAMESPACE))
            sdm.addFeature(DeliveryReceipt.NAMESPACE);

        DeliveryReceiptManager.getInstanceFor(mConnection).dontAutoAddDeliveryReceiptRequests();
        DeliveryReceiptManager.getInstanceFor(mConnection).setAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.disabled);

    }


    private void onReconnectionSuccessful() {
        mNeedReconnect = false;
        setState(LOGGED_IN, null);
        
    }


    private void addProviderManagerExtensions ()
    {

        //  Private Data Storage
       // ProviderManager.addIQProvider("query","jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());

        //  Time
        /**
        try {
            ProviderManager.addIQProvider("query","jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
        } catch (ClassNotFoundException e) {
            Log.w("TestClient", "Can't load class for org.jivesoftware.smackx.packet.Time");
        }*/

        //  Roster Exchange
//        ProviderManager.addExtensionProvider("x","jabber:x:roster", new RosterExchangeProvider());

        //  Message Events
       // ProviderManager.addExtensionProvider("x","jabber:x:event", new MessageEventProvider());

        //  Chat State

        ProviderManager.addExtensionProvider("active","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        ProviderManager.addExtensionProvider("composing","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        ProviderManager.addExtensionProvider("paused","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        ProviderManager.addExtensionProvider("inactive","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        ProviderManager.addExtensionProvider("gone","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());


        //  XHTML
        ProviderManager.addExtensionProvider("html","http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());

        //  Group Chat Invitations
      //  ProviderManager.addExtensionProvider("x","jabber:x:conference", new GroupChatInvitation.Provider());

        //  Service Discovery # Items
        ProviderManager.addIQProvider("query","http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());

        //  Service Discovery # Info
        ProviderManager.addIQProvider("query","http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());

        //  Data Forms
        ProviderManager.addExtensionProvider("x","jabber:x:data", new DataFormProvider());

        //  MUC User
        ProviderManager.addExtensionProvider("x","http://jabber.org/protocol/muc#user", new MUCUserProvider());

        //  MUC Admin
        ProviderManager.addIQProvider("query","http://jabber.org/protocol/muc#admin", new MUCAdminProvider());

        //  MUC Owner
        ProviderManager.addIQProvider("query","http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());


        //  Delayed Delivery
     //   ProviderManager.addExtensionProvider("x","jabber:x:delay", new DelayInformationProvider());

        //  Version
        try {
            ProviderManager.addIQProvider("query","jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
        } catch (ClassNotFoundException err) {
            //  Not sure what's happening here.
        }

        //  VCard
        ProviderManager.addIQProvider("vCard","vcard-temp", new VCardProvider());

        //  Offline Message Requests
        ProviderManager.addIQProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());

        //  Offline Message Indicator
        ProviderManager.addExtensionProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());

        //  Last Activity
        ProviderManager.addIQProvider("query","jabber:iq:last", new LastActivity.Provider());

        //  User Search
        ProviderManager.addIQProvider("query","jabber:iq:search", new UserSearch.Provider());

        //  SharedGroupsInfo
        ProviderManager.addIQProvider("sharedgroup","http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());

        //  JEP-33: Extended Stanza Addressing
        ProviderManager.addExtensionProvider("addresses","http://jabber.org/protocol/address", new MultipleAddressesProvider());

        //   FileTransfer
        ProviderManager.addIQProvider("si","http://jabber.org/protocol/si", new StreamInitiationProvider());

        ProviderManager.addIQProvider("query","http://jabber.org/protocol/bytestreams", new BytestreamsProvider());

        //  Privacy
        ProviderManager.addIQProvider("query","jabber:iq:privacy", new PrivacyProvider());
        ProviderManager.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider());
        ProviderManager.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.MalformedActionError());
        ProviderManager.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadLocaleError());
        ProviderManager.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadPayloadError());
        ProviderManager.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadSessionIDError());
        ProviderManager.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.SessionExpiredError());

    }

    class NameSpace {

        public static final String DISCO_INFO = "http://jabber.org/protocol/disco#info";
        public static final String DISCO_ITEMS = "http://jabber.org/protocol/disco#items";
        public static final String IQ_GATEWAY = "jabber:iq:gateway";
        public static final String IQ_GATEWAY_REGISTER = "jabber:iq:gateway:register";
        public static final String IQ_LAST = "jabber:iq:last";
        public static final String IQ_REGISTER = "jabber:iq:register";
        public static final String IQ_REGISTERED = "jabber:iq:registered";
        public static final String IQ_ROSTER = "jabber:iq:roster";
        public static final String IQ_VERSION = "jabber:iq:version";
        public static final String CHATSTATES = "http://jabber.org/protocol/chatstates";
        public static final String XEVENT = "jabber:x:event";
        public static final String XDATA = "jabber:x:data";
        public static final String MUC = "http://jabber.org/protocol/muc";
        public static final String MUC_USER = MUC + "#user";
        public static final String MUC_ADMIN = MUC + "#admin";
        public static final String SPARKNS = "http://www.jivesoftware.com/spark";
        public static final String DELAY = "urn:xmpp:delay";
        public static final String OFFLINE = "http://jabber.org/protocol/offline";
        public static final String X_DELAY = "jabber:x:delay";
        public static final String VCARD_TEMP = "vcard-temp";
        public static final String VCARD_TEMP_X_UPDATE = "vcard-temp:x:update";
        public static final String ATTENTIONNS = "urn:xmpp:attention:0";

    }


    public boolean registerAccount (Imps.ProviderSettings.QueryMap providerSettings, String username, String password, Map<String,String> params) throws Exception
    {

        initConnection(providerSettings, username);

        if (mConnection.isConnected() && mConnection.isSecureConnection()) {
            org.jivesoftware.smackx.iqregister.AccountManager aMgr = org.jivesoftware.smackx.iqregister.AccountManager.getInstance(mConnection);

            if (aMgr.supportsAccountCreation()) {
                aMgr.createAccount(username, password, params);

                return true;

            }
        }

        return false;

    }

    public boolean changeServerPassword (long providerId, long accountId, String oldPassword, String newPassword) throws Exception
    {

        boolean result = false;

        try {

            loginSync(accountId, oldPassword, providerId, false);

            if (mConnection.isConnected() && mConnection.isSecureConnection() && mConnection.isAuthenticated()) {
                org.jivesoftware.smackx.iqregister.AccountManager aMgr = org.jivesoftware.smackx.iqregister.AccountManager.getInstance(mConnection);
                aMgr.changePassword(newPassword);
                result = true;
                do_logout();
            }
        }
        catch (XMPPException xe)
        {
            result = false;
        }

        return result;

    }

    private void handleChatState (String from, String chatStateXml) throws RemoteException {

        Presence p = null;
        Contact contact = mContactListManager.getContact(XmppAddress.stripResource(from));
        if (contact == null)
            return;

        boolean isTyping = false;

        //handle is-typing, probably some indication on screen
        if (chatStateXml.contains(ChatState.active.toString())) {
            p = new Presence(Presence.AVAILABLE, "", null, null,
                    Presence.CLIENT_TYPE_MOBILE);

        }
        else if (chatStateXml.contains(ChatState.composing.toString())) {
            p = new Presence(Presence.AVAILABLE, "", null, null,
                    Presence.CLIENT_TYPE_MOBILE);

            isTyping = true;

        }
        else if (chatStateXml.contains(ChatState.inactive.toString())||chatStateXml.contains(ChatState.paused.toString())) {

        }
        else if (chatStateXml.contains(ChatState.gone.toString())) {

        }

        IChatSession csa = mSessionManager.getAdapter().getChatSession(from);
        csa.setContactTyping(contact, isTyping);

        if (p != null) {
            String[] presenceParts = from.split("/");
            if (presenceParts.length > 1)
                p.setResource(presenceParts[1]);

            contact.setPresence(p);
            Collection<Contact> contactsUpdate = new ArrayList<Contact>();
            contactsUpdate.add(contact);
            mContactListManager.notifyContactsPresenceUpdated(contactsUpdate.toArray(new Contact[contactsUpdate.size()]));

        }


    }

    @Override
    public void sendTypingStatus (final String to, final boolean isTyping)
    {
        mExecutor.execute(new Runnable() {
            public void run() {
                sendChatState(to, isTyping ? ChatState.composing : ChatState.inactive);
            }
        });
    }

    private void sendChatState (String to, ChatState currentChatState)
    {
        try {

            if (mConnection.isConnected())
            {
                Chat thisChat = mChatManager.createChat(to);
                ChatStateManager.getInstance(mConnection).setCurrentState(currentChatState, thisChat);
            }
        }
        catch (Exception e)
        {
            Log.e(ImApp.LOG_TAG,"error sending chat state",e);
        }
    }

    private void setPresence (String from, int presenceType) {

        Presence p = null;
        Contact contact = mContactListManager.getContact(XmppAddress.stripResource(from));
        if (contact == null)
            return;

        p = new Presence(presenceType, "", null, null,
                Presence.CLIENT_TYPE_MOBILE);

        String[] presenceParts = from.split("/");
        if (presenceParts.length > 1)
            p.setResource(presenceParts[1]);

        contact.setPresence(p);
        Collection<Contact> contactsUpdate = new ArrayList<Contact>();
        contactsUpdate.add(contact);
        mContactListManager.notifyContactsPresenceUpdated(contactsUpdate.toArray(new Contact[contactsUpdate.size()]));



    }

    private Contact handlePresenceChanged(org.jivesoftware.smack.packet.Presence presence) {

        if (presence == null || presence.getFrom() == null) //our presence isn't really valid
            return null;

        if (presence.getType() == org.jivesoftware.smack.packet.Presence.Type.error)
        {            
            if (mRoster == null)
                return null;

            if (presence.getFrom() == null)
                return null;
            
            presence = mRoster.getPresence(presence.getFrom());
        }
        
        if (TextUtils.isEmpty(presence.getFrom()))
            return null;

        if (presence.getFrom().startsWith(mUser.getAddress().getBareAddress())) //ignore presence from yourself
            return null;

        XmppAddress xaddress = new XmppAddress(presence.getFrom());

        Presence p = new Presence(parsePresence(presence), presence.getStatus(), null, null,
                Presence.CLIENT_TYPE_MOBILE);

        //this is only persisted in memory
        p.setPriority(presence.getPriority());

        // Get presence from the Roster to handle priorities and such
        // TODO: this causes bad network and performance issues
        //   if (presence.getType() == Type.available) //get the latest presence for the highest priority

        if (mContactListManager == null)
            return null; //we may have logged out

        Contact contact = mContactListManager.getContact(xaddress.getBareAddress());

        String[] presenceParts = presence.getFrom().split("/");
        if (presenceParts.length > 1)
            p.setResource(presenceParts[1]);


        if (presence.getType() == org.jivesoftware.smack.packet.Presence.Type.subscribe
                ) {
            debug(TAG, "got subscribe request: " + presence.getFrom());

            try
            {
                if (contact == null) {
                    XmppAddress xAddr = new XmppAddress(presence.getFrom());
                    contact = new Contact(xAddr, xAddr.getUser());
                }

                contact.setPresence(p);

                ContactList cList = null;

                while (cList == null) {
                    try {

                        cList = getContactListManager().getDefaultContactList();
                        mContactListManager.doAddContactToListAsync(contact, cList, false);
                        mContactListManager.getSubscriptionRequestListener().onSubScriptionRequest(contact, mProviderId, mAccountId);

                    } catch (ImException ime) {

                        debug (TAG, "Contact List not yet ready... let's sleep!");
                        Thread.sleep (1000);
                    }
                }

            }
            catch (Exception e)
            {
                Log.e(TAG,"remote exception on subscription handling",e);
            }


        }
        else if (presence.getType() == org.jivesoftware.smack.packet.Presence.Type.subscribed) {

            debug(TAG, "got subscribed confirmation: " + presence.getFrom());
            try
            {
                //i want your presence
                org.jivesoftware.smack.packet.Presence reqSubscribe = new org.jivesoftware.smack.packet.Presence(
                        org.jivesoftware.smack.packet.Presence.Type.subscribed);
                reqSubscribe.setTo(contact.getAddress().getBareAddress());
                sendPacket(reqSubscribe);

                if (contact == null) {
                    XmppAddress xAddr = new XmppAddress(presence.getFrom());
                    contact = new Contact(xAddr, xAddr.getUser());
                    mContactListManager.doAddContactToListAsync(contact,getContactListManager().getDefaultContactList(),true);
                }

                p.setPriority(1000);//max this out to ensure the user shows as online
                contact.setPresence(p);
                mContactListManager.getSubscriptionRequestListener().onSubscriptionApproved(contact, mProviderId, mAccountId);
            }
            catch (Exception e)
            {
                Log.e(TAG,"remote exception on subscription handling",e);
            }
        }
        else if (presence.getType() == org.jivesoftware.smack.packet.Presence.Type.unsubscribe) {
            debug(TAG,"got unsubscribe request: " + presence.getFrom());

            //TBD how to handle this
            //     mContactListManager.getSubscriptionRequestListener().onUnSubScriptionRequest(contact);
        }
        else if (presence.getType() == org.jivesoftware.smack.packet.Presence.Type.unsubscribed) {
            debug(TAG,"got unsubscribe request: " + presence.getFrom());
            try
            {
                mContactListManager.getSubscriptionRequestListener().onSubscriptionDeclined(contact, mProviderId, mAccountId);

            }
            catch (RemoteException e)
            {
                Log.e(TAG,"remote exception on subscription handling",e);
            }

        }

        //this is typical presence, let's get the latest/highest priority
        debug(TAG,"got presence: " + presence.getFrom() + "=" + presence.getType());

        if (contact != null)
        {

            if (contact.getPresence() != null) {
                Presence pOld = contact.getPresence();

                if (pOld == null || pOld.getResource() == null) {
                    contact.setPresence(p);
                } else if (pOld.getResource() != null && pOld.getResource().equals(p.getResource())) //if the same resource as the existing one, then update it
                {
                    contact.setPresence(p);
                } else if (p.getPriority() >= pOld.getPriority()) //if priority is higher, then override
                {
                    contact.setPresence(p);
                }
            }
            else
                contact.setPresence(p);

            ExtensionElement packetExtension=presence.getExtension("x","vcard-temp:x:update");
            if (packetExtension != null) {

                DefaultExtensionElement o=(DefaultExtensionElement)packetExtension;
                String hash=o.getValue("photo");
                if (hash != null) {


                    boolean hasMatches = DatabaseUtils.doesAvatarHashExist(mContext.getContentResolver(),  Imps.Avatars.CONTENT_URI, contact.getAddress().getBareAddress(), hash);

                    if (!hasMatches) //we must reload
                        qAvatar.push(contact.getAddress().getBareAddress());


                }else
                {
                    //no avatar so push
                    qAvatar.push(contact.getAddress().getAddress());
                }
            }

        }


        return contact;
    }

    private void initPresenceProcessor ()
    {
        mTimerPresence = new Timer();

        mTimerPresence.schedule(new TimerTask() {

            public void run() {

                if (qPresence.size() > 0)
                {
                    Map<String, Contact> alUpdate = new HashMap<String, Contact>();
                    
                    org.jivesoftware.smack.packet.Presence p = null;
                    Contact contact = null;

                    while (qPresence.peek() != null)
                    {
                        p = qPresence.poll();
                        contact = handlePresenceChanged(p);
                        if (contact != null)
                        {
                            alUpdate.put(contact.getAddress().getBareAddress(),contact);

                        }

                    }
                    
                    //loadVCardsAsync();

                    //Log.d(TAG,"XMPP processed presence q=" + alUpdate.size());

                    Collection<Contact> contactsUpdate = alUpdate.values();

                    if (mContactListManager != null)
                        mContactListManager.notifyContactsPresenceUpdated(contactsUpdate.toArray(new Contact[contactsUpdate.size()]));

                }
                
             }

          }, 500, 500);
    }
    
    Timer mTimerPackets = null;
    
    private void initPacketProcessor ()
    {
        mTimerPackets = new Timer();

        mTimerPackets.scheduleAtFixedRate(new TimerTask() {

            public void run() {

                try
                {
                    org.jivesoftware.smack.packet.Stanza packet = null;
                    
                    if (qPacket.size() > 0)
                        while (qPacket.peek()!=null)
                        {
                            packet = qPacket.poll();
                                    
                            if (mConnection == null || (!mConnection.isConnected())) {
                                debug(TAG, "postponed packet to " + packet.getTo()
                                        + " because we are not connected");
                                postpone(packet);
                                return;
                            }
                            try {
                                mConnection.sendPacket(packet);
                            } catch (IllegalStateException ex) {
                                postpone(packet);
                               debug(TAG, "postponed packet to " + packet.getTo()
                                        + " because socket is disconnected");
                            }
                        }


                }
                catch (Exception e)
                {
                    Log.e(TAG,"error processing presence",e);
                }


             }

          }, 500, 500);
    }

}
