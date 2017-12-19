package com.android.camera.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;
import android.net.Uri;

import com.android.camera.data.LocalDataAdapter;
import org.codeaurora.snapcam.R;

import uk.co.senab.photoview.PhotoView;
import com.bumptech.glide.Glide;

public class PhotoViewPager extends ViewPager {
    public PhotoViewPager(Context context) {
        super(context);
    }

    public PhotoViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (Exception e) {
        }
        return false;
    }

    public static class MyViewPagerAdapter extends PagerAdapter {
        List<Uri> imgs = new ArrayList<>();
        Context mContext;
        public MyViewPagerAdapter(Context context, LocalDataAdapter mDataAdapter) {
            this.mContext = context;
            for(int i = 0; i < mDataAdapter.getTotalNumber(); i++){
                if(mDataAdapter.getImageData(i).isPhoto()){
                    imgs.add(mDataAdapter.getImageData(i).getContentUri());
                }
            }
        }
      
        @Override
        public int getCount() {
            return imgs.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((PhotoViewPager) container).removeView((View) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            LinearLayout view = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.photo_item, null);
            PhotoView img = (PhotoView) view.findViewById(R.id.iv_photo);
            Glide.with(mContext).load(imgs.get(position)).into(img);
            ((PhotoViewPager) container).addView(view);
            return view;
        }
    }

}