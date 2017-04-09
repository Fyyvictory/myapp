package com.example.imdemo.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Chronometer;

/**
 * Created by SH on 2017/1/10.
 */

public class MyChronometer extends Chronometer{
    public MyChronometer(Context context) {
        super(context);
    }

    public MyChronometer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyChronometer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        visibility = VISIBLE;
        super.onWindowVisibilityChanged(visibility);
    }
}
