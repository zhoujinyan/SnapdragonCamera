package com.android.camera.ui;

import android.animation.Animator;
import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.util.Log;
import android.util.SparseArray;
import android.view.animation.BounceInterpolator;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.codeaurora.snapcam.R;

import com.android.camera.TsMakeupManager;
import com.android.camera.PreferenceGroup;
import com.android.camera.ListPreference;
import com.android.camera.CameraSettings;
import com.android.camera.util.CameraUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.android.camera.CameraActivity;


public class ModuleSwitcher extends LinearLayout implements SeekBar.OnSeekBarChangeListener {


    public static final int VIDEO_MODULE_INDEX = 0;
    public static final int MAKEUP_MODULE_INDEX = 1;
    public static final int PHOTO_MODULE_INDEX = 2;
    public static final int WIDE_ANGLE_PANO_MODULE_INDEX = 3;
    public static final int LIGHTCYCLE_MODULE_INDEX = 4;
    public static final int GCAM_MODULE_INDEX = 5;
	public static final int CAPTURE_MODULE_INDEX = 6;    
	public static final int PANOCAPTURE_MODULE_INDEX = 7;
	private static final int SELECTION_DELAY = 300;

    private String[] mValues = {
			getResources().getString(R.string.switcher_video),
			getResources().getString(R.string.switcher_beauty),
			getResources().getString(R.string.switcher_photo),
			getResources().getString(R.string.switcher_pano)
    };

    public volatile static boolean makeup;

    private int mSelectedColor;
    private int mUnselectedColor;

    /** 当前的选择索引 */
    private int mSelectedIndex = 1;
    private int mItemSize;
    private boolean mSwitching;
    private boolean mUnusable = true;

    private ModuleSwitchListener mModuleSwitchListener;
    private LinearLayout mContainer;
    private View makeupView;
    private SeekBar mSeekBar;
    private PreferenceGroup mPreferenceGroup;
    private TsMakeupManager.MakeupLevelListener mMakeupListener;
    private SparseArray<TextView> makeup_map_reverse;
    private int makeup_selected_id;

    private Context mContext;
	private CameraActivity mActivity;
	private LayoutParams mparams ;
	
	Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
						Bundle bundle = msg.getData();
				   		int index = bundle.getInt("index");
                       	setSelection(index);
                        break;
                }
            }
        };
    public void setSwitchListener(ModuleSwitchListener listen) {
        mModuleSwitchListener = listen;
    }

	public void setActivity(CameraActivity activity) {
		this.mActivity = activity;
	}

    public ModuleSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        Resources res = getResources();
        Resources.Theme theme = context.getTheme();
        mSelectedColor = res.getColor(R.color.mode_selected_text_color,theme);
        mUnselectedColor = res.getColor(R.color.mode_unselect_text_color,theme);

        //Log.d("ModuleSwitcher", String.format("=============index:%d switching:%b  unusable:%b",mSelectedIndex,mSwitching,mUnusable));
    }

    private OnClickListener mItemClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            ItemTextView item = (ItemTextView) v;
            if(mSelectedIndex == item.getIndex()) {
                return;
            }
			Log.d("ModuleSwitcher","onClick");
			if(!CameraUtil.isFastClick()) {
				Log.d("ModuleSwitcher","setSelection");
				mActivity.getBlurData();
				 if(mHandler.hasMessages(SELECTION_DELAY)) {
					mHandler.removeMessages(SELECTION_DELAY);
			     }
				Message msg = new Message();
				Bundle bundle = new Bundle();
				msg.what = 0;
				bundle.putInt("index",item.getIndex());
				msg.setData(bundle);
				mHandler.sendMessageDelayed(msg, SELECTION_DELAY);
			}
        }
    };

    private OnClickListener mMakeupItemClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if(makeup_selected_id == v.getId()){
                return;
            }
            makeup_selected_id = v.getId();
            renderMakeupColor((TextView) v, makeup_map_reverse.get(makeup_selected_id));
            mSeekBar.setProgress(getPrefValue(makeup_selected_id == R.id.id_txt_makeup_whiten ?
            (CameraUtil.isTsMakeUp ? CameraSettings.KEY_TS_MAKEUP_LEVEL_WHITEN : CameraSettings.KEY_ARCSOFT_BEAUTY_LEVEL_WHITEN) 
            : (CameraUtil.isTsMakeUp ? CameraSettings.KEY_TS_MAKEUP_LEVEL_CLEAN : CameraSettings.KEY_ARCSOFT_BEAUTY_LEVEL_SOFTEN)));
        }
    };

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        createItemView();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        // 页面重绘的时需要将translation置0，否则计算位置时会加上，导致位置错乱
        mContainer.setTranslationX(0f);

        // 中心位置再偏移n * size + size/2(半边)
        int center = getWidth() / 2;
        int left = center - ((mSelectedIndex * mItemSize) + mItemSize / 2);
        int right = left + mContainer.getWidth();
        mContainer.layout(left,mContainer.getTop(),right, mContainer.getBottom());
		Log.d("ModuleSwitcher","onLayout left:"+left+",right:"+right+",getBottom:"+mContainer.getBottom()+",getTop:"+mContainer.getTop());
    }

    public void setUnusable(boolean v){
        this.mUnusable = v;
    }

    public void setSelection(int targetIndex) {
        if(mSwitching || mUnusable || mSelectedIndex == targetIndex) {
            return;
        }
        makeup = (targetIndex == MAKEUP_MODULE_INDEX);

        if(mModuleSwitchListener != null) {
            mModuleSwitchListener.onModuleSelected(targetIndex);
        }
    }
    
    public void renderSelection(int targetIndex) {
        if(makeup && targetIndex == PHOTO_MODULE_INDEX)
            targetIndex = MAKEUP_MODULE_INDEX;
        float translationX = mContainer.getTranslationX();
        translationX = complexOffset(mSelectedIndex,targetIndex,translationX);
        mContainer.setTranslationX(translationX);
		Log.d("ModuleSwitcher","renderSelection translationX:"+translationX);

        renderItemColor(mSelectedIndex,targetIndex);
        mSelectedIndex = targetIndex;

        makeupView.setVisibility(makeup ? View.VISIBLE : View.GONE);
    }

    public boolean isMakeup(){
        return this.makeup;
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

    void createItemView() {
		Log.d("ModuleSwitcher","createItemView");
		
        mContainer = new LinearLayout(getContext());
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
       // params.bottomMargin = -105;
        params.gravity = Gravity.BOTTOM;
        mContainer.setOrientation(LinearLayout.HORIZONTAL);
        mContainer.setLayoutParams(params);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mItemSize = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 48, metrics);
        int width = mItemSize;
        int height = width / 4 * 3;

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
            item.setOnClickListener(mItemClick);
            mContainer.addView(item);
        }

        this.addView(mContainer);
			this.makeup_selected_id = R.id.id_txt_makeup_clean;
        makeupView = View.inflate(mContext, R.layout.ts_makeup_single_level_view, null);
	    mparams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		makeupView.setLayoutParams(mparams);
        TextView tv1 = (TextView) makeupView.findViewById(R.id.id_txt_makeup_clean);
        TextView tv2 = (TextView) makeupView.findViewById(R.id.id_txt_makeup_whiten);
        tv1.setOnClickListener(mMakeupItemClick);
        tv2.setOnClickListener(mMakeupItemClick);
        this.makeup_map_reverse = new SparseArray<TextView>(2);
        this.makeup_map_reverse.put(R.id.id_txt_makeup_clean, tv2);
        this.makeup_map_reverse.put(R.id.id_txt_makeup_whiten, tv1);
        mSeekBar = (SeekBar) makeupView.findViewById(R.id.seekbar_makeup_level);
        mSeekBar.setOnSeekBarChangeListener(this);
        this.addView(makeupView,0);

    }

    private void renderItemColor(int fromIndex,int toIndex) {
        ((TextView) mContainer.getChildAt(fromIndex)).setTextColor(mUnselectedColor);
        ((TextView) mContainer.getChildAt(toIndex)).setTextColor(mSelectedColor);
    }

    private void renderMakeupColor(TextView selected, TextView unSelected){
        selected.setTextColor(mSelectedColor);
        unSelected.setTextColor(mUnselectedColor);
    }

    public void initSeekBar(PreferenceGroup mPreferenceGroup, TsMakeupManager.MakeupLevelListener mMakeupListener){
        this.mPreferenceGroup = mPreferenceGroup;
        this.mMakeupListener = mMakeupListener;
        mSeekBar.setProgress(getPrefValue(makeup_selected_id == R.id.id_txt_makeup_whiten ? 
            (CameraUtil.isTsMakeUp ? CameraSettings.KEY_TS_MAKEUP_LEVEL_WHITEN : CameraSettings.KEY_ARCSOFT_BEAUTY_LEVEL_WHITEN) 
            : (CameraUtil.isTsMakeUp ? CameraSettings.KEY_TS_MAKEUP_LEVEL_CLEAN : CameraSettings.KEY_ARCSOFT_BEAUTY_LEVEL_SOFTEN)));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        setSeekbarValue(seekBar.getProgress());
    }

    private void setSeekbarValue(int value) {
        String key = CameraUtil.isTsMakeUp ? CameraSettings.KEY_TS_MAKEUP_LEVEL_WHITEN: CameraSettings.KEY_ARCSOFT_BEAUTY_LEVEL_WHITEN;
        if(makeup_selected_id == R.id.id_txt_makeup_clean) {
            key = CameraUtil.isTsMakeUp ? CameraSettings.KEY_TS_MAKEUP_LEVEL_CLEAN : CameraSettings.KEY_ARCSOFT_BEAUTY_LEVEL_SOFTEN;
        }
        setEffectValue(key, String.valueOf(value));
    }

    private void setEffectValue(String key, String value) {
        final ListPreference pref = (ListPreference) mPreferenceGroup.findPreference(key);
        if (pref == null)
            return;

        pref.setMakeupSeekBarValue(value);
        mMakeupListener.onMakeupLevel(key, value);
    }

    private int getPrefValue(String key) {
        ListPreference pref = mPreferenceGroup.findPreference(key);
        String value = pref.getValue();
        if(TextUtils.isEmpty(value)) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    public static interface ItemSelectionListener {
        void onSelection(int position, int id);
    }

    public interface ModuleSwitchListener {
        public void onModuleSelected(int i);

        public void onShowSwitcherPopup();
    }

	public void movePosition(boolean flag) {
		Log.d("ModuleSwitcher","movePosition flag:"+flag+",bottomMargin:"+mparams.bottomMargin);
		boolean showMakeup = makeupView.getVisibility() == View.VISIBLE?true:false;
		makeupView.setVisibility(View.GONE);
		if(flag) {
			if(mparams.bottomMargin != 165) {
				mparams.bottomMargin = 165;
			}
			
		} else{
			if(mparams.bottomMargin != 0) {
				mparams.bottomMargin = 0;
			}
		}
		 makeupView.setVisibility(showMakeup ? View.VISIBLE : View.GONE);
	}
}
