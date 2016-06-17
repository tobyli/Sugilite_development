package edu.cmu.hcii.sugilite.automation;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import edu.cmu.hcii.sugilite.SugiliteAccessibilityService;

/**
 * @author toby
 * @date 6/17/16
 * @time 5:03 PM
 */
public class ServiceStatusManager {
    private Context context;
    public ServiceStatusManager(Context context){
        this.context = context;
    }

    /**
     *
     * @return true if SugiliteAccessibilityService is active, false otherwise
     */
    public boolean isRunning() {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SugiliteAccessibilityService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * bring the user to the accessibility settings if SugiliteAccessibilityService is not active
     */
    public void promptEnabling(){
        if(!isRunning()){
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            context.startActivity(intent);
        }
    }

}
