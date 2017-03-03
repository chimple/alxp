package org.chimple.rivescript;

import android.content.Context;
import android.content.res.AssetManager;

import com.rivescript.ObjectMacro;

import org.chimple.messenger.ImApp;
import org.chimple.messenger.ui.ConversationView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by srikanth on 08/02/17.
 */

public class ChangeKeyboard implements ObjectMacro {
    public ChangeKeyboard(Context context) {
    }

    public String call(com.rivescript.RiveScript rs, String[] args) {
        ImApp.sImApp.displayKeyBoard(ConversationView.DEFAULT_KEYBOARD_TYPE);
        return "";
    }
}
