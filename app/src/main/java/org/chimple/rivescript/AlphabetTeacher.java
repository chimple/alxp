package org.chimple.rivescript;

import android.content.Context;
import android.content.res.AssetManager;

import com.rivescript.ObjectMacro;
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

public class AlphabetTeacher implements ObjectMacro {
    private List<Character> A_ARRAY;
    private List<String> W_ARRAY;
    private List<String> O_ARRAY;

    public AlphabetTeacher(Context context) {
        A_ARRAY = new ArrayList<Character>();
        W_ARRAY = new ArrayList<String>();
        O_ARRAY = new ArrayList<String>();

        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(context.getAssets().open("rs/alphabet.csv")));

            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                //process line
                String[] content = mLine.split(",");
                if(content.length >= 3) {
                    A_ARRAY.add(content[0].charAt(0));
                    W_ARRAY.add(content[1]);
                    O_ARRAY.add(content[2]);
                }
            }
        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }
    }

    public String call (com.rivescript.RiveScript rs, String[] args) {
        char message = 0;
        if(!args[0].isEmpty()) {
            message = args[0].charAt(0);
        }

        // To get/set user variables for the user, you can use currentUser
        // to find their ID and then use the usual methods.
        String user = rs.currentUser();

        StringBuilder sb = new StringBuilder(getTeaching(rs, user, 1, message++)).append("\n");
        sb.append(getTeaching(rs, user, 2, message++)).append("\n");
        sb.append(getTeaching(rs, user, 3, message++)).append("\n");

        if(Math.random() > 0.5) {
            rs.setUservar(user, "topic", "oquestion1");
            sb.append(rs.getUservar(user, "w1")).append(" ?");
        } else {
            rs.setUservar(user, "topic", "aquestion1");
            sb.append(rs.getUservar(user, "o1")).append(" ?");
        }

        return sb.toString();
    }

    private int getIndexOfChar(char c) {
        int startIndex = 0;
        int endIndex = A_ARRAY.size();
        boolean foundChar = false;
        for(int i = 0; i < A_ARRAY.size(); i++) {
            if(A_ARRAY.get(i).equals(c) && !foundChar) {
                startIndex = i;
                foundChar = true;
            } else if(!A_ARRAY.get(i).equals(c) && foundChar) {
                endIndex = i;
                break;
            }
        }
        return startIndex + (int)(Math.random() * (endIndex - startIndex));
    }

    private String getTeaching(com.rivescript.RiveScript rs, String user, int var, char alpha) {
        int index = getIndexOfChar(alpha);
        rs.setUservar(user, "a" + String.valueOf(var), String.valueOf(A_ARRAY.get(index)));
        rs.setUservar(user, "o" + String.valueOf(var), O_ARRAY.get(index));
        rs.setUservar(user, "w" + String.valueOf(var), W_ARRAY.get(index));

        return String.valueOf(A_ARRAY.get(index)) + " for " + W_ARRAY.get(index) + " " + O_ARRAY.get(index);
    }
}
