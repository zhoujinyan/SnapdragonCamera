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

import android.view.View;

import com.android.camera.ShutterButton.OnShutterButtonListener;
import com.android.camera.PauseButton.OnPauseButtonListener;
import android.graphics.Rect;

public interface VideoController extends OnShutterButtonListener, OnPauseButtonListener {

	public static final int INIT = -1;
    public static final int PREVIEW_STOPPED = 0;
    public static final int IDLE = 1;  // preview is active
    // Focus is in progress. The exact focus state is in Focus.java.
    public static final int FOCUSING = 2;
	public static final int SWITCHING_CAMERA = 4;
    public void onReviewDoneClicked(View view);
    public void onReviewCancelClicked(View viwe);
    public void onReviewPlayClicked(View view);

    public boolean isVideoCaptureIntent();
    public boolean isInReviewMode();
    public int onZoomChanged(int index);

    public void onSingleTapUp(View view, int x, int y);
	public void onMoveUpAndDown(int flag);

    public void stopPreview();

    public void updateCameraOrientation();

    // Callbacks for camera preview UI events.
    public void onPreviewUIReady();
    public void onPreviewUIDestroyed();

	public void onScreenSizeChanged(int width, int height);

    public void onPreviewRectChanged(Rect previewRect);
	public void setExposure(int value);

	public void onMove();
}
