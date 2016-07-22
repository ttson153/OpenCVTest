package com.tts.example.opencvtest.customcam;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;
import java.util.List;

/**
 * Created by tts on 7/22/16.
 */
public class LegacyCameraController extends CameraController implements Camera.PreviewCallback {
    private static final String TAG = "LegacyCameraController";
    private static final int MAGIC_TEXTURE_ID = 10; // any old integer works as we're not using OpenGL directly
    private static final int NUM_FRAMES = 2;

    private Frame[] frames = new Frame[NUM_FRAMES];
    private Camera camera;
    private Camera.Size previewSize;

    // leave as member variable or else "cancelBuffer: BufferQueue has been abandoned!" errors start occurring
    private SurfaceTexture surfaceTexture;

    @Override
    public void initialize() throws NoCameraAvailableException
    {
        try
        {
            Log.d(TAG, "starting camera initialization...");
            camera = detectCamera();
            initCamera();
            Log.d(TAG, "camera initialization finished");
        }
        catch(IOException e)
        {
            Log.e(TAG, "error initializing camera");
            throw new NoCameraAvailableException();
        }
    }

    private Camera detectCamera() throws NoCameraAvailableException
    {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for(int i = 0; i < Camera.getNumberOfCameras(); ++i)
        {
            Camera.getCameraInfo(i, cameraInfo);
            if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
            {
                try
                {
                    Log.d(TAG, "attempting to open camera #" + i);
                    return Camera.open(i);
                }
                catch(RuntimeException e)
                {
                    Log.w(TAG, "failed opening camera #" + i + ": " + e.getLocalizedMessage());
                }
            }
        }

        Log.e(TAG, "no available cameras found");
        throw new NoCameraAvailableException();
    }

    private void initCamera() throws IOException
    {
        Camera.Parameters params = camera.getParameters();

        previewSize = detectSize(params);
        Log.d(TAG, "detected preview size=" + previewSize.width + "x" + previewSize.height);
        params.setPreviewSize(previewSize.width, previewSize.height);

        // NV21 is supported by all Android cameras and isn't too difficult to work with
        // in OpenCV, so use it on all platforms
        params.setPreviewFormat(ImageFormat.NV21);

        // enable video recording hint for better performance if allowed
        if(shouldEnableRecordingHint())
        {
            params.setRecordingHint(true);
        }

        // set focus mode to one of the following options, in descending priority:
        // 1. continuous picture - aggressive refocusing for still shots
        // 2. continuous video - slow refocusing for watchable videos
        List focusModes = params.getSupportedFocusModes();
        if(focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
        {
            Log.d(TAG, "enabling continuous picture focus mode");
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
        {
            Log.d(TAG, "enabling continuous video focus mode");
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        else
        {
            Log.w(TAG, "no continuous focus mode found");
        }

        camera.setParameters(params);
        params = camera.getParameters();

        Log.d(TAG, "initializing frame buffers");
        for(int i = 0; i < NUM_FRAMES; ++i)
        {
            frames[i] = initFrame(params);
            camera.addCallbackBuffer(frames[i].nv21Data);
        }

        camera.setPreviewCallbackWithBuffer(this);

        surfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);
        camera.setPreviewTexture(surfaceTexture);

        camera.startPreview();
    }

    private Camera.Size detectSize(Camera.Parameters params)
    {
        Camera.Size biggest = null;
        for(Camera.Size size : params.getSupportedPreviewSizes())
        {
            if(biggest == null || (biggest.width < size.width && biggest.height < size.height))
            {
                biggest = size;
            }
        }
        return biggest;
    }

    private boolean shouldEnableRecordingHint()
    {
        if(android.os.Build.MODEL.equals("GT-I9100"))
        {
            // galaxy S2 has problems with setRecordingHint
            return false;
        }

        return true;
    }

    private Frame initFrame(Camera.Parameters params)
    {
        Camera.Size size = params.getPreviewSize();
        int bpp = ImageFormat.getBitsPerPixel(params.getPreviewFormat());
        int previewBufferSize = size.width * size.height * bpp / 8;
        return new Frame(size.width, size.height, new byte[previewBufferSize]);
    }

    @Override
    public void shutdown()
    {
        Log.d(TAG, "shutting down camera");

        if(camera != null)
        {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }

        // NULL all cached frames to allow GC
        for(int i = 0; i < frames.length; ++i)
        {
            frames[i].close();
            frames[i] = null;
        }
    }

    @Override
    public int getPreviewWidth()
    {
        return previewSize.width;
    }

    @Override
    public int getPreviewHeight()
    {
        return previewSize.height;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
        Log.v(TAG, "received camera preview frame of size=" + data.length);
        Frame frame = lookupFrame(data);
        if(frame != null)
        {
            frameListener.onFrame(frame);
            camera.addCallbackBuffer(frame.nv21Data);
        }
        else
        {
            Log.e(TAG, "could not locate frame for preview data!");
        }
    }

    private Frame lookupFrame(byte[] data)
    {
        for(Frame f : frames)
        {
            if(f.nv21Data == data)
            {
                return f;
            }
        }
        return null;
    }
}
