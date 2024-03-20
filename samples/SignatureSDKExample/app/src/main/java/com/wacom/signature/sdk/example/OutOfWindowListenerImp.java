package com.wacom.signature.sdk.example;

import com.wacom.signature.sdk.OutOfWindowListener;

public class OutOfWindowListenerImp implements OutOfWindowListener {

    @Override
    public boolean stopCapturingStroke(float x, float y) {
        return true;
    }
}
