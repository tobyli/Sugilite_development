package edu.cmu.hcii.sugilite.sovite;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.recording.RecordingPopUpDialog;
import edu.cmu.hcii.sugilite.recording.SugiliteScreenshotManager;

/**
 * @author toby
 * @date 2/25/20
 * @time 10:13 AM
 */

//Singleton
public class SoviteAppNameAppInfoManager {
    private static final String TAG = SoviteAppNameAppInfoManager.class.getName();
    private static SoviteAppNameAppInfoManager instance = null;

    private PackageManager packageManager;
    private Context appContext;
    private Map<String, String> packageNameReadableNameMap;

    public static SoviteAppNameAppInfoManager getInstance(Context appContext){
        if (instance == null) {
            instance = new SoviteAppNameAppInfoManager(appContext);
        }
        return instance;
    }

    private SoviteAppNameAppInfoManager(Context appContext) {
        this.appContext = appContext;
        this.packageManager = appContext.getPackageManager();
        this.packageNameReadableNameMap = new HashMap<>();

        setupPackageNameReadableNameMap();
    }

    private void setupPackageNameReadableNameMap(){
        packageNameReadableNameMap.put("com.android.launcher3", "Home Screen");
        packageNameReadableNameMap.put("com.google.android.googlequicksearchbox", "Home Screen");
        packageNameReadableNameMap.put("com.google.android.apps.nexuslauncher", "Home Screen");
    }

    public String getReadableAppNameForPackageName(String packageName) {
        ApplicationInfo applicationInfo;
        try{
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        }
        catch (Exception e){
            applicationInfo = null;
        }
        if(packageNameReadableNameMap.containsKey(packageName)) {
            return packageNameReadableNameMap.get(packageName);
        }
        else if (applicationInfo != null) {
            return (String) packageManager.getApplicationLabel(applicationInfo);
        }
        else {
            return packageName;
        }
    }

    public Map<String, String> getAllAvailableAppPackageNameReadableNameMap(boolean toSkipSystemApps) {
        Map<String, String> appPackageNameReadableNameMap = new HashMap<>();
        List<ApplicationInfo> applicationInfoList = packageManager.getInstalledApplications(0);

        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // only get apps that can be launched from the launcher
        List<ResolveInfo>  resolveinfoList = packageManager.queryIntentActivities(resolveIntent, 0);
        Set<String> allowPackages = new HashSet();
        for (ResolveInfo resolveInfo:resolveinfoList){
            allowPackages.add(resolveInfo.activityInfo.packageName);
        }


        for (ApplicationInfo applicationInfo : applicationInfoList) {
            if (toSkipSystemApps && (!(allowPackages.contains(applicationInfo.packageName)))) {
                //TODO: check what apps are considered system
                continue;
            }
            String packageName = applicationInfo.packageName;
            if (packageNameReadableNameMap.containsKey(packageName)) {
                appPackageNameReadableNameMap.put(packageName, packageNameReadableNameMap.get(packageName));
            } else if (applicationInfo != null) {
                appPackageNameReadableNameMap.put(packageName, packageManager.getApplicationLabel(applicationInfo).toString());
            }
        }
        return appPackageNameReadableNameMap;
    }

    public Map<String, String> getAppReadableNameAppPackageNameMap(boolean toSkipSystemApps) {
        Map<String, String> appReadableNameAppPackageNameMap = new HashMap<>();
        for (Map.Entry<String, String> entry : getAllAvailableAppPackageNameReadableNameMap(toSkipSystemApps).entrySet()) {
            appReadableNameAppPackageNameMap.put(entry.getValue(), entry.getKey());
        }
        return appReadableNameAppPackageNameMap;
    }

    public String extractStringFromStringValueFormula(String formula) {
        if (formula.startsWith("(") && formula.endsWith(")")) {
            formula = formula.substring(1, formula.length() - 1);
            formula = formula.replaceFirst("string", "").trim();
            if (formula.startsWith("\"") && formula.endsWith("\"")) {
                formula = formula.substring(1, formula.length() - 1);
            }
            return formula;
        } else {
            Log.e(TAG, String.format("error in processing the formula: %s", formula));
            return "NULL";
        }
    }

    public Drawable getApplicationIconFromPackageName (String packageName) {
        try {
            return packageManager.getApplicationIcon(packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
