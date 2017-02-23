package org.awesomeapp.messenger.ui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import java.util.List;

import im.zom.messenger.R;

/**
 * Created by user on 15-02-2017.
 */

public class SpeechToTextKeyboard {
    /** A link to the KeyboardView that is used to render this SpeechToTextKeyboard. */
    KeyboardView mKeyboardView;
    ConversationView conversationView;
    Keyboard.Key _mikeBtn;
    List<Keyboard.Key> keys;
    /** A link to the activity that hosts the {@link #mKeyboardView}. */
    private Activity mHostActivity;

    /** The key (code) handler. */
    private KeyboardView.OnKeyboardActionListener mOnKeyboardActionListener = new KeyboardView.OnKeyboardActionListener() {

//        public final static int CodeDelete   = -5; // Keyboard.KEYCODE_DELETE
//        public final static int CodeCancel   = -3; // Keyboard.KEYCODE_CANCEL
//        public final static int CodePrev     = 55000;
//        public final static int CodeAllLeft  = 55001;
//        public final static int CodeLeft     = 55002;
//        public final static int CodeRight    = 55003;
//        public final static int CodeAllRight = 55004;
//        public final static int CodeNext     = 55005;
//        public final static int CodeClear    = 55006;

        @Override public void onKey(int primaryCode, int[] keyCodes) {

            View focusCurrent = mHostActivity.getWindow().getCurrentFocus();

            if(primaryCode == -101)
            {
//                    Keyboard currentKeyboard = mKeyboardView.getKeyboard();
//                    keys = currentKeyboard.getKeys();
//                    Drawable d = mHostActivity.getResources().getDrawable(R.drawable.microphone_process);
//                    _mikeBtn = keys.get(0);
//                     keys.get(0).icon = d;
                    conversationView.mActivity.mSpeechRecognizer.startListening(conversationView.mActivity.mSpeechRecognizerIntent);
            }
            else if(primaryCode == -102)
            {
                if(conversationView.mComposeMessage.getText().length()>0)
                    conversationView.mComposeMessage.setText(conversationView.mComposeMessage.getText().toString().substring(0, conversationView.mComposeMessage.getText().toString().length() - 1));
            }

        }

        @Override public void onPress(int arg0) {
        }

        @Override public void onRelease(int primaryCode) {
        }

        @Override public void onText(CharSequence text) {
        }

        @Override public void swipeDown() {
        }

        @Override public void swipeLeft() {
        }

        @Override public void swipeRight() {
        }

        @Override public void swipeUp() {
        }
    };

    /**
     * Create a custom keyboard, that uses the KeyboardView (with resource id <var>viewid</var>) of the <var>host</var> activity,
     * and load the keyboard layout from xml file <var>layoutid</var> (see {@link Keyboard} for description).
     * Note that the <var>host</var> activity must have a <var>KeyboardView</var> in its layout (typically aligned with the bottom of the activity).
     * Note that the keyboard layout xml file may include key codes for navigation; see the constants in this class for their values.
     * Note that to enable EditText's to use this custom keyboard, call the {@link #(int)}.
     *
     * @param host The hosting activity.
     * @param viewid The id of the KeyboardView.
     * @param layoutid The id of the xml file containing the keyboard layout.
     */
    public SpeechToTextKeyboard(Activity host, int viewid, int layoutid, ConversationView _conversationView) {
        conversationView = _conversationView;
        mHostActivity= host;
        mKeyboardView= (KeyboardView)mHostActivity.findViewById(viewid);
        mKeyboardView.setKeyboard(new Keyboard(mHostActivity, layoutid));

        mKeyboardView.setPreviewEnabled(false); // NOTE Do not show the preview balloons
        mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);
        // Hide the standard keyboard initially
        mHostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /** Returns whether the SpeechToTextKeyboard is visible. */
    public boolean isSpeechToTextKeyboardVisible() {
        return mKeyboardView.getVisibility() == View.VISIBLE;
    }

    /** Make the SpeechToTextKeyboard visible, and hide the system keyboard for view v. */
    public void showSpeechToTextKeyboard() {
        mKeyboardView.setVisibility(View.VISIBLE);
        mKeyboardView.setEnabled(true);
    }

    /** Make the SpeechToTextKeyboard invisible. */
    public void hideSpeechToTextKeyboard() {
        mKeyboardView.setVisibility(View.GONE);
        mKeyboardView.setEnabled(false);
    }

    /**
     * Register <var>EditText<var> with resource id <var>resid</var> (on the hosting activity) for using this custom keyboard.
     *
     * @param resid The resource id of the EditText that registers to the custom keyboard.
     */
    public ImageButton registerButton(int resid) {
        // Find the EditText 'resid'
        ImageButton imgButton = (ImageButton) mHostActivity.findViewById(resid);
        // Make the custom keyboard appear

        imgButton.setOnClickListener(new View.OnClickListener() {
            // NOTE By setting the on click listener, we can show the custom keyboard again, by tapping on an edit box that already had focus (but that had the keyboard hidden).
            @Override public void onClick(View v) {

                //showSpeechToTextKeyboard(v);
            }
        });
        // Disable standard keyboard hard way
        // NOTE There is also an easy way: 'edittext.setInputType(InputType.TYPE_NULL)' (but you will not have a cursor, and no 'edittext.setCursorVisible(true)' doesn't work )
/*        edittext.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                EditText edittext = (EditText) v;
                int inType = edittext.getInputType();       // Backup the input type
                edittext.setInputType(InputType.TYPE_NULL); // Disable standard keyboard
                edittext.onTouchEvent(event);               // Call native handler
                edittext.setInputType(inType);              // Restore input type
                return true; // Consume touch event
            }
        });
*/        // Disable spell check (hex strings look like words to Android)
//        edittext.setInputType(edittext.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        return imgButton;
    }
}


// NOTE How can we change the background color of some keys (like the shift/ctrl/alt)?
// NOTE What does android:keyEdgeFlags do/mean

