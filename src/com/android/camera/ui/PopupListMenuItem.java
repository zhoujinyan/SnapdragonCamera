/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.camera.CameraSettings;
import com.android.camera.ListPreference;
import com.android.camera.IconListPreference;
import org.codeaurora.snapcam.R;
import android.widget.LinearLayout;
import android.view.MotionEvent;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;



/**
 * A one-line camera setting could be one of three types: knob, switch or
 * restore preference button. The setting includes a title for showing the
 * preference title which is initialized in the SimpleAdapter. A knob also
 * includes (ex: Picture size), a previous button, the current value (ex: 5MP),
 * and a next button. A switch, i.e. the preference RecordLocationPreference,
 * has only two values on and off which will be controlled in a switch button.
 * Other setting popup window includes several InLineSettingItem items with
 * different types if possible.
 */
public class PopupListMenuItem extends RelativeLayout {
    private static final String TAG = "PopupListMenuItem";
    private Listener mListener;
    protected ListPreference mPreference;
	
	private int mSelectedColor;
    private int mUnselectedColor;
	  /** 当前的选择索引 */
    private int mSelectedIndex = 0;
    private int mItemSize;
	private boolean mUnusable = true;
    private int img_header_id;
	private LinearLayout mContainer;

    static public interface Listener {
        public void onListPrefChanged(ListPreference pref);
    }

	public void setSettingChangedListener(Listener listener) {
        mListener = listener;
    }

    public PopupListMenuItem(Context context, AttributeSet attrs) {
        super(context, attrs);
		Resources res = getResources();
        Resources.Theme theme = context.getTheme();
        mSelectedColor = res.getColor(R.color.mode_selected_text_color,theme);
        mUnselectedColor = res.getColor(R.color.mode_unselect_text_color,theme);
    }

   private OnClickListener mItemClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            ItemTextView item = (ItemTextView) v;
            if(mSelectedIndex == item.getIndex()) {
                return;
            }
            setSelection(item.getIndex());
		   Log.d(TAG,"setSelection getIndex:"+item.getIndex());
        }
    };

    private View.OnTouchListener mItemTouch = new View.OnTouchListener() {
        private long startTime;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
       		 Log.d(TAG,"onClick getAction:"+event.getAction());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startTime = System.currentTimeMillis();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (System.currentTimeMillis() - startTime < 200) {
                    mItemClick.onClick(v);
					Log.d(TAG,"onClick V:"+v);
                }

            }
            return true;
        }
    };
	

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

	@Override
	   protected void onLayout(boolean changed, int l, int t, int r, int b) {
		   super.onLayout(changed, l, t, r, b);
	
		   // 页面重绘的时需要将translation置0，否则计算位置时会加上，导致位置错乱
		   mContainer.setTranslationX(0f);
	
		   // 中心位置再偏移n * size + size/2(半边)
		   int center = getWidth() / 2;
		   // int left = center - ((mSelectedIndex * mItemSize) + mItemSize / 2);
		   int left = center - (mItemSize + mItemSize / 2) - 30;
		   int right = left + mContainer.getWidth();
		   mContainer.layout(left,mContainer.getTop(),right, mContainer.getBottom());
	   }

    public void initialize(ListPreference preference, int img_header_id) {
        if (preference == null)
            return;
		this.img_header_id = img_header_id;
        this.mPreference = preference;
		this.mSelectedIndex = mPreference.getCurrentIndex();
		createItemView();
    }

	public void setUnusable(boolean v){
		  this.mUnusable = v;
	}

    public void setSelection(int targetIndex) {
        if(mUnusable || mSelectedIndex == targetIndex) {
            return;
        }
        renderSelection(targetIndex);

    }

	public void renderSelection(int targetIndex) {
		Log.d(TAG,"renderSelection targetIndex:"+targetIndex);
        renderItemColor(mSelectedIndex,targetIndex);
        mSelectedIndex = targetIndex;
		changeIndex(mSelectedIndex);
    }

	private void renderItemColor(int fromIndex,int toIndex) {
		  ((TextView) mContainer.getChildAt(fromIndex)).setTextColor(mUnselectedColor);
		  ((TextView) mContainer.getChildAt(toIndex)).setTextColor(mSelectedColor);
	}

	 void createItemView() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mItemSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, metrics);
        int width = mItemSize;
        int height = width / 4 * 3;

        ImageView img_header = new ImageView(getContext());
        img_header.setScaleType(ImageView.ScaleType.CENTER);
        img_header.setLayoutParams(new LayoutParams(width- 30, LayoutParams.MATCH_PARENT));
        img_header.setImageResource(img_header_id);
        this.addView(img_header);

        mContainer = new LinearLayout(getContext());
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mContainer.setOrientation(LinearLayout.HORIZONTAL);
        mContainer.setLayoutParams(params);

		 CharSequence[] entries = mPreference.getEntries();
        for (int i = 0; i < entries.length; i++) {
			Log.d(TAG,"createItemView KEY:"+mPreference.getKey()+",entries:"+entries[i]+",mSelectedIndex:"+mSelectedIndex);
            ItemTextView item = new ItemTextView(getContext());
            item.setTextColor(mSelectedIndex == i ? mSelectedColor : mUnselectedColor);
            item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            item.setText(entries[i]);
            item.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            item.setWidth(width);
            item.setHeight(height);
            item.setIndex(i);
            item.setOnTouchListener(mItemTouch);
            mContainer.addView(item);
        }

        this.addView(mContainer);
	}
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mContainer.onTouchEvent(event);
    }

    protected boolean changeIndex(int index) {
		mPreference.setValueIndex(index);
        if (mListener != null) {
            mListener.onListPrefChanged(mPreference);
        }

        return true;
    }


    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        event.getText().add(mPreference.getTitle() + mPreference.getEntry());
        return true;
    }

}
