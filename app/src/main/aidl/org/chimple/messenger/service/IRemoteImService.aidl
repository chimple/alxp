/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

import org.chimple.messenger.service.IImConnection;
import org.chimple.messenger.service.IConnectionCreationListener;
import org.chimple.messenger.crypto.IOtrKeyManager;

interface IRemoteImService {

    /**
     * Gets a list of all installed plug-ins. Each item is an ImPluginInfo.
     */
    List getAllPlugins();

    /**
     * Register a listener on the service so that the client can be notified when
     * there is a connection be created.
     */
    void addConnectionCreatedListener(IConnectionCreationListener listener);

    /**
     * Unregister the listener on the service so that the client doesn't ware of
     * the connection creation anymore.
     */
    void removeConnectionCreatedListener(IConnectionCreationListener listener);

    /**
     * Create a connection for the given provider.
     */
    IImConnection createConnection(long providerId, long accountId);

    /**
     * Create a connection for the given provider.
     */
    IImConnection getConnection(long providerId);

    /**
     * Get all the active connections.
     */
    List getActiveConnections();

    /**
     * Dismiss all notifications for an IM provider.
     */
    void dismissNotifications(long providerId);

    /**
     * Dismiss notification for the specified chat.
     */
    void dismissChatNotification(long providerId, String username);
    
    
    /**
    * do it
    */
    boolean unlockOtrStore (String password);
    
    /**
    * cleaning up rpocess
    **/
    void setKillProcessOnStop (boolean killProcess);
    
    /**
    * get interface to keymanager/store singleton
    **/
    IOtrKeyManager getOtrKeyManager  ();
    
    /**
    * use debug log to logcat out
    **/
    void enableDebugLogging (boolean debugOn);
    
    /**
    * update settings from OTR
    **/
    void updateStateFromSettings ();

    /**
    * do as the name says!
    **/
    void shutdownAndLock ();
}
