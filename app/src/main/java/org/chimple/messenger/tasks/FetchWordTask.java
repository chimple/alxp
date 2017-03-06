package org.chimple.messenger.tasks;

import android.app.Dialog;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.chimple.messenger.ImApp;
import org.chimple.messenger.model.WordInformation;

import java.util.Locale;

import im.zom.messenger.R;

/**
 * Created by Shyamal.Upadhyaya on 09/02/17.
 */

public class FetchWordTask extends AsyncTask<String, Void, WordInformation> {

    private ImApp app = null;
    private String word = null;
    private Dialog dialog = null;
    private FetchWordProcessor fetchWordProcessor = null;
    private static final String DEBUG_TAG = "FetchWord";

    public FetchWordTask(ImApp app, String word, Dialog dialog) {
        this.app = app;
        this.word = word;
        this.dialog = dialog;
        fetchWordProcessor = new FetchWordProcessor(app, word);
    }


    @Override
    protected WordInformation doInBackground(String... params) {
        WordInformation information = fetchWordProcessor.fetchWord();

        return information;
    }

    @Override
    protected void onPostExecute(WordInformation wordInformation) {

        TextView engword = (TextView) this.dialog.findViewById(R.id.engword);
        final ImageView engvoice = (ImageView) this.dialog.findViewById(R.id.engvoice);
        TextView engmeaning = (TextView) this.dialog.findViewById(R.id.engmeaning);
        ImageView engimage = (ImageView) this.dialog.findViewById(R.id.engimage);
        TextView otherword = (TextView) this.dialog.findViewById(R.id.otherword);
        ImageView othervoice = (ImageView) this.dialog.findViewById(R.id.othervoice);
        TextView othermeaning = (TextView) this.dialog.findViewById(R.id.othermeaning);

        engword.setText(wordInformation.getName().toString());
        engmeaning.setText(wordInformation.getMeaning().toString());
        otherword.setText(wordInformation.getSpName().toString());
        othermeaning.setText(wordInformation.getSpMeaning().toString());

//        othervoice.setVisibility(View.GONE);
//        engvoice.setVisibility(View.GONE);
        Glide.with(this.dialog.getContext()).load(wordInformation.getImageUrl()).into(engimage);

        engvoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("english speaking cat");
                app.speakOut("cat", new Locale("en", "US"));
            }
        });

        othervoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("spanish speaking cat");
                app.speakOut("cat", new Locale("es", "US"));
            }
        });

        this.dialog.show();
    }
}
