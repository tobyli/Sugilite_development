package edu.cmu.hcii.sugilite.sharing;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;

import java.util.LinkedList;
import java.util.List;

/**
 * @author toby
 * @date 10/2/19
 * @time 6:24 PM
 */
public class TempUserAccountNameManager {
    AccountManager manager;

    public TempUserAccountNameManager(Context context) {
        this.manager = AccountManager.get(context);
    }

    /**
     * try to get the user's name in the Google account
     * @return
     */
    public String getUserGoogleName(){
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> usernames = new LinkedList<String>();

        for (Account account : accounts) {
            usernames.add(account.name);
        }

        if (usernames.size() > 0) {
            return usernames.get(0);
        }

        return null;
    }

    /**
     * get the ANDROID_ID of the phone
     * @return
     */
    public String getAndroidID(){
        return Settings.Secure.ANDROID_ID;
    }

    public String getBestUserName(){
        return getUserGoogleName() == null ? getAndroidID() : getUserGoogleName();
    }

}
