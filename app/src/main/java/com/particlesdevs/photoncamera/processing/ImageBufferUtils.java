package com.particlesdevs.photoncamera.processing;

import android.graphics.Point;
import android.hardware.camera2.CaptureResult;

import java.nio.ByteBuffer;

public class ImageBufferUtils {
    public static void RemoveHotpixelsRaw(ByteBuffer in, Point size, CaptureResult res) {
        Point[] hotpixels = res.get(CaptureResult.STATISTICS_HOT_PIXEL_MAP);
        for (Point hotpixel : hotpixels) {
            int ind = size.x * hotpixel.y + hotpixel.x;
            in.asShortBuffer().put(ind, (short) 1024);
        }

    }
}
