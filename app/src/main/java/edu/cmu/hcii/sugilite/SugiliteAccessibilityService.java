package edu.cmu.hcii.sugilite;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.accessibility.AccessibilityEvent;

public class SugiliteAccessibilityService extends AccessibilityService {
    public SugiliteAccessibilityService() {
    }

    @Override
    public void onServiceConnected() {

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }
}
