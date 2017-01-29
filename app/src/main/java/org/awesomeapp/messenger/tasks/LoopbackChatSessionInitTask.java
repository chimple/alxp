package org.awesomeapp.messenger.tasks;

import android.os.AsyncTask;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.provider.Imps;
import org.awesomeapp.messenger.service.IChatSession;
import org.awesomeapp.messenger.service.IImConnection;

/**
 * Created by Shyamal.Upadhyaya on 29/01/17.
 */

public class LoopbackChatSessionInitTask extends AsyncTask<String, Long, Long> {

    ImApp mApp;
    long mProviderId;
    long mAccountId;
    int mContactType;

    public LoopbackChatSessionInitTask (ImApp app, long providerId, long accountId, int contactType)
    {
        mApp = app;
        mProviderId = providerId;
        mAccountId = accountId;
        mContactType = contactType;
    }

    public Long doInBackground (String... remoteAddresses)
    {
        if (mProviderId != -1 && mAccountId != -1 && remoteAddresses != null) {
            try {
                IImConnection conn = mApp.getConnection(mProviderId, mAccountId);

                if (conn == null)
                    return -1L;

                for (String address : remoteAddresses) {
                    org.awesomeapp.messenger.service.IChatSessionManager manager = conn.getChatSessionManager();

                    IChatSession session = conn.getChatSessionManager().getChatSession(address);

                    //always need to recreate the MUC after login
                    if (mContactType == Imps.Contacts.TYPE_GROUP)
                        session = conn.getChatSessionManager().createMultiUserChatSession(address, null, null, false);

                    if (session != null && mContactType == Imps.Contacts.TYPE_NORMAL)
                    {
                        if (mProviderId != 2 && session.getDefaultOtrChatSession() != null
                                && (!session.getDefaultOtrChatSession().isChatEncrypted()))
                        {
                            session.getDefaultOtrChatSession().startChatEncryption();
                        }

                    } else {

                        if (mContactType == Imps.Contacts.TYPE_GROUP)
                            session = conn.getChatSessionManager().createMultiUserChatSession(address, null, null, false);
                        else {
                            session = conn.getChatSessionManager().createChatSession(address, false);
                            if(mProviderId != 2) {
                                session.getDefaultOtrChatSession().startChatEncryption();
                            }
                        }



                    }

                    if (session != null)
                        return (session.getId());


                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return -1L;
    }

    protected void onPostExecute(Long chatId) {


    }
}
