package org.awesomeapp.messenger.tasks;

import android.os.AsyncTask;

import org.awesomeapp.messenger.ImApp;
import org.awesomeapp.messenger.model.WordInformation;

/**
 * Created by Shyamal.Upadhyaya on 09/02/17.
 */

public class FetchWordTask extends AsyncTask<String, Void, WordInformation> {

    private ImApp app = null;
    private String word = null;
    private FetchWordProcessor fetchWordProcessor = null;
    private static final String DEBUG_TAG = "FetchWord";

    public FetchWordTask(ImApp app, String word) {
        this.app = app;
        this.word = word;
        fetchWordProcessor = new FetchWordProcessor(app, word);
    }


    @Override
    protected WordInformation doInBackground(String... params) {
        WordInformation information = fetchWordProcessor.fetchWord();
        return information;
    }

    @Override
    protected void onPostExecute(WordInformation wordInformation) {

    }
}
