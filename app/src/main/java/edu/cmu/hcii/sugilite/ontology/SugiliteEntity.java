package edu.cmu.hcii.sugilite.ontology;

import android.view.accessibility.AccessibilityNodeInfo;
import android.graphics.Rect;

import java.util.Objects;

import edu.cmu.hcii.sugilite.Node;

/**
 * @author toby
 * @date 9/25/17
 * @time 5:57 PM
 */
public class SugiliteEntity<T> {
    private Integer entityId;
    private T entityValue;

    //TODO: we'll see if we want to explicity store this - might be useful for things like AccessibilityNodeInfo
    private transient Class<T> type;

    public SugiliteEntity() {
        type = null;
    }

    public SugiliteEntity(Integer entityId, Class<T> type, T value){
        this.entityId = entityId;
        this.entityValue = value;
        this.type = type;
        //TODO: initiate a unique entityId
    }

    public SugiliteEntity(SugiliteSerializableEntity se) {
        // every sugiliteEntity is serializable right now, might change later
        entityId = se.getEntityId();
        type = se.getType();
        entityValue = (T)se.getEntityValue();
    }

    public Integer getEntityId() {
        return entityId;
    }

    public T getEntityValue(){
        return entityValue;
    }

    public Class<T> getType() {
        return type;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public void setEntityValue(T entityValue) {
        this.entityValue = entityValue;
    }

    public void setType(Class<T> type) {
        this.type = type;
    }

    //test
    public static void main(String[] args) {
        SugiliteEntity<String> testEntity = new SugiliteEntity<>(1, String.class, "Hello Duck");
        SugiliteEntity testEntity2 = testEntity;
        System.out.println(testEntity2.getEntityValue()); // Display the string.
        System.out.println(testEntity2.getEntityValue().getClass());
        System.out.println(testEntity2.getClass());
        System.out.println(testEntity.getClass());
        SugiliteEntity<Integer> testEntity3 = new SugiliteEntity<>(2, Integer.class, 5);
        SugiliteEntity testEntity4 = testEntity3;
        System.out.println(testEntity4.getEntityValue()); // Display the integer.
        System.out.println(testEntity4.getEntityValue().getClass());
        System.out.println(testEntity4.getClass());
        System.out.println(testEntity3.getClass());
    }

    @Override
    public boolean equals(Object obj) {
        Class type = ((T) new Object()).getClass();

        if(obj == this){
            return true;
        }
        if(obj instanceof SugiliteEntity){
            if(type.isInstance(((SugiliteEntity) obj).entityValue)){
                if(((SugiliteEntity) obj).entityValue instanceof AccessibilityNodeInfo){
                    AccessibilityNodeInfo curr = (AccessibilityNodeInfo) (((SugiliteEntity) this).entityValue);
                    AccessibilityNodeInfo toComp = (AccessibilityNodeInfo) (((SugiliteEntity) obj).entityValue);
                    Rect thisBox = new Rect();
                    Rect compBox = new Rect();
                    curr.getBoundsInScreen(thisBox);
                    toComp.getBoundsInScreen(compBox);
                    return thisBox.contains(compBox) && compBox.contains(thisBox) &&
                            curr.getClassName().toString().equals(toComp.getClassName().toString()) &&
                            curr.getContentDescription().toString().equals(toComp.getContentDescription().toString());

                }
                return entityValue.equals(((SugiliteEntity) obj).entityValue);
            }
            return false;
        }

        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.entityValue);
    }
}
