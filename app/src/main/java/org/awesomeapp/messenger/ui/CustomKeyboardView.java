package org.awesomeapp.messenger.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;

import java.util.List;

/**
 * Created by srikanth on 23/02/17.
 */

public class CustomKeyboardView extends KeyboardView {
    private Typeface emojiFont;
    public CustomKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        emojiFont = Typeface.createFromAsset(context.getAssets(),
                "fonts/AddEmoji.ttf"); //Insert your font here.
    }

    public CustomKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        emojiFont = Typeface.createFromAsset(context.getAssets(),
                "fonts/AddEmoji.ttf"); //Insert your font here.
    }

    @Override
    public void onDraw(Canvas canvas) {
            Paint mPaint = new Paint();
            mPaint.setTextAlign(Paint.Align.CENTER);
            mPaint.setTextSize(80);
            mPaint.setColor(Color.BLACK);

            mPaint.setTypeface(emojiFont);
        List<Keyboard.Key> keys = getKeyboard().getKeys();
        for(Keyboard.Key key: keys) {

            if (key.label != null) {
                String keyLabel = key.label.toString();
                canvas.drawText(keyLabel, key.x + (key.width / 2),
                        key.y + (key.height / 2), mPaint);
            } else if (key.icon != null) {
                key.icon.setBounds(key.x, key.y, key.x + key.width, key.y + key.height);
                key.icon.draw(canvas);
            }
        }

    }
}