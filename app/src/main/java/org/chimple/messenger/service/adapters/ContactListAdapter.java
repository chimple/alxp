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

package org.chimple.messenger.service.adapters;

import org.chimple.messenger.model.Address;
import org.chimple.messenger.model.Contact;
import org.chimple.messenger.model.ContactList;
import org.chimple.messenger.model.ImErrorInfo;
import org.chimple.messenger.model.ImException;

import org.chimple.messenger.service.RemoteImService;

public class ContactListAdapter extends org.chimple.messenger.service.IContactList.Stub {
    private ContactList mAdaptee;
    private long mDataBaseId;

    public ContactListAdapter(ContactList adaptee, long dataBaseId) {
        mAdaptee = adaptee;
        mDataBaseId = dataBaseId;
    }

    public long getDataBaseId() {
        return mDataBaseId;
    }

    public Address getAddress() {
        return mAdaptee.getAddress();
    }

    public int addContact(String address, String nickname) {
        if (address == null) {
            RemoteImService.debug("Address can't be null!");
            return ImErrorInfo.ILLEGAL_CONTACT_ADDRESS;
        }

        try {
            mAdaptee.addContact(address, nickname);
        } catch (IllegalArgumentException e) {
            return ImErrorInfo.ILLEGAL_CONTACT_ADDRESS;
        } catch (ImException e) {
            return ImErrorInfo.NETWORK_ERROR;
        }

        return ImErrorInfo.NO_ERROR;
    }

    public String getName() {
        return mAdaptee.getName();
    }

    public int removeContact(String address) {
        Contact contact = mAdaptee.getContact(address);
        if (contact == null) {
            return ImErrorInfo.ILLEGAL_CONTACT_ADDRESS;
        }

        try {
            mAdaptee.removeContact(contact);
        } catch (ImException e) {
            return e.getImError().getCode();
        }

        return ImErrorInfo.NO_ERROR;
    }

    public void setDefault(boolean isDefault) {
        mAdaptee.setDefault(isDefault);
    }

    public boolean isDefault() {
        return mAdaptee.isDefault();
    }

    public void setName(String name) {
        if (name == null) {
            RemoteImService.debug("Name can't be null!");
            return;
        }

        mAdaptee.setName(name);
    }
}
