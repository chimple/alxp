package org.chimple.messenger.tts;

import android.content.Context;
import android.speech.tts.TextToSpeech;

/**
 * Created by Shyamal.Upadhyaya on 15/02/17.
 */

public class CustomTextToSpeech extends TextToSpeech {
    private TextToSpeechCommunicateListener communicateListener;

    public CustomTextToSpeech(Context context, OnInitListener listener) {
        super(context, listener);

    }

    public TextToSpeechCommunicateListener getCommunicateListener() {
        return communicateListener;
    }

    public void setCommunicateListener(TextToSpeechCommunicateListener communicateListener) {
        this.communicateListener = communicateListener;
    }
}

