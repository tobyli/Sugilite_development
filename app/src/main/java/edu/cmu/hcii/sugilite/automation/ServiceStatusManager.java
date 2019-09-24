package edu.cmu.hcii.sugilite.automation;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.accessibility_service.SugiliteAccessibilityService;

/**
 * @author toby
 * @date 6/17/16
 * @time 5:03 PM
 */
public class ServiceStatusManager {
    private ActivityManager manager;
    private static ServiceStatusManager instance;
    private Class serviceClass;

    //FIXME: modified by Oscar. SugiliteAccessibilityService cannot be found if looked at different
    // contexts (e.g., Activities, Services, providers, etc). So we need: to make sure we use always
    // the same context (app context)
    private ServiceStatusManager(Context context){
         this.manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public static ServiceStatusManager getInstance(Context ctx){
        if( instance == null ){
            instance = new ServiceStatusManager( ctx.getApplicationContext() );
        }
        return instance;
    }

    public static ServiceStatusManager getInstance(Context ctx, Class serviceClass){
        if( instance == null ){
            instance = new ServiceStatusManager( ctx.getApplicationContext());
        }
        instance.serviceClass = serviceClass;
        return instance;
    }

    /**
     *
     * @return true if SugiliteAccessibilityService is active, false otherwise
     */
    public boolean isRunning() {
        Class clazz = serviceClass == null? SugiliteAccessibilityService.class : serviceClass;
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (clazz.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * bring the user to the accessibility settings if SugiliteAccessibilityService is not active
     */
    public void promptEnabling(){
        promptEnabling(false);
    }

    /**
     * This method is used externally by Middleware. It is not used internally by Sugilite
     * @param addFlag
     */
    public void promptEnabling(boolean addFlag){
        if(!isRunning()){
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            if(addFlag){
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            SugiliteData.getAppContext().startActivity(intent);
        }
    }

}
