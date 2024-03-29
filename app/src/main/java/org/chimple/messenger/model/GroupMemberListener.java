/*
 * Copyright (C) 2007 Esmertec AG. Copyright (C) 2007 The Android Open Source
 * Project
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

package org.chimple.messenger.model;

public interface GroupMemberListener {
    /**
     * Notifies that a contact has joined into this group.
     *
     * @param contact the contact who has joined into this group.
     */
    public void onMemberJoined(ChatGroup group, Contact contact);

    /**
     * Notifies that a contact has left this group.
     *
     * @param contact the contact who has left the group.
     */
    public void onMemberLeft(ChatGroup group, Contact contact);

    /**
     * Called when a previous request to add or remove a member to/from a group
     * failed.
     *
     * @param error the error information
     */
    public void onError(ChatGroup group, ImErrorInfo error);

    /**
     * Called when subject chcanges
     *
     * @param subject the new subject
     */
    public void onSubjectChanged(ChatGroup group, String subject);
}
