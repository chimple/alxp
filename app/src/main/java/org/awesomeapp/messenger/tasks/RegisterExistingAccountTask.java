package org.awesomeapp.messenger.tasks;

import android.os.AsyncTask;
import android.util.Log;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.ui.legacy.SignInHelper;
import org.awesomeapp.messenger.ui.onboarding.OnboardingAccount;
import org.awesomeapp.messenger.ui.onboarding.OnboardingManager;

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
            OnboardingAccount result = OnboardingManager.registerAccount(this.app, null, nickName, userName, null, null, 5222, false);
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
        }
    }
}
