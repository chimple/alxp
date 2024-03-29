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

package org.chimple.messenger.ui.legacy;

import im.zom.messenger.R;
import org.chimple.messenger.model.Contact;
import org.chimple.messenger.model.ContactListListener;
import org.chimple.messenger.model.ImErrorInfo;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import org.chimple.messenger.ImApp;

public class SimpleAlertHandler extends Handler {

    Activity mActivity;
    Resources mRes;

    public SimpleAlertHandler(Activity activity) {
        mActivity = activity;
        mRes = mActivity.getResources();
    }

    protected void promptDisconnectedEvent(Message msg) {

        ImErrorInfo error = (ImErrorInfo) msg.obj;
        String promptMsg = null;
        if (error != null) {
            promptMsg = mActivity.getString(R.string.signed_out_prompt_with_error,"",
                    ErrorResUtils.getErrorRes(mRes, error.getCode()));
        }
        else
        {
            promptMsg = mActivity.getString(R.string.error);
        }

        if (promptMsg != null)
            showAlert(R.string.error, promptMsg);

    }

    public void registerForBroadcastEvents() {
        ImApp app = (ImApp)mActivity.getApplication();

        app.registerForBroadcastEvent(
                ImApp.EVENT_CONNECTION_DISCONNECTED, this);
    }

    public void unregisterForBroadcastEvents() {
        ImApp app = (ImApp)mActivity.getApplication();

        app.unregisterForBroadcastEvent(
                ImApp.EVENT_CONNECTION_DISCONNECTED, this);
    }

    public void showAlert(int titleId, int messageId) {
        showAlert(mRes.getString(titleId), mRes.getString(messageId));
    }

    public void showAlert(int titleId, CharSequence message) {
        showAlert(mRes.getString(titleId), message);
    }

    public void showAlert(CharSequence title, int messageId) {
        showAlert(title, mRes.getString(messageId));
    }

    public void showAlert(final CharSequence title, final CharSequence message) {

        if (title == null || message == null)
            return;

        if (!title.equals(message)) //sometimes this reads Attention: Attention!
        {
            Toast.makeText(mActivity, title + ": " + message, Toast.LENGTH_SHORT).show();
        }

    }

    public void showServiceErrorAlert(String msg) {
        showAlert(R.string.error, msg);
    }

    public void showContactError(int errorType, ImErrorInfo error, String listName, Contact contact) {
        int id = 0;
        switch (errorType) {
        case ContactListListener.ERROR_LOADING_LIST:
            id = R.string.load_contact_list_failed;
            break;

        case ContactListListener.ERROR_CREATING_LIST:
            id = R.string.add_list_failed;
            break;

        case ContactListListener.ERROR_BLOCKING_CONTACT:
            id = R.string.block_contact_failed;
            break;

        case ContactListListener.ERROR_UNBLOCKING_CONTACT:
            id = R.string.unblock_contact_failed;
            break;
        }

        String errorInfo = ErrorResUtils.getErrorRes(mRes, error.getCode());
        if (id != 0) {
            errorInfo = mRes.getText(id) + "\n" + errorInfo;
        }

        showAlert(R.string.error, errorInfo);
    }

}
