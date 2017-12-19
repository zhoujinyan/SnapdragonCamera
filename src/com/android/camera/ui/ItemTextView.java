package com.android.camera.ui;

import android.content.Context;
import android.widget.TextView;

/**
 * Created by denny on 2016/11/15.
 */

public class ItemTextView extends TextView{

    private int index;

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public ItemTextView(Context context) {
        super(context);
    }
}
