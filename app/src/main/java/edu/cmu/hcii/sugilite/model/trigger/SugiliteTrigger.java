package edu.cmu.hcii.sugilite.model.trigger;

/**
 * Created by toby on 1/15/17.
 */

public class SugiliteTrigger {
    private String name, scriptName, triggerContent, appPackageName;
    private int type;
    public static final int APP_LAUNCH_TRIGGER = 1, NOTIFICATION_TRIGGER = 2;

    public SugiliteTrigger(String name, String scriptName, String triggerContent, String appPackageName, int type){
        this.name = name;
        this.scriptName = scriptName;
        this.triggerContent = triggerContent;
        this.appPackageName = appPackageName;
        this.type = type;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setScriptName(String scriptName){
        this.scriptName = scriptName;
    }

    public void setTriggerContent(String triggerContent){
        this.triggerContent = triggerContent;
    }

    public void setAppPackageName(String appPackageName){
        this.appPackageName = appPackageName;
    }
    public void setType(int type){
        this.type = type;
    }

    public String getName(){
        return name;
    }

    public String getScriptName(){
        return scriptName;
    }

    public String getTriggerContent(){
        return triggerContent;
    }

    public String getAppPackageName(){
        return appPackageName;
    }

    public int getType(){
        return type;
    }




}
