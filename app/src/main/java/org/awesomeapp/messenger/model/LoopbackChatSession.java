package org.awesomeapp.messenger.model;

import com.rivescript.RiveScript;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.provider.Imps;

/**
 * Created by Shyamal.Upadhyaya on 29/01/17.
 */

public class LoopbackChatSession extends ChatSession {
    private RiveScript riveScript = null;

    public LoopbackChatSession(ImEntity participant, ChatSessionManager manager) {
        super(participant, manager);
        riveScript = new RiveScript(ImApp.getAppContext(), true);
        riveScript.loadDirectory("rs/Ada");
        riveScript.sortReplies();
    }

    /**
     * Sends a message to other participant(s) in this session asynchronously
     * and adds the message to the history. TODO: more docs on async callbacks.
     *
     * @param message the message to send.
     */
    public int sendMessageAsync(Message message) {

        if (mParticipant instanceof Contact) {
            message.setTo(mParticipant.getAddress());
            message.setType(Imps.MessageType.OUTGOING_NON_ENCRYPTED_VERIFIED);
            mManager.sendMessageAsync(this, message);
        }

        return message.getType();
    }


    public boolean onReceiveMessage(Message message) {

        if (mListener != null) {
            message.setType(Imps.MessageType.INCOMING_NON_ENCRYPTED_VERIFIED);

            //process incoming body with RivaScript - TODO
            String body = message.getBody();
            String reply = riveScript.reply("localuser", body);
            body = reply;
            message.setBody(body);

            return mListener.onIncomingMessage(this, message);
        }
        else {
            return false;
        }
    }
}
