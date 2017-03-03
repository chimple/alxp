package org.chimple.messenger.tts;

import java.util.List;
import java.util.Locale;

/**
 * Created by Shyamal.Upadhyaya on 14/02/17.
 */

public interface TextToSpeechEventListener {

    public void onSuccessfulInitiated(CustomTextToSpeech tts);

    public void onDownloadRequiredForLanguageData(List<Locale> missingLocals);

    public void onDownloadNotCompletedForLanguageData();

    public void onErrorToInitialize();

}
