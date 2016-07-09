package edu.cmu.hcii.sugilite.ui;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;

/**
 * @author toby
 * @date 6/24/16
 * @time 1:53 PM
 */
public class UIElementFeatureRecommender {

    private String packageName, className, text, contentDescription, viewId, boundsInParent, boundsInScreen;
    private boolean isEditable;
    private long time;
    private int eventType;
    private Set<Map.Entry<String, String>> allParentFeatures = new HashSet<>();
    private Set<Map.Entry<String, String>> allChildFeatures = new HashSet<>();

    public UIElementFeatureRecommender(String packageName, String className, String text, String contentDescription, String viewId, String boundsInParent, String boundsInScreen,
                                        boolean isEditable, long time, int eventType, Set<Map.Entry<String, String>> allParentFeatures, Set<Map.Entry<String, String>> allChildFeatures){
        this.packageName = packageName;
        this.className = className;
        this.text = text;
        this.contentDescription = contentDescription;
        this.viewId = viewId;
        this.boundsInParent = boundsInParent;
        this.boundsInScreen = boundsInScreen;
        this.isEditable = isEditable;
        this.time = time;
        this.eventType = eventType;
        this.allParentFeatures = allParentFeatures;
        this.allChildFeatures = allChildFeatures;
    }

    public boolean choosePackageName(){
        return true;
    }

    public boolean chooseClassName(){
        return true;
    }

    public boolean chooseText(){
        if(text.contentEquals("NULL"))
            return false;
        else
            return true;
    }

    public boolean chooseContentDescription(){
        if(contentDescription.contentEquals("NULL") || (!text.contentEquals("NULL")))
            return false;
        else
            return true;

    }

    public boolean chooseViewId(){
        if(viewId.contentEquals("NULL"))
            return false;
        else
            return true;
    }

    public boolean chooseBoundsInScreen(){
        if(boundsInScreen.contentEquals("NULL"))
            return false;
        if(contentDescription.contentEquals("NULL") && text.contentEquals("NULL") && allChildFeatures.size() == 0)
            return true;
        else
            return false;
    }

    public boolean chooseBoundsInParent(){
        return false;
    }

    public Set<Map.Entry<String, String>> chooseParentFeatures(){
        return new HashSet<Map.Entry<String, String>>();
    }

    public Set<Map.Entry<String, String>> chooseChildFeatures(){
        Set<Map.Entry<String, String>> retSet = new HashSet<>();
        if(!(contentDescription.contentEquals("NULL") && text.contentEquals("NULL")))
            return retSet;
        for(Map.Entry<String, String> entry : allChildFeatures){
            if(entry.getKey().contentEquals("Text")) {
                retSet.add(entry);
                break;
            }
        }
        return retSet;
    }






}
