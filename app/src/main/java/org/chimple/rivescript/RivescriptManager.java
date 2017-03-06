package org.chimple.rivescript;

import com.rivescript.RiveScript;
import org.chimple.messenger.ImApp;
import android.content.SharedPreferences;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by srikanth on 09/02/17.
 */

public class RivescriptManager {
    private static RiveScript rs;
    private static String currentChattee;
    private static final String RS_USER = "localuser";

    public static String reply(String chattee, String message) {
        if(!chattee.equals(currentChattee) || rs == null) {
            if(rs != null) {
                SharedPreferences sp = ImApp.getAppContext().getSharedPreferences(currentChattee, 0);
                SharedPreferences.Editor ed = sp.edit();
                for(Map.Entry<String, String> entry : rs.getUservars(RS_USER).entrySet()) {
                    ed.putString(entry.getKey(), entry.getValue());
                }
                ed.commit();
            }
            currentChattee = chattee;
            rs = new RiveScript(ImApp.getAppContext(), true);
            rs.loadDirectory(chattee + "/rive");
            rs.sortReplies();
            rs.setSubroutine("alphabet_teacher", new AlphabetTeacher(ImApp.getAppContext()));
            rs.setSubroutine("change_keyboard", new ChangeKeyboard(ImApp.getAppContext()));
            rs.setSubroutine("phonics_teacher", new PhonicsTeacher(ImApp.getAppContext()));
            SharedPreferences sp = ImApp.getAppContext().getSharedPreferences(currentChattee, 0);
            for(Map.Entry<String, ?> entry : sp.getAll().entrySet()) {
                rs.setUservar(RS_USER, entry.getKey(), entry.getValue().toString());
            }
        }
        return rs.reply(RS_USER, message);
    }

    public static <T> void shuffleArray(T[] ar)
    {
        // If running on Java 6 or older, use `new Random()` on RHS here
        Random rnd = new Random();
        for (int i = ar.length - 1; i > 0; i--)
        {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            T a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

}
