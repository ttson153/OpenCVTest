package com.tts.example.opencvtest.customcam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by tts on 7/22/16.
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraView";

    private CameraController cameraController;
    private boolean enabled;

    /* typical ctors removed for brevity; all ctors call init() */

    private void init()
    {
        cameraController = createController();
        cameraController.setFrameListener(this);
        getHolder().addCallback(this);
    }

    private CameraController createController()
    {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
        {
            // TODO: implement camera2 interface
            //cameraController = new Camera2Controller();
            return new LegacyCameraController();
        }
        else
        {
            return new LegacyCameraController();
        }
    }

    public void startPreview()
    {
        if(!enabled)
        {
            Log.d(TAG, "starting camera preview...");
            enabled = true;

            try
            {
                cameraController.initialize();
            }
            catch (NoCameraAvailableException e)
            {
                Log.e(TAG, "no cameras found");
                // TODO: warning dialog
                return;
            }

            // initialize cached bitmap
            int w = cameraController.getPreviewWidth();
            int h = cameraController.getPreviewHeight();
            setup(w, h);
        }
    }

    public void endPreview()
    {
        if(enabled)
        {
            Log.d(TAG, "ending camera preview...");
            enabled = false;

            cameraController.shutdown();

            tearDown();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        Log.d(TAG, "surface changed; restarting camera...");
        endPreview();
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        Log.d(TAG, "surface destroyed; releasing camera...");
        endPreview();
    }

    @Override
    public void onFrame(Frame frame)
    {
        handleFrame(frame.width, frame.height, frame.nv21Data, frame.bitmap);
        drawFrame(frame);
    }

    private void drawFrame(Frame frame)
    {
        Canvas canvas = getHolder().lockCanvas();
        if(canvas != null)
        {
            try
            {
                canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
                canvas.drawBitmap(frame.bitmap,
                        new Rect(0, 0, frame.bitmap.getWidth(), frame.bitmap.getHeight()),
                        new Rect((canvas.getWidth() - frame.bitmap.getWidth()) / 2,
                                (canvas.getHeight() - frame.bitmap.getHeight()) / 2,
                                (canvas.getWidth() - frame.bitmap.getWidth()) / 2
                                        + frame.bitmap.getWidth(),
                                (canvas.getHeight() - frame.bitmap.getHeight()) / 2
                                        + frame.bitmap.getHeight()),
                        null);
            }
            finally
            {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    private native void setup(int width, int height);

    private native void handleFrame(int width, int height, byte[] nv21Data, Bitmap bitmap);

    private native void tearDown();
}
