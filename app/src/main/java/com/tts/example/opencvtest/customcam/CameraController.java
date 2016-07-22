package com.tts.example.opencvtest.customcam;

/**
 * Created by tts on 7/22/16.
 */
public abstract class CameraController {
    protected FrameListener frameListener;

    public abstract void initialize() throws NoCameraAvailableException;

    public abstract void shutdown();

    public abstract int getPreviewWidth();

    public abstract int getPreviewHeight();

    public void setFrameListener(FrameListener frameListener)
    {
        this.frameListener = frameListener;
    }
}
