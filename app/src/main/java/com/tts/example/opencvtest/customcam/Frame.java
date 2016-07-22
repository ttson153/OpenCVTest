package com.tts.example.opencvtest.customcam;

import android.graphics.Bitmap;

/**
 * Created by tts on 7/22/16.
 */
public class Frame {
    public final int width;
    public final int height;
    public final byte[] nv21Data;
    public final Bitmap bitmap;

    public Frame(int width, int height, byte[] nv21Data)
    {
        this.width = width;
        this.height = height;
        this.nv21Data = nv21Data;
        this.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    public void close()
    {
        bitmap.recycle();
    }
}
