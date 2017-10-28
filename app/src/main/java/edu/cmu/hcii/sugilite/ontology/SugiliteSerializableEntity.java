package edu.cmu.hcii.sugilite.ontology;

import android.view.accessibility.AccessibilityNodeInfo;

import edu.cmu.hcii.sugilite.BuildConfig;
import edu.cmu.hcii.sugilite.Node;

import java.io.Serializable;

/**
 * @author nancy
 * @date 10/25/17
 * @time 2:13 PM
 */
public class SugiliteSerializableEntity<T extends Serializable> extends SugiliteEntity implements Serializable {

    public SugiliteSerializableEntity(SugiliteEntity e) {
        this.setEntityId(e.getEntityId());
        if (e.getEntityValue() instanceof Serializable) {
            this.setType(e.getType());
            this.setEntityValue(e.getEntityValue());

        } else {
            if(e.getEntityValue() instanceof AccessibilityNodeInfo) {
                this.setType(Node.class);
                this.setEntityValue(new Node((AccessibilityNodeInfo) e.getEntityValue()));
            }
            else {
                if (BuildConfig.DEBUG) {
                    throw new AssertionError();
                }
            }
        }
    }
}
