package org.chimple.messenger.tasks;

import android.content.ContentResolver;
import android.database.Cursor;

import org.chimple.messenger.ImApp;
import org.chimple.messenger.model.WordInformation;
import org.chimple.messenger.provider.Imps;

/**
 * Created by Shyamal.Upadhyaya on 09/02/17.
 */

public class FetchWordProcessor {
    private long wordId = -1;
    private String word = null;
    private ImApp mApp;
    ContentResolver mResolver = null;


    public FetchWordProcessor(ImApp app, String word) {
        this.mApp = app;
        this.mResolver = mApp.getContentResolver();
        this.word = word;
    }

    public WordInformation fetchWord() {
        WordInformation wordInformation = new WordInformation();
        wordInformation.setName(this.word);
        String where = Imps.Word.NAME + " = ?";
        String[] selectionArgs = new String[]{this.word.toLowerCase()};

        Cursor c = mResolver.query(Imps.Word.CONTENT_URI, null, where,
                selectionArgs, null);

        try {
            if (c != null && c.getCount() > 0) {

                if (c.moveToFirst()) {
                    System.out.println("name " + c.getString(1));
                    System.out.println("meaning " + c.getString(2));
                    System.out.println("imageUrl " + c.getString(3));
                    System.out.println("sp name " + c.getString(4));
                    System.out.println("sp meaning " + c.getString(5));

                    wordInformation.setName(""+c.getString(1));
                    wordInformation.setMeaning(""+c.getString(2));
                    wordInformation.setImageUrl(""+c.getString(3));
                    wordInformation.setSpName(""+c.getString(4));
                    wordInformation.setSpMeaning(""+c.getString(5));
                }
            }

        } finally {
            c.close();
        }

        return wordInformation;
    }
}
