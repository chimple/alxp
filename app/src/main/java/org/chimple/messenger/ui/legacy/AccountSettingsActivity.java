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

import org.chimple.messenger.ImApp;
import org.chimple.messenger.provider.Imps;
import org.chimple.messenger.service.ImServiceConstants;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import cn.pedant.SweetAlert.*;

public class AccountSettingsActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    private long mProviderId;
    private long mAccountId;

    private EditTextPreference mXmppResource;
    private EditTextPreference mXmppResourcePrio;
    private EditTextPreference mPort;
    private EditTextPreference mServer;
    private CheckBoxPreference mAllowPlainAuth;
    private CheckBoxPreference mRequireTls;
    private CheckBoxPreference mDoDnsSrv;

    private void setInitialValues() {
        ContentResolver cr = getContentResolver();
        Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString(mProviderId)},null);
        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(pCursor, cr,
                mProviderId, false /* keep updated */, null /* no handler */);
        String text;

        text = settings.getXmppResource();
        mXmppResource.setText(text);
        if (text != null) {
            mXmppResource.setSummary(text);
        }
        text = Integer.toString(settings.getXmppResourcePrio());
        mXmppResourcePrio.setText(text);
        if (text != null) {
            mXmppResourcePrio.setSummary(text);
        }
        text = Integer.toString(settings.getPort());
        mPort.setText(text);
        if (text != null && settings.getPort() != 0) {
            mPort.setSummary(text);
        }
        text = settings.getServer();
        mServer.setText(text);
        if (text != null) {
            mServer.setSummary(text);
        }
        mAllowPlainAuth.setChecked(settings.getAllowPlainAuth());
        mRequireTls.setChecked(settings.getRequireTls());
        mDoDnsSrv.setChecked(settings.getDoDnsSrv());

        settings.close();
    }


    private void deleteAccount ()
    {
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(getString(R.string.delete_account))
           //     .setContentText("Won't be able to recover this account!")
                .setConfirmText(getString(R.string.confirm))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        confirmDeleteAccount();
                        sDialog.dismissWithAnimation();
                    }
                })
                .show();

    }

    private void confirmDeleteAccount ()
    {

        //need to delete
        ((ImApp)getApplication()).deleteAccount(getContentResolver(),mAccountId, mProviderId);

        finish();
    }

    /* save the preferences in Imps so they are accessible everywhere */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

        ContentResolver cr = getContentResolver();
        Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString(mProviderId)},null);

        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                pCursor, cr, mProviderId, true /* don't keep updated */, null /* no handler */);
        String value;

        if (key.equals("pref_account_xmpp_resource")) {
            value = prefs.getString(key, null);
            settings.setXmppResource(value);
            if (value != null) {
                value = value.trim();
                mXmppResource.setSummary(value);
                mXmppResource.setText(value); // In case it was trimmed
            }
        } else if (key.equals("pref_account_xmpp_resource_prio")) {

            value = prefs.getString(key, "20");
            try {
                settings.setXmppResourcePrio(Integer.parseInt(value));
            } catch (NumberFormatException nfe) {
                Toast.makeText(getBaseContext(),
                        getString(R.string.error_account_settings_priority), Toast.LENGTH_SHORT)
                        .show();
            }
            mXmppResourcePrio.setSummary(value);
        } else if (key.equals("pref_account_port")) {
            value = prefs.getString(key, "0");
            try {
                settings.setPort(Integer.parseInt(value));
            } catch (NumberFormatException nfe) {
                Toast.makeText(getBaseContext(), getString(R.string.error_account_settings_port), Toast.LENGTH_SHORT)
                        .show();
            }
            if (settings.getPort() != 0)
                mPort.setSummary(value);
        } else if (key.equals("pref_account_server")) {
            value = prefs.getString(key, null);
            settings.setServer(value);
            if (value != null) {
                value = value.trim();
                mServer.setSummary(value);
                mServer.setText(value); // In case it was trimmed
            }
        } else if (key.equals("pref_security_allow_plain_auth")) {
            settings.setAllowPlainAuth(prefs.getBoolean(key, false));
        } else if (key.equals("pref_security_require_tls")) {
            settings.setRequireTls(prefs.getBoolean(key, true));
        } else if (key.equals("pref_security_tls_cert_verify")) {
            settings.setTlsCertVerify(prefs.getBoolean(key, true));
        } else if (key.equals("pref_security_do_dns_srv")) {
            settings.setDoDnsSrv(prefs.getBoolean(key, true));
        }

        settings.setShowMobileIndicator(true);
        settings.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set dummy name for preferences so that they don't mix with global ones.
        // FIXME we should not be writing these out to a file, since they are written to
        // the DB in onSharedPreferenceChanged().
        getPreferenceManager().setSharedPreferencesName("account");
        addPreferencesFromResource(R.xml.account_settings);

        Intent intent = getIntent();
        mProviderId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, -1);
        mAccountId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, -1);

        if (mProviderId < 0) {
            Log.e(ImApp.LOG_TAG, "AccountSettingsActivity intent requires provider id extra");
            throw new RuntimeException(
                    "AccountSettingsActivity must be created with an provider id");
        }
        mXmppResource = (EditTextPreference) findPreference(("pref_account_xmpp_resource"));
        mXmppResourcePrio = (EditTextPreference) findPreference(("pref_account_xmpp_resource_prio"));
        mPort = (EditTextPreference) findPreference(("pref_account_port"));
        mServer = (EditTextPreference) findPreference(("pref_account_server"));
        mAllowPlainAuth = (CheckBoxPreference) findPreference(("pref_security_allow_plain_auth"));
        mRequireTls = (CheckBoxPreference) findPreference(("pref_security_require_tls"));
        mDoDnsSrv = (CheckBoxPreference) findPreference(("pref_security_do_dns_srv"));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
        root.addView(bar, 0); // insert at top
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        bar.inflateMenu(R.menu.menu_account_settings);
        bar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem arg0) {
                if(arg0.getItemId() == R.id.menu_delete){
                    deleteAccount();
                }
                return false;
            }
        });
    }
    


    @Override
    protected void onResume() {
        super.onResume();

        setInitialValues();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
    }

}
