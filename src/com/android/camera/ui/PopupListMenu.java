/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2010 The Android Open Source Project
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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.camera.ListPreference;
import com.android.camera.PreferenceGroup;
import com.android.camera.SettingsManager;

import org.codeaurora.snapcam.R;

import java.util.ArrayList;
import java.util.List;
import android.widget.FrameLayout;


/* A popup window that contains several camera settings. */
public class PopupListMenu extends ListView
        implements PopupListMenuItem.Listener {
    @SuppressWarnings("unused")
    private static final String TAG = "PopupListMenu";
	private Listener mListener;
    private ArrayList<ListPreference> mListItem = new ArrayList<ListPreference>();

    private boolean[] mEnabled;
	private int mIconIds[] = {
			 R.drawable.icon_settings_time,
			 R.drawable.icon_settings_pic_size,
			 R.drawable.icon_settings_sound,
			// R.drawable.icon_settings_bokeh,
    };
	private int mIconIds2[] = {
			 R.drawable.icon_settings_time,
			 R.drawable.icon_settings_pic_size,
			 R.drawable.icon_settings_sound,
			// R.drawable.icon_settings_bokeh,
			 R.drawable.icon_settings_self_mirro
    };

    @Override
    public void onListPrefChanged(ListPreference pref) {
        if (mListener != null) {
            mListener.onSettingChanged(pref);
        }
    }

    static public interface Listener {
        public void onSettingChanged(ListPreference pref);

        public void onPreferenceClicked(ListPreference pref);

        public void onPreferenceClicked(ListPreference pref, int y);
		public void onListMenuTouched();
    }

    static public interface SettingsListener {
        // notify SettingsManager
        public void onSettingChanged(ListPreference pref);
    }

	public void setSettingChangedListener(Listener listener) {
		 mListener = listener;
	 }

    private class MoreSettingAdapter extends ArrayAdapter<ListPreference> {
        LayoutInflater mInflater;
		Context context;
	    PopupListMenuItem popupItem;

        MoreSettingAdapter() {
            super(PopupListMenu.this.getContext(), 0, mListItem);
            context = getContext();
            mInflater = LayoutInflater.from(context);
        }

        private int getSettingLayoutId(ListPreference pref) {
            return R.layout.popup_menu_item;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ListPreference pref = mListItem.get(position);
			int viewLayoutId = getSettingLayoutId(pref);
            convertView = (FrameLayout)
                    mInflater.inflate(viewLayoutId, parent, false);
            popupItem = (PopupListMenuItem) convertView.findViewById(R.id.popup_menu_item);
			int iconId = -1;
			Log.d(TAG,"getView size:"+mListItem.size());
			int tmpIconIds[] = mListItem.size() == mIconIds.length ? mIconIds:mIconIds2;
			if(position >= 0 && position < tmpIconIds.length) {
				iconId = tmpIconIds[position];
			}
            popupItem.initialize(pref, iconId); // no init for restore one
            popupItem.setSettingChangedListener(PopupListMenu.this);
			popupItem.setUnusable(false);
			popupItem.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
        });
            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            if (position >= 0 && position < mEnabled.length) {
                return mEnabled[position];
            }
            return true;
        }
    }

    public PopupListMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(PreferenceGroup group, String[] keys) {
        // Prepare the setting items.
        for (int i = 0; i < keys.length; ++i) {
            ListPreference pref = group.findPreference(keys[i]);
            if (pref != null) {
				  mListItem.add(pref);
			      Log.d(TAG,"initialize KEY:"+pref.getKey());
			}   
        }
        ArrayAdapter<ListPreference> mListItemAdapter = new MoreSettingAdapter();
        setAdapter(mListItemAdapter);
        // Initialize mEnabled
        mEnabled = new boolean[mListItem.size()];
        for (int i = 0; i < mEnabled.length; i++) {
            mEnabled[i] = true;
        }
    }

	public void onSettingChanged(ListPreference pref) {
        if (mListener != null) {
            mListener.onSettingChanged(pref);
        }
    }

	public void overrideSettings(final String... keyvalues) {
		  int count = mEnabled == null ? 0 : mEnabled.length;
		  for (int i = 0; i < keyvalues.length; i += 2) {
			  String key = keyvalues[i];
			  String value = keyvalues[i + 1];
			  for (int j = 0; j < count; j++) {
				  ListPreference pref = mListItem.get(j);
				  if (pref != null && key.equals(pref.getKey())) {
					  // Change preference
					  if (value != null)
						  pref.setValue(value);
					  // If the preference is overridden, disable the preference
					  boolean enable = value == null;
					  mEnabled[j] = enable;
					  int offset = getFirstVisiblePosition();
					  if (offset >= 0) {
						  int indexInView = j - offset;
						  if (getChildCount() > indexInView && indexInView >= 0) {
							  getChildAt(indexInView).setEnabled(enable);
						  }
					  }
				  }
			  }
		  }
	  }

    // When preferences are disabled, we will display them grayed out. Users
    // will not be able to change the disabled preferences, but they can still
    // see
    // the current value of the preferences
    public void setPreferenceEnabled(String key, boolean enable) {
        int count = mEnabled == null ? 0 : mEnabled.length;
        for (int j = 0; j < count; j++) {
            ListPreference pref = mListItem.get(j);
            if (pref != null && key.equals(pref.getKey())) {
                mEnabled[j] = enable;
                break;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_MOVE) {
			 mListener.onListMenuTouched();
        }
        return super.onTouchEvent(ev);
    }

}
