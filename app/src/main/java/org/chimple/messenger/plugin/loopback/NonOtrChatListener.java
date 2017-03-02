package org.chimple.messenger.plugin.loopback;

import android.text.TextUtils;

import net.java.otr4j.session.SessionStatus;

import org.chimple.messenger.crypto.OtrDataHandler;
import org.chimple.messenger.crypto.OtrDebugLogger;
import org.chimple.messenger.model.ChatSession;
import org.chimple.messenger.model.ImErrorInfo;
import org.chimple.messenger.model.Message;
import org.chimple.messenger.model.MessageListener;

/**
 * Created by Shyamal.Upadhyaya on 29/01/17.
 */

public class NonOtrChatListener implements MessageListener {

    private MessageListener mMessageListener;

    public NonOtrChatListener(MessageListener listener) {
        this.mMessageListener = listener;
    }

    @Override
    public boolean onIncomingMessage(ChatSession session, Message msg) {

        OtrDebugLogger.log("processing incoming message: " + msg.getID());

        boolean result = false;

        String body = msg.getBody();
        String remoteAddress = msg.getFrom().getAddress();

        try {
            if (!TextUtils.isEmpty(body)) {
                result = true;
                msg.setBody(body);
                mMessageListener.onIncomingMessage(session, msg);
            }

        } catch (Exception oe) {

            OtrDebugLogger.log("error message", oe);
        }
        return result;
    }

    @Override
    public void onSendMessageError(ChatSession ses, Message msg, ImErrorInfo error) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMessagePostponed(ChatSession ses, String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onIncomingReceipt(ChatSession ses, String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onReceiptsExpected(ChatSession ses, boolean isExpected) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onStatusChanged(ChatSession session, SessionStatus status) {
        throw new UnsupportedOperationException();
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
    public void onIncomingTransferRequest(OtrDataHandler.Transfer transfer) {
        throw new UnsupportedOperationException();
    }
}
