/*
 * Copyright (C) 2008 Esmertec AG. Copyright (C) 2008 The Android Open Source
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
package org.chimple.messenger.plugin.loopback;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.chimple.messenger.plugin.ImConfigNames;
import org.chimple.messenger.plugin.ImPlugin;
import org.chimple.messenger.plugin.ImpsConfigNames;

import java.util.HashMap;
import java.util.Map;

/** Simple example of writing a plug-in for the IM application. */
public class LoopbackImPlugin extends Service implements ImPlugin {
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** The implementation of IImPlugin defined through AIDL. */
    public Map getProviderConfig() {

        HashMap<String, String> config = new HashMap<String, String>();
        // The protocol name MUST be IMPS now.
        config.put(ImConfigNames.PROTOCOL_NAME, "LOOPBACK");
        config.put(ImConfigNames.PLUGIN_VERSION, "0.1");
        config.put(ImpsConfigNames.HOST, "http://xxx.xxxx.xxx");
        config.put(ImpsConfigNames.CUSTOM_PRESENCE_MAPPING,
        "org.chimple.messenger.plugin.loopback.LoopbackPresenceMapping");
        return config;
    }
}
