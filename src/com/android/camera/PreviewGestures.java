/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.camera;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.util.Log;
import com.android.camera.PhotoMenu;
import com.android.camera.VideoMenu;
import com.android.camera.ui.PieRenderer;
import com.android.camera.ui.RenderOverlay;
import com.android.camera.ui.TrackingFocusRenderer;
import com.android.camera.ui.ZoomRenderer;
import com.android.camera.ui.ModuleSwitcher;
import com.android.camera.util.CameraUtil;
import android.os.Handler;
import android.os.Message;


/* PreviewGestures disambiguates touch events received on RenderOverlay
 * and dispatch them to the proper recipient (i.e. zoom renderer or pie renderer).
 * Touch events on CameraControls will be handled by framework.
 * */
public class PreviewGestures
        implements ScaleGestureDetector.OnScaleGestureListener {

    private static final String TAG = "CAM_gestures";

    private static final int MODE_NONE = 0;
    private static final int MODE_ZOOM = 2;

    public static final int DIR_UP = 0;
    public static final int DIR_DOWN = 1;
    public static final int DIR_LEFT = 2;
    public static final int DIR_RIGHT = 3;

    private SingleTapListener mTapListener;
    private RenderOverlay mOverlay;
    private PieRenderer mPie;
    private TrackingFocusRenderer mTrackingFocus;
    private ZoomRenderer mZoom;
    private MotionEvent mDown;
    private MotionEvent mCurrent;
    private ScaleGestureDetector mScale;
    private int mMode;
    private boolean mZoomEnabled;
    private boolean mEnabled;
    private boolean mZoomOnly;
    private GestureDetector mGestureDetector;
    private CaptureUI mCaptureUI;
    private PhotoMenu mPhotoMenu;
    private VideoMenu mVideoMenu;
    private boolean waitUntilNextDown;
    private boolean setToFalse;
    private CameraActivity mActivity;
    private ModuleSwitcher mSwitcher;
    private int mCurModeIndex = -1;
    private GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public void onLongPress (MotionEvent e) {
            // Open pie
            if (!mZoomOnly && mPie != null) {
                openPie();
            }
        }

        @Override
        public boolean onSingleTapUp (MotionEvent e) {
            // Tap to focus when pie is not open
            if (mPie != null) {
                mTapListener.onSingleTapUp(null, (int) e.getX(), (int) e.getY());
                return true;
            }
            return false;
        }

        @Override
        public boolean onScroll (MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1 == null) {
                // e1 can be null if for some cases.
                return false;
            }
            if (mZoomOnly || mMode == MODE_ZOOM) return false;

           /** int deltaX = (int) (e1.getX() - e2.getX());
            int deltaY = (int) (e1.getY() - e2.getY());

            int orientation = 0;
            if (mPhotoMenu != null)
                orientation = mPhotoMenu.getOrientation();
            else if (mVideoMenu != null)
                orientation = mVideoMenu.getOrientation();
            if (isRightSwipe(0, deltaX, deltaY)) {
                rightSwipeToMode();
                return true;
            }else if (isLeftSwipe(0, deltaX, deltaY)) {
                leftSwipeToMode();
                return true;
            }*/	
            int direction = detectDicr(e1.getX(),e1.getY(),e2.getX(),e2.getY()); 
		   Log.d(TAG,"onScroll direction:"+direction);
		    switch(direction) {
				case 1:
					mTapListener.onMoveUpAndDown(1);
					return true;
				case 2:
					mTapListener.onMoveUpAndDown(2);
					return true;
				case 3:
					mActivity.getBlurData();
           			mHandler.removeMessages(0);
					mHandler.sendEmptyMessageDelayed(0, 350);
                	return true;
				case 4:
					mActivity.getBlurData();
           			mHandler.removeMessages(1);
					mHandler.sendEmptyMessageDelayed(1, 350);
					return true;
				default:
					return false;
			}
        }

		Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                       	rightSwipeToMode();
                        break;
                    case 1:
                        leftSwipeToMode();
                        break; 
                }
            }
        };

	private void leftSwipeToMode(){
		switch (mCurModeIndex){
			case ModuleSwitcher.VIDEO_MODULE_INDEX:
				mSwitcher.setSelection(ModuleSwitcher.WIDE_ANGLE_PANO_MODULE_INDEX);
				break;
			case ModuleSwitcher.PHOTO_MODULE_INDEX:
				mSwitcher.setSelection(ModuleSwitcher.MAKEUP_MODULE_INDEX);
				break;
            case ModuleSwitcher.MAKEUP_MODULE_INDEX:
                mSwitcher.setSelection(ModuleSwitcher.VIDEO_MODULE_INDEX);
                break;
			case ModuleSwitcher.WIDE_ANGLE_PANO_MODULE_INDEX:
				mSwitcher.setSelection(ModuleSwitcher.PHOTO_MODULE_INDEX);
				break;
			default:
				//mActivity.onModuleSelected(ModuleSwitcher.PHOTO_MODULE_INDEX);
				//mSwitcher.setSelection(ModuleSwitcher.PHOTO_MODULE_INDEX);
				break;
		}
	}
	private void rightSwipeToMode(){
		switch (mCurModeIndex){
			case ModuleSwitcher.VIDEO_MODULE_INDEX:
				mSwitcher.setSelection(ModuleSwitcher.MAKEUP_MODULE_INDEX);
				break;
            case ModuleSwitcher.MAKEUP_MODULE_INDEX:
                mSwitcher.setSelection(ModuleSwitcher.PHOTO_MODULE_INDEX);
                break;
			case ModuleSwitcher.PHOTO_MODULE_INDEX:
				mSwitcher.setSelection(ModuleSwitcher.WIDE_ANGLE_PANO_MODULE_INDEX);
				break;			
			case ModuleSwitcher.WIDE_ANGLE_PANO_MODULE_INDEX:
				mSwitcher.setSelection(ModuleSwitcher.VIDEO_MODULE_INDEX);
				break;
			default:
				//mActivity.onModuleSelected(ModuleSwitcher.PHOTO_MODULE_INDEX);
				//mSwitcher.setSelection(ModuleSwitcher.PHOTO_MODULE_INDEX);
				break;
		}
	}	

	private boolean isRightSwipe(int orientation, int deltaX, int deltaY) {
            /*switch (orientation) {
                case 90:
                    return deltaY > 0 && Math.abs(deltaY) > 2 * Math.abs(deltaX);
                case 180:
                    return deltaX > 0 && Math.abs(deltaX) > 2 * Math.abs(deltaY);
                case 270:
                    return deltaY < 0 && Math.abs(deltaY) > 2 * Math.abs(deltaX);
                default:
                    return deltaX < 0 && Math.abs(deltaX) > 2 * Math.abs(deltaY);
            }*/
	    return deltaX > 0 && Math.abs(deltaX) > 2 * Math.abs(deltaY);
        }

        private boolean isLeftSwipe(int orientation, int deltaX, int deltaY) {
            switch (orientation) {
                case 90:
                    return deltaY > 0 && Math.abs(deltaY) > 2 * Math.abs(deltaX);
                case 180:
                    return deltaX > 0 && Math.abs(deltaX) > 2 * Math.abs(deltaY);
                case 270:
                    return deltaY < 0 && Math.abs(deltaY) > 2 * Math.abs(deltaX);
                default:
                    return deltaX < 0 && Math.abs(deltaX) > 2 * Math.abs(deltaY);
            }
        }
		 //通过手势来移动方块：1,2,3,4对应上下左右  
		private int detectDicr(float start_x,float start_y,float end_x,float end_y){  
		  Log.d(TAG,"detectDicr start_x:"+start_x+",start_y:"+start_y+",end_x:"+end_x+",end_y:"+end_y);
      	  boolean isLeftOrRight = (Math.abs(start_x - end_x)>100) &&( Math.abs(start_x - end_x) > Math.abs(start_y - end_y)) ? true : false;  
          if (isLeftOrRight){  
		  	 if(!CameraUtil.isThreeFastScroll()) {
				 if (start_x - end_x > 0){  
					return 3;  
                 }else if (start_x - end_x < 0){  
					 return 4;
                 }  
			 }
           	
      	  }else {  
		     if ((start_y - end_y > 0) && (Math.abs(start_y - end_y)>50)){  
				 if(!CameraUtil.isFastScroll()) {
					 return 1; 
				 }
           	 }else if ((start_y - end_y < 0) && (Math.abs(start_y - end_y)>50)){  
              	 if(!CameraUtil.isSecondFastScroll()) {
				     return 2; 
				 }  
           	 }  
		   }
        	return 0;  
    	}  
    };

    public interface SingleTapListener {
        public void onSingleTapUp(View v, int x, int y);
		public void onMoveUpAndDown(int flag);
		
    }
    public void setCurModeIndex(int index){
        if(mSwitcher.makeup && index == ModuleSwitcher.PHOTO_MODULE_INDEX)
            index = ModuleSwitcher.MAKEUP_MODULE_INDEX;
        mCurModeIndex = index;
    }
	  public PreviewGestures(CameraActivity ctx, SingleTapListener tapListener,
                           ZoomRenderer zoom, PieRenderer pie, TrackingFocusRenderer trackingfocus) {
        mTapListener = tapListener;
        mPie = pie;
        mTrackingFocus = trackingfocus;
        mZoom = zoom;
        mMode = MODE_NONE;
        mScale = new ScaleGestureDetector(ctx, this);
        mEnabled = true;
        mGestureDetector = new GestureDetector(mGestureListener);
    }
    public PreviewGestures(CameraActivity ctx, SingleTapListener tapListener,
            ZoomRenderer zoom, PieRenderer pie, ModuleSwitcher Switcher) {
        mTapListener = tapListener;
        mPie = pie;
        mZoom = zoom;
        mMode = MODE_NONE;
	mActivity = ctx;
        mScale = new ScaleGestureDetector(ctx, this);
        mEnabled = true;
	mSwitcher = Switcher; 
        mGestureDetector = new GestureDetector(mGestureListener);
    }
	

    public void setRenderOverlay(RenderOverlay overlay) {
        mOverlay = overlay;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public void setZoomEnabled(boolean enable) {
        mZoomEnabled = enable;
    }

    public void setZoomOnly(boolean zoom) {
        mZoomOnly = zoom;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setCaptureUI(CaptureUI ui) {
        mCaptureUI = ui;
    }

    public void setPhotoMenu(PhotoMenu menu) {
        mPhotoMenu = menu;
    }

    public void setVideoMenu(VideoMenu menu) {
        mVideoMenu = menu;
    }

    public PhotoMenu getPhotoMenu() {
        return mPhotoMenu;
    }

    public VideoMenu getVideoMenu() {
        return mVideoMenu;
    }

    public boolean dispatchTouch(MotionEvent m) {
        if (setToFalse) {
            waitUntilNextDown = false;
            setToFalse = false;
        }
        if (waitUntilNextDown) {
            if (MotionEvent.ACTION_UP != m.getActionMasked()
                    && MotionEvent.ACTION_CANCEL != m.getActionMasked())
                return true;
            else {
                setToFalse = true;
                return true;
            }
        }
        if (!mEnabled) {
            return false;
        }
        mCurrent = m;
        if (MotionEvent.ACTION_DOWN == m.getActionMasked()) {
            mMode = MODE_NONE;
            mDown = MotionEvent.obtain(m);
        }

        // If pie is open, redirects all the touch events to pie.
        if (mPie != null && mPie.isOpen()) {
            return sendToPie(m);
        }
        if (mPhotoMenu != null) {
            if (mPhotoMenu.isMenuBeingShown()) {
                if (!mPhotoMenu.isMenuBeingAnimated()) {
                  //  waitUntilNextDown = true;
                  //  mPhotoMenu.closeView();
                }
               // return true;
            }
            if (mPhotoMenu.isPreviewMenuBeingShown()||mPhotoMenu.isMenuBeingShown()) {
                waitUntilNextDown = true;
                mPhotoMenu.animateSlideOutPreviewMenu();
                return true;
            }
        }

        if (mVideoMenu != null) {
            if (mVideoMenu.isMenuBeingShown()) {
                if (!mVideoMenu.isMenuBeingAnimated()) {
                    waitUntilNextDown = true;
                    mVideoMenu.closeView();
                }
                return true;
            }

            if (mVideoMenu.isPreviewMenuBeingShown()) {
                waitUntilNextDown = true;
                mVideoMenu.animateSlideOutPreviewMenu();
                return true;
            }
        }

        // If pie is not open, send touch events to gesture detector and scale
        // listener to recognize the gesture.
        mGestureDetector.onTouchEvent(m);
        if (mZoom != null) {
            mScale.onTouchEvent(m);
            if (MotionEvent.ACTION_POINTER_DOWN == m.getActionMasked()) {
                mMode = MODE_ZOOM;
                if (mZoomEnabled) {
                    // Start showing zoom UI as soon as there is a second finger down
                    //mZoom.onScaleBegin(mScale);
                }
            } else if (MotionEvent.ACTION_POINTER_UP == m.getActionMasked()) {
                mZoom.onScaleEnd(mScale);
            }
        }
        return true;
    }

    public boolean waitUntilNextDown() {
        return waitUntilNextDown;
    }

    private MotionEvent makeCancelEvent(MotionEvent m) {
        MotionEvent c = MotionEvent.obtain(m);
        c.setAction(MotionEvent.ACTION_CANCEL);
        return c;
    }

    private void openPie() {
        mGestureDetector.onTouchEvent(makeCancelEvent(mDown));
        mScale.onTouchEvent(makeCancelEvent(mDown));
        mOverlay.directDispatchTouch(mDown, mPie);
    }

    private boolean sendToPie(MotionEvent m) {
        return mOverlay.directDispatchTouch(m, mPie);
    }

    // OnScaleGestureListener implementation
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        return mZoom.onScale(detector);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (mPie == null || !mPie.isOpen()) {
            mMode = MODE_ZOOM;
            mGestureDetector.onTouchEvent(makeCancelEvent(mCurrent));
            if (!mZoomEnabled) return false;
            return mZoom.onScaleBegin(detector);
        }
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mZoom.onScaleEnd(detector);
    }
}

