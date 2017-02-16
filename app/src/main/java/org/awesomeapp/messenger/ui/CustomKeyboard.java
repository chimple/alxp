package org.awesomeapp.messenger.ui;

/**
 * Created by User on 07-02-2017.
 */

import android.app.Activity;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.text.Editable;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import im.zom.messenger.R;

public class CustomKeyboard {

    /** A link to the KeyboardView that is used to render this CustomKeyboard. */
    private KeyboardView mKeyboardView;
    private HashMap<Integer, String> sentence_HashMap ;
    private String mInputType;

    /** A link to the activity that hosts the {@link #mKeyboardView}. */
    private Activity     mHostActivity;

    private ConversationView mConversationView;

    /** The key (code) handler. */
    private OnKeyboardActionListener mOnKeyboardActionListener = new OnKeyboardActionListener() {

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
            // NOTE We can say '<Key android:codes="49,50" ... >' in the xml file; all codes come in keyCodes, the first in this list in primaryCode
            // Get the EditText and its Editable
            View focusCurrent = mHostActivity.getWindow().getCurrentFocus();
           // if( focusCurrent==null || focusCurrent.getClass()!=EditText.class ) return;
            EditText edittext = (EditText) focusCurrent;
            if (mInputType.equals("word")){
                Editable editable = edittext.getText();
                edittext.setText(editable.toString() + sentence_HashMap.get(primaryCode).toString());
                mConversationView.sendMessage();
//                int start = edittext.getSelectionStart();
//                editable.insert(start,sentence_HashMap.get(primaryCode).toString());
            }
            else {
                Editable editable = edittext.getText();
                int start = edittext.getSelectionStart();
                editable.insert(start, Character.toString((char) primaryCode));
                mConversationView.sendMessage();
            }
            // Apply the key to the edittext
//            if( primaryCode==CodeCancel ) {
//                hideCustomKeyboard();
//            } else if( primaryCode==CodeDelete ) {
//                if( editable!=null && start>0 ) editable.delete(start - 1, start);
//            } else if( primaryCode==CodeClear ) {
//                if( editable!=null ) editable.clear();
//            } else if( primaryCode==CodeLeft ) {
//                if( start>0 ) edittext.setSelection(start - 1);
//            } else if( primaryCode==CodeRight ) {
//                if (start < edittext.length()) edittext.setSelection(start + 1);
//            } else if( primaryCode==CodeAllLeft ) {
//                edittext.setSelection(0);
//            } else if( primaryCode==CodeAllRight ) {
//                edittext.setSelection(edittext.length());
//            } else if( primaryCode==CodePrev ) {
////                View focusNew= edittext.focusSearch(View.FOCUS_BACKWARD);
////                if( focusNew!=null ) focusNew.requestFocus();
//            } else if( primaryCode==CodeNext ) {
////                View focusNew= edittext.focusSearch(View.FOCUS_FORWARD);
////                if( focusNew!=null ) focusNew.requestFocus();
//            } else { // insert character
                //editable.insert(start, Character.toString((char) primaryCode));
//            }
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
     * Note that to enable EditText's to use this custom keyboard, call the {@link #registerEditText(int)}.
     *
     * @param host The hosting activity.
     * @param viewid The id of the KeyboardView.
     * @param layoutid The id of the xml file containing the keyboard layout.
     */
    public CustomKeyboard(Activity host, int viewid, int layoutid) {
        mHostActivity= host;
        mKeyboardView= (KeyboardView)mHostActivity.findViewById(viewid);
        mKeyboardView.setKeyboard(new Keyboard(mHostActivity, layoutid));


        mKeyboardView.setPreviewEnabled(false); // NOTE Do not show the preview balloons
        mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);
        // Hide the standard keyboard initially
        mHostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);



        sentence_HashMap = new HashMap<Integer, String>();
    }

    /** Returns whether the CustomKeyboard is visible. */
    public boolean isCustomKeyboardVisible() {
        return mKeyboardView.getVisibility() == View.VISIBLE;
    }

    /** Make the CustomKeyboard visible, and hide the system keyboard for view v. */
    public void showCustomKeyboard( View v ) {
        mKeyboardView.setVisibility(View.VISIBLE);
        mKeyboardView.setEnabled(true);
        if( v!=null ) ((InputMethodManager)mHostActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    /** Make the CustomKeyboard invisible. */
    public void hideCustomKeyboard() {
        mKeyboardView.setVisibility(View.GONE);
        mKeyboardView.setEnabled(false);
    }

    /**
     * Register <var>EditText<var> with resource id <var>resid</var> (on the hosting activity) for using this custom keyboard.
     *
     * @param resid The resource id of the EditText that registers to the custom keyboard.
     */
    public EditText registerEditText(int resid) {
        // Find the EditText 'resid'
        EditText edittext= (EditText)mHostActivity.findViewById(resid);
        // Make the custom keyboard appear
        edittext.setOnFocusChangeListener(new OnFocusChangeListener() {
            // NOTE By setting the on focus listener, we can show the custom keyboard when the edit box gets focus, but also hide it when the edit box loses focus
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if( hasFocus ) showCustomKeyboard(v); else hideCustomKeyboard();
            }
        });
        edittext.setOnClickListener(new OnClickListener() {
            // NOTE By setting the on click listener, we can show the custom keyboard again, by tapping on an edit box that already had focus (but that had the keyboard hidden).
            @Override public void onClick(View v) {
                showCustomKeyboard(v);
            }
        });
        // Disable standard keyboard hard way
        // NOTE There is also an easy way: 'edittext.setInputType(InputType.TYPE_NULL)' (but you will not have a cursor, and no 'edittext.setCursorVisible(true)' doesn't work )
        edittext.setOnTouchListener(new OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                EditText edittext = (EditText) v;
                int inType = edittext.getInputType();       // Backup the input type
                edittext.setInputType(InputType.TYPE_NULL); // Disable standard keyboard
                edittext.onTouchEvent(event);               // Call native handler
                edittext.setInputType(inType);              // Restore input type
                return true; // Consume touch event
            }
        });
        // Disable spell check (hex strings look like words to Android)
        edittext.setInputType(edittext.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        return edittext;
    }

    public void dyanamicKeyBoard ( String[] userKeys){

        Keyboard updatedKeyboard = null;
        if (userKeys[0].length() < 3){
            if (userKeys.length >6 && userKeys.length < 10) // for 9 keys
                updatedKeyboard = new Keyboard(mHostActivity,R.xml.custom_keyboard);

            if(userKeys.length > 3 && userKeys.length < 7) { //for 6 keys
                updatedKeyboard = new Keyboard(mHostActivity, R.xml.custom_keyboard_2x3);
                mKeyboardView = (KeyboardView) mHostActivity.findViewById(R.id.keyboardview_2x3);
            }
            if (userKeys.length < 4 ){ // for 3 keys
                updatedKeyboard = new Keyboard(mHostActivity,R.xml.custom_keyboard_1x3);
                mKeyboardView= (KeyboardView)mHostActivity.findViewById(R.id.keyboardview_1x3);
            }

            mKeyboardView.setKeyboard(updatedKeyboard);
            mKeyboardView.setPreviewEnabled(false);
            mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);
            mKeyboardView.setVisibility(View.VISIBLE);
            mKeyboardView.setEnabled(true);
            mHostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            mHostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            //mKeyboardView.keyTextSize
            List<Keyboard.Key> keys = mKeyboardView.getKeyboard().getKeys();

            int count = 0;
            for (Keyboard.Key key : keys) {
                int [] codes = {49};
                codes[0] = userKeys[count].charAt(0);
                key.codes = codes;
                key.label = userKeys[count++];
            }
        }
        else{
            mInputType = "word";
            sentence_HashMap.clear();
            updatedKeyboard = new Keyboard(mHostActivity,R.xml.custom_word_keyboard);
            mKeyboardView = (KeyboardView) mHostActivity.findViewById(R.id.keyboardview_1x3);

            mKeyboardView.setKeyboard(updatedKeyboard);
            mKeyboardView.setPreviewEnabled(false);
            mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);
            mKeyboardView.setVisibility(View.VISIBLE);
            mKeyboardView.setEnabled(true);
            mHostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            mHostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            List<Keyboard.Key> keys = mKeyboardView.getKeyboard().getKeys();

            int count = 0;
            for (Keyboard.Key key : keys) {
                int [] codes = {49};
                codes[0] = count;
                key.codes = codes;
                key.label = userKeys[count];
                sentence_HashMap.put(count,userKeys[count]);
                count++;
            }
        }

    }

    public void showDefalutKeyboard(){
       // mHostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    public void setConversationViewObject(ConversationView conversationViewObject){
        mConversationView = conversationViewObject;
    }
}


// NOTE How can we change the background color of some keys (like the shift/ctrl/alt)?
// NOTE What does android:keyEdgeFlags do/mean

