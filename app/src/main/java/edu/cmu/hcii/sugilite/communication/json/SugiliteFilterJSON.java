package edu.cmu.hcii.sugilite.communication.json;

import android.graphics.Rect;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.block.util.UIElementMatchingFilter;

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
            this.isClickable = filter.getIsClickable();
            this.textOrChildTextOrContentDescription = filter.getTextOrChildTextOrContentDescription();
            if(filter.getParentFilter() != null)
                this.parentFilter = new SugiliteFilterJSON(filter.getParentFilter());

            Set<UIElementMatchingFilter> childrenFilters = filter.getChildFilter();
            if(childrenFilters != null) {
                childFilter = new HashSet<SugiliteFilterJSON>();
                for(UIElementMatchingFilter cf : childrenFilters) {
                    SugiliteFilterJSON s = new SugiliteFilterJSON(cf);
                    this.childFilter.add(s);
                }
            }

            Set<UIElementMatchingFilter> siblingFilters = filter.getSiblingFilter();
            if(siblingFilters != null) {
                siblingFilter = new HashSet<SugiliteFilterJSON>();
                for(UIElementMatchingFilter sf : siblingFilters) {
                    SugiliteFilterJSON s = new SugiliteFilterJSON(sf);
                    this.siblingFilter.add(s);
                }
            }

            if(filter.alternativeLabels != null && filter.alternativeLabels.size() > 0 && Const.KEEP_ALL_ALTERNATIVES_IN_THE_FILTER) {
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
        filter.setTextOrChildTextOrContentDescription(textOrChildTextOrContentDescription);
        filter.setIsClickable(isClickable);

        if(boundsInScreen != null)
            filter.setBoundsInScreen(Rect.unflattenFromString(boundsInScreen));
        if(boundsInParent != null)
            filter.setBoundsInParent(Rect.unflattenFromString(boundsInParent));
        if(parentFilter != null)
            filter.setParentFilter(parentFilter.toUIElementMatchingFilter());
        if(childFilter != null && childFilter.size() != 0) {
            for(SugiliteFilterJSON cf : childFilter) {
                filter.setChildFilter(cf.toUIElementMatchingFilter());
            }
        }
        if(siblingFilter != null && siblingFilter.size() != 0) {
            for(SugiliteFilterJSON sf : siblingFilter) {
                filter.setSiblingFilter(sf.toUIElementMatchingFilter());
            }
        }
        if(alternativeLabels != null) {
            filter.alternativeLabels = new HashSet<>();
            for(SugiliteAlternativePairJSON pair : alternativeLabels){
                filter.alternativeLabels.add(new AbstractMap.SimpleEntry<String, String>(pair.type, pair.value));
            }
        }
        return filter;
    }
    public String text, contentDescription, viewId, packageName, className, boundsInScreen, boundsInParent, textOrChildTextOrContentDescription;
    public SugiliteFilterJSON parentFilter;
    public Set<SugiliteFilterJSON> childFilter, siblingFilter;
    public Set<SugiliteAlternativePairJSON> alternativeLabels;
    public boolean isClickable = false;
}
