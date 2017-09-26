package edu.cmu.hcii.sugilite.recording;

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

/**
 * this class returns whether to select a feature by default for recording an operation
 */
public class UIElementFeatureRecommender {

    private String packageName, className, text, contentDescription, viewId, boundsInParent, boundsInScreen;
    private String scriptName;
    private boolean isEditable;
    private long time;
    private int eventType;
    private Set<Map.Entry<String, String>> allParentFeatures = new HashSet<>();
    private Set<Map.Entry<String, String>> allChildFeatures = new HashSet<>();
    private Set<Map.Entry<String, String>> allSiblingFeatures = new HashSet<>();

    public UIElementFeatureRecommender(String packageName, String className, String text, String contentDescription, String viewId, String boundsInParent, String boundsInScreen, String scriptName,
                                        boolean isEditable, long time, int eventType, Set<Map.Entry<String, String>> allParentFeatures, Set<Map.Entry<String, String>> allChildFeatures,Set<Map.Entry<String, String>> allSiblingFeatures){
        this.packageName = packageName;
        this.className = className;
        this.text = text;
        this.contentDescription = contentDescription;
        this.viewId = viewId;
        this.boundsInParent = boundsInParent;
        this.boundsInScreen = boundsInScreen;
        this.scriptName = scriptName;
        this.isEditable = isEditable;
        this.time = time;
        this.eventType = eventType;
        this.allParentFeatures = allParentFeatures;
        this.allChildFeatures = allChildFeatures;
        this.allSiblingFeatures = allSiblingFeatures;
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
        if(contentDescription.contentEquals("NULL") && (text.contentEquals("NULL") || text.contentEquals("")) && isEditable)
            return true;
        else
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
            if(entry.getValue().length() > 3 && scriptName.toLowerCase().contains(entry.getValue().toLowerCase())){
                retSet.add(entry);
                break;
            }
        }
        for(Map.Entry<String, String> entry : allChildFeatures){
            if(retSet.size() > 0)
                break;
            if(entry.getKey().contentEquals("Text")) {
                retSet.add(entry);
                break;
            }
        }
        return retSet;
    }

    public Set<Map.Entry<String, String>> chooseSiblingFeatures(){
        Set<Map.Entry<String, String>> retSet = new HashSet<>();
        if(!(contentDescription.contentEquals("NULL") && text.contentEquals("NULL")))
            return retSet;
        for(Map.Entry<String, String> entry : allSiblingFeatures){
            // TODO: magic number
            if(entry.getValue().length() > 3 && scriptName.toLowerCase().contains(entry.getValue().toLowerCase())){
                retSet.add(entry);
                return retSet;
            }
        }
        for(Map.Entry<String, String> entry : allSiblingFeatures){
            if(entry.getKey().contentEquals("Text")) {
                retSet.add(entry);
                return retSet;
            }
        }
        return retSet;
    }






}
