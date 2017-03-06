package org.chimple.rivescript;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.rivescript.ObjectMacro;

import org.chimple.messenger.ImApp;
import org.chimple.messenger.provider.Imps;
import org.chimple.messenger.ui.ConversationView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import timber.log.Timber;

/**
 * Created by srikanth on 08/02/17.
 */

public class PhonicsTeacher implements ObjectMacro {

    public PhonicsTeacher(Context context) {

    }

    public String call (com.rivescript.RiveScript rs, String[] args) {
        String user = rs.currentUser();
        String[] phonics = getPhonics();
        StringBuilder sb = new StringBuilder();
        if(phonics[0] != null) {
            sb.append(phonics[0]).append("\n");
        }
        if(phonics[1] != null) {
            sb.append(phonics[1]).append("\n");
        }
        if(phonics[2] != null) {
            sb.append(phonics[2]).append("\n");
        }

        return sb.toString();
    }

    private String[] getPhonics() {
        String[] retStr = new String[3];
        Cursor mCursor = ImApp.getAppContext().getContentResolver().query(Imps.PhonicsList.CONTENT_URI, null, null, null, null);
        if (mCursor != null) {
            int mCount = mCursor.getCount();
            double r = Math.random();
            int row = (int)(mCount * r);
            if(mCursor.moveToPosition(row)) {
                String phonetic = mCursor.getString(mCursor.getColumnIndex(Imps.PhonicsList.PHONETIC));
                String where = Imps.Phonic.PHONETIC + " = ?";
                String[] selectionArgs = new String[]{phonetic};
                Cursor pCursor = ImApp.getAppContext().getContentResolver().query(Imps.Phonic.CONTENT_URI, null, where, selectionArgs, null);
                if (pCursor != null && pCursor.getCount() >= 3) {
                    for (int i = 0; i < 3; i++) {
//                        int pRow = (int)(pCursor.getCount() * Math.random());
                        if (pCursor.moveToPosition(i)) {
                            retStr[i] = pCursor.getString(pCursor.getColumnIndex(Imps.Phonic.WORD));
                        }
                    }
                } else {
                    Timber.d("Failed getting data from phonics for " + phonetic);
                }
                pCursor.close();
            }
        } else {
            Timber.d("Failed getting data from phonicsList");
        }
        mCursor.close();
        return retStr;
    }
}
