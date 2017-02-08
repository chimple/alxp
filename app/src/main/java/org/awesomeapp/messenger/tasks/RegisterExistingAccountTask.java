package org.awesomeapp.messenger.tasks;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.ui.legacy.SignInHelper;
import org.awesomeapp.messenger.ui.onboarding.OnboardingAccount;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;

import static org.awesomeapp.messenger.ImApp.isXMPPAccountRegisteredInProgress;

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
            this.app.isXMPPAccountRegisteredInProgress = true;
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isXMPPAccountRegistered", isXMPPAccountRegisteredInProgress);
            editor.commit();

        }
    }
}
