package org.chimple.messenger.crypto;

import android.text.TextUtils;

import org.chimple.messenger.model.ChatSession;
import org.chimple.messenger.model.ImErrorInfo;
import org.chimple.messenger.model.Message;
import org.chimple.messenger.model.MessageListener;
import org.chimple.messenger.util.Debug;

import java.util.ArrayList;
import java.util.List;

import net.java.otr4j.OtrException;
import net.java.otr4j.session.Session;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
import net.java.otr4j.session.TLV;

public class OtrChatListener implements MessageListener {

    public static final int TLV_DATA_REQUEST = 0x100;
    public static final int TLV_DATA_RESPONSE = 0x101;
    private OtrChatManager mOtrChatManager;
    private MessageListener mMessageListener;

    public OtrChatListener(OtrChatManager otrChatManager, MessageListener listener) {
        this.mOtrChatManager = otrChatManager;
        this.mMessageListener = listener;
    }

    @Override
    public boolean onIncomingMessage(ChatSession session, Message msg) {

        OtrDebugLogger.log("processing incoming message: " + msg.getID());

        boolean result = false;

        String body = msg.getBody();
        String remoteAddress = msg.getFrom().getAddress();
        String localAddress = msg.getTo().getAddress();

        //body = Debug.injectErrors(body);

        SessionID sessionID = mOtrChatManager.getSessionId(localAddress, remoteAddress);
        SessionStatus otrStatus = mOtrChatManager.getSessionStatus(sessionID);

        List<TLV> tlvs = new ArrayList<TLV>();

        try {

            body = mOtrChatManager.decryptMessage(localAddress, remoteAddress, body, tlvs);

            if (!TextUtils.isEmpty(body)) {
                result = true;
                msg.setBody(body);
                mMessageListener.onIncomingMessage(session, msg);
            } else {

                OtrDebugLogger.log("Decrypted incoming body was null (otrdata?)");

            }

            for (TLV tlv : tlvs) {
                if (tlv.getType() == TLV_DATA_REQUEST) {
                    OtrDebugLogger.log("Got a TLV Data Request: " + new String(tlv.getValue()));

                    mMessageListener.onIncomingDataRequest(session, msg, tlv.getValue());
                    result = true;

                } else if (tlv.getType() == TLV_DATA_RESPONSE) {

                    OtrDebugLogger.log("Got a TLV Data Response: " + new String(tlv.getValue()));

                    mMessageListener.onIncomingDataResponse(session, msg, tlv.getValue());
                    result = true;

                }
            }

        } catch (OtrException oe) {

            OtrDebugLogger.log("error decrypting message", oe);
          //  mOtrChatManager.refreshSession(sessionID.getLocalUserId(),sessionID.getRemoteUserId());
            // msg.setBody("[" + "You received an unreadable encrypted message" + "]");
            // mMessageListener.onIncomingMessage(session, msg);
            // mOtrChatManager.injectMessage(sessionID, "[error please stop/start encryption]");

        }


        SessionStatus newStatus = mOtrChatManager.getSessionStatus(sessionID.getLocalUserId(),sessionID.getRemoteUserId());
        if (newStatus != otrStatus) {

            OtrDebugLogger.log("OTR status changed from: " + otrStatus + " to " + newStatus);
            mMessageListener.onStatusChanged(session, newStatus);
        }

        return result;
    }

    @Override
    public void onIncomingDataRequest(ChatSession session, Message msg, byte[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onIncomingDataResponse(ChatSession session, Message msg, byte[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onSendMessageError(ChatSession session, Message msg, ImErrorInfo error) {

        mMessageListener.onSendMessageError(session, msg, error);
        OtrDebugLogger.log("onSendMessageError: " + msg.toString());
    }

    @Override
    public void onIncomingReceipt(ChatSession ses, String id) {
        mMessageListener.onIncomingReceipt(ses, id);
    }

    @Override
    public void onMessagePostponed(ChatSession ses, String id) {
        mMessageListener.onMessagePostponed(ses, id);
    }

    @Override
    public void onReceiptsExpected(ChatSession ses, boolean isExpected) {
        mMessageListener.onReceiptsExpected(ses, isExpected);
    }

    @Override
    public void onStatusChanged(ChatSession session, SessionStatus status) {
        mMessageListener.onStatusChanged(session, status);
    }

    @Override
    public void onIncomingTransferRequest(OtrDataHandler.Transfer transfer) {
        mMessageListener.onIncomingTransferRequest(transfer);
    }
}
