package org.chimple.messenger.tasks;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.chimple.messenger.ImApp;
import org.chimple.messenger.ui.legacy.SignInHelper;
import org.chimple.messenger.ui.onboarding.OnboardingAccount;
import org.chimple.messenger.ui.onboarding.OnboardingManager;

/**
 * Created by Shyamal.Upadhyaya on 07/02/17.
 */

public class RegisterExistingAccountTask extends AsyncTask<String, Void, OnboardingAccount> {

    private ImApp app = null;

    public RegisterExistingAccountTask(ImApp app) {
       this.app = app;
    }
    @Override
    protected OnboardingAccount doInBackground(String... account) {
        try {

            String nickName = account[0];
            String userName = account[1];
            String providerId = account[2];
            String accountId = account[3];
            String password = account[4];
            OnboardingAccount result = OnboardingManager.activateAlreadyRegisteredAccount(this.app, null, nickName, userName, password, providerId, accountId, null, 5222);
            return result;
        }
        catch (Exception e)
        {
            Log.e(ImApp.LOG_TAG, "auto onboarding fail", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(OnboardingAccount account) {
        if(account != null) {
            String mUsername = account.getUsername() + '@' + account.getDomain();

            SignInHelper signInHelper = new SignInHelper(app.getApplicationContext(), null);
            signInHelper.activateAccount(account.getProviderId(),account.getAccountId());
            signInHelper.signIn(account.getPassword(), account.getProviderId(), account.getAccountId(), true);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.app);
            this.app.isXMPPAccountRegisteredInProgress = false;
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isXMPPAccountRegistered", true);
            editor.commit();

        }
    }
}
