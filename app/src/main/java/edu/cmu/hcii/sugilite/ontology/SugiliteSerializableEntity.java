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
public class SugiliteSerializableEntity<T extends Serializable> implements Serializable {
    private Integer entityId;
    private T entityValue;
    private transient Class<T> type;


    public SugiliteSerializableEntity(SugiliteEntity e) {
        entityId = e.getEntityId();
        if (e.getEntityValue() instanceof Serializable) {
            type = e.getType();
            entityValue = (T)e.getEntityValue();

        } else {
            if(e.getEntityValue() instanceof AccessibilityNodeInfo) {
                type = (Class<T>) Node.class;
                entityValue = (T)new Node((AccessibilityNodeInfo) e.getEntityValue());
            }
            else {
                if (BuildConfig.DEBUG) {
                    throw new AssertionError();
                }
            }
        }
    }

    public Class<T> getType() {
        return type;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public T getEntityValue() {
        return entityValue;
    }
}
