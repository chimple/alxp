package org.chimple.messenger.tts;

import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.LANG_AVAILABLE;
import static android.speech.tts.TextToSpeech.LANG_MISSING_DATA;
import static android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED;

/**
 * Created by Shyamal.Upadhyaya on 14/02/17.
 */

public class TextToSpeechRecognizer {

    private static final String TAG = "TextToSpeechRecognizer";

    private CustomTextToSpeech tts;

    private Context context;

    private List<Locale> missingDownloadedTTSLanguages;

    private TextToSpeechEventListener listener;

    public TextToSpeechRecognizer(Context context, final List<Locale> supportedLocals,
                                  TextToSpeechEventListener listener)
    {
        this.missingDownloadedTTSLanguages = new ArrayList<Locale>();
        this.context = context;
        this.listener = listener;
        initializeTextToSpeech(supportedLocals);
    }


    private void initializeTextToSpeech(final List<Locale> supportedLocals)
    {
        tts = new CustomTextToSpeech(context, new CustomTextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                if (status == TextToSpeech.SUCCESS)
                {
                    afterSuccessOnInit(supportedLocals);
                } else
                {
                    Log.e(TAG, "error creating text to speech");
                    listener.onErrorToInitialize();
                }
            }
        });
    }

    private void checkIfLocalPackIsDownloaded(Locale locale) {
        switch (tts.isLanguageAvailable(locale))
        {
            case LANG_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                Log.d(TAG, "SUPPORTED");
                tts.setLanguage(locale);
                listener.onSuccessfulInitiated(tts);
                break;
            case LANG_MISSING_DATA:
                Log.d(TAG, "MISSING_DATA");
                missingDownloadedTTSLanguages.add(locale);
                break;
            case LANG_NOT_SUPPORTED:
                Log.d(TAG, "NOT SUPPORTED");
                break;
        }
    }

    private void afterSuccessOnInit(final List<Locale> supportedLocals)
    {
        Iterator<Locale> sIt = supportedLocals.iterator();
        while (sIt.hasNext()) {
            Locale l = sIt.next();
            checkIfLocalPackIsDownloaded(l);
        }

        if(missingDownloadedTTSLanguages != null && missingDownloadedTTSLanguages.size() > 0) {
            if (TTSDataInstalledBroadcastReceiver.isWaiting(context))
            {
                listener.onDownloadNotCompletedForLanguageData();
            } else
            {
                listener.onDownloadRequiredForLanguageData(missingDownloadedTTSLanguages);
            }
        }
    }


    public static void installLanguageData(final Context context)
    {
        TTSDataInstalledBroadcastReceiver.setWaiting(context, true);
        Intent installIntent = new Intent();
        installIntent.setAction(
                TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        context.startActivity(installIntent);
    }
}
