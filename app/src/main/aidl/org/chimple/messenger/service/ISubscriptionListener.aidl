/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.chimple.messenger.service;

import org.chimple.messenger.model.Contact;

oneway interface ISubscriptionListener {

/**
     * Called when:
     *  <ul>
     *  <li> the request a contact has sent to client
     *  </ul>
     *
     * @see org.chimple.messenger.engine.SubscriptionRequestListener#onSubScriptionRequest(Contact from)
     */
    void onSubScriptionChanged(in Contact from, long providerId, long accountId, int subType, int subStatus);


    /**
     * Called when:
     *  <ul>
     *  <li> the request a contact has sent to client
     *  </ul>
     *
     * @see org.chimple.messenger.engine.SubscriptionRequestListener#onSubScriptionRequest(Contact from)
     */
    void onSubScriptionRequest(in Contact from, long providerId, long accountId);

    /**
     * Called when the request is approved by user.
     *
     * @see org.chimple.messenger.engine.SubscriptionRequestListener#onSubscriptionApproved(String contact)
     */
    void onSubscriptionApproved(in Contact from, long providerId, long accountId);

    /**
     * Called when a subscription request is declined.
     *
     * @see org.chimple.messenger.engine.ContactListListener#onSubscriptionDeclined(String contact)
     */
    void onSubscriptionDeclined(in Contact from, long providerId, long accountId);
}
