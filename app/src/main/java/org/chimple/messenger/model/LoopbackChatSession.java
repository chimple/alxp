package org.chimple.messenger.model;

import org.chimple.rivescript.RivescriptManager;

import org.chimple.messenger.ImApp;
import org.chimple.messenger.provider.Imps;

/**
 * Created by Shyamal.Upadhyaya on 29/01/17.
 */

public class LoopbackChatSession extends ChatSession {

    public LoopbackChatSession(ImEntity participant, ChatSessionManager manager) {
        super(participant, manager);
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

            String body = message.getBody();
            String reply = RivescriptManager.reply(message.getFrom().getUser(), body);
            String[] replies = reply.split("\n");
            boolean status = true;
            for (String r: replies) {
                message.setBody(r);
                status &= mListener.onIncomingMessage(this, message);
            }
            return status;
        } else {
            return false;
        }
    }
}
