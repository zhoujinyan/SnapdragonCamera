package com.android.camera.widget;

import android.animation.Animator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.util.Log;
import android.view.animation.BounceInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

import org.codeaurora.snapcam.R;

import com.android.camera.ui.ItemTextView;

/**
 * Created by denny on 2016/11/14.
 */

public class SwitcherBar extends LinearLayout {

    private String[] mValues;

    private int img_header_id;

    private int mSelectedColor;
    private int mUnselectedColor;

    /** 当前的选择索引 */
    private int mSelectedIndex = 0;
    private int mItemSize;
    private boolean mSwitching;
    private boolean mUnusable = true;

    private ItemSelectionListener mItemSelectionListener;
    private LinearLayout mContainer;

    public void setItemSelectionListener(ItemSelectionListener listen) {
        mItemSelectionListener = listen;
    }


    public SwitcherBar(Context context, AttributeSet attrs) {
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
        }
    };

    private View.OnTouchListener mItemTouch = new View.OnTouchListener() {
        private long startTime;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startTime = System.currentTimeMillis();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (System.currentTimeMillis() - startTime < 200) {
                    mItemClick.onClick(v);
                }

            }
            return true;
        }
    };

    public void initData(String[] values, int img_header_id, int currentIndex){
        this.mValues = values;
        this.img_header_id = img_header_id;
        this.mSelectedIndex = currentIndex;
        createItemView();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // createItemView();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        // 页面重绘的时需要将translation置0，否则计算位置时会加上，导致位置错乱
        mContainer.setTranslationX(0f);

        // 中心位置再偏移n * size + size/2(半边)
        int center = getWidth() / 2;
        // int left = center - ((mSelectedIndex * mItemSize) + mItemSize / 2);
        int left = center - (mItemSize + mItemSize / 2) * 4 / 5;
        int right = left + mContainer.getWidth();
        mContainer.layout(left,mContainer.getTop(),right, mContainer.getBottom());
    }

    public void setUnusable(boolean v){
        this.mUnusable = v;
    }

    public void setSelection(int targetIndex) {
        if(mSwitching || mUnusable || mSelectedIndex == targetIndex) {
            return;
        }
        renderSelection(targetIndex);

        if(mItemSelectionListener != null){
            mItemSelectionListener.onSelection(targetIndex, 0);
        }
    }
    
    public void renderSelection(int targetIndex) {
        // float translationX = mContainer.getTranslationX();
        // translationX = complexOffset(mSelectedIndex,targetIndex,translationX);
        // mContainer.setTranslationX(translationX);

        renderItemColor(mSelectedIndex,targetIndex);
        mSelectedIndex = targetIndex;
    }

    private float complexOffset(int fromIndex ,int toIndex ,float translationX) {
        float transX = 0;
        if (fromIndex < toIndex) {
            // 左移动
            transX =- ((toIndex - fromIndex) * mItemSize - translationX);
        } else {
            // 右移动
            transX = (fromIndex - toIndex) * mItemSize + translationX;
        }
        return transX;
    }

    Animator.AnimatorListener mEndAnimListen = new Animator.AnimatorListener(){
        @Override
        public void onAnimationStart(Animator animation) {
            mSwitching = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mSwitching = false;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mSwitching = false;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    void createItemView() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mItemSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, metrics);
        int width = mItemSize;
        int height = width / 4 * 3;

        ImageView img_header = new ImageView(getContext());
        img_header.setScaleType(ImageView.ScaleType.CENTER);
        img_header.setLayoutParams(new LayoutParams(width / 4 * 6, LayoutParams.MATCH_PARENT));
        // img_header.setLeft((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, metrics));
        img_header.setImageResource(img_header_id);
        this.addView(img_header);

        mContainer = new LinearLayout(getContext());
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mContainer.setOrientation(LinearLayout.HORIZONTAL);
        mContainer.setLayoutParams(params);


        for (int i = 0; i < mValues.length; i++) {
            ItemTextView item = new ItemTextView(getContext());
            item.setTextColor(mSelectedIndex == i ? mSelectedColor : mUnselectedColor);
            item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            item.setText(mValues[i]);
            item.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            item.setWidth(width);
            item.setHeight(height);
//            item.setBackgroundResource(R.color.pano_progress_done);
            item.setIndex(i);
            // item.setOnClickListener(mItemClick);
            item.setOnTouchListener(mItemTouch);
            mContainer.addView(item);
        }

        this.addView(mContainer);
    }

    private void renderItemColor(int fromIndex,int toIndex) {
        ((TextView) mContainer.getChildAt(fromIndex)).setTextColor(mUnselectedColor);
        ((TextView) mContainer.getChildAt(toIndex)).setTextColor(mSelectedColor);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mContainer.onTouchEvent(event);
    }

    public static interface ItemSelectionListener {
        void onSelection(int position, int id);
    }
}
