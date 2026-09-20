package com.maps.sdk;
import android.content.Context;
import android.widget.FrameLayout;
import com.maps.sdk.controllers.CameraController;

public class MapView extends FrameLayout {
    public MapView(Context c) { super(c); }
    public CameraController getCameraController() { return null; }
}
