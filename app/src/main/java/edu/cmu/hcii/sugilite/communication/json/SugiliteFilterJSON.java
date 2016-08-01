package edu.cmu.hcii.sugilite.communication.json;

import android.graphics.Rect;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;

/**
 * Created by toby on 7/14/16.
 */
public class SugiliteFilterJSON {
    public SugiliteFilterJSON(UIElementMatchingFilter filter){
        if(filter != null) {
            this.text = filter.getText();
            this.contentDescription = filter.getContentDescription();
            this.viewId = filter.getViewId();
            this.packageName = filter.getPackageName();
            this.className = filter.getClassName();
            this.boundsInScreen = filter.getBoundsInScreen();
            this.boundsInParent = filter.getBoundsInParent();
            if(filter.getParentFilter() != null)
                this.parentFilter = new SugiliteFilterJSON(filter.getParentFilter());
            if(filter.getChildFilter() != null)
                this.childFilter = new SugiliteFilterJSON(filter.getChildFilter());
            if(filter.alternativeLabels != null && filter.alternativeLabels.size() > 0) {
                this.alternativeLabels = new HashSet<>();
                for(Map.Entry<String, String> entry : filter.alternativeLabels){
                    this.alternativeLabels.add(new SugiliteAlternativePairJSON(entry.getKey(), entry.getValue()));
                }
            }
        }
    }
    public UIElementMatchingFilter toUIElementMatchingFilter(){
        UIElementMatchingFilter filter = new UIElementMatchingFilter();
        filter.setText(text);
        filter.setContentDescription(contentDescription);
        filter.setViewId(viewId);
        filter.setPackageName(packageName);
        filter.setClassName(className);
        if(boundsInScreen != null)
            filter.setBoundsInScreen(Rect.unflattenFromString(boundsInScreen));
        if(boundsInParent != null)
            filter.setBoundsInParent(Rect.unflattenFromString(boundsInParent));
        if(parentFilter != null)
            filter.setParentFilter(parentFilter.toUIElementMatchingFilter());
        if(childFilter != null)
            filter.setChildFilter(childFilter.toUIElementMatchingFilter());
        if(alternativeLabels != null) {
            filter.alternativeLabels = new HashSet<>();
            for(SugiliteAlternativePairJSON pair : alternativeLabels){
                filter.alternativeLabels.add(new AbstractMap.SimpleEntry<String, String>(pair.type, pair.value));
            }
        }
        return filter;
    }
    public String text, contentDescription, viewId, packageName, className, boundsInScreen, boundsInParent;
    public SugiliteFilterJSON parentFilter, childFilter;
    public Set<SugiliteAlternativePairJSON> alternativeLabels;
}
