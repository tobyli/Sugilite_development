package edu.cmu.hcii.sugilite.ontology;

import java.util.Objects;

/**
 * @author toby
 * @date 9/25/17
 * @time 5:57 PM
 */
public class SugiliteEntity<T> {
    private Integer entityId;
    private T entityValue;

    //TODO: we'll see if we want to explicity store this - might be useful for things like AccessibilityNodeInfo
    private final Class<T> type;


    public SugiliteEntity(Integer entityId, Class<T> type, T value){
        this.entityId = entityId;
        this.entityValue = value;
        this.type = type;
        //TODO: initiate a unique entityId
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
        if(obj == this){
            return true;
        }
        if(obj instanceof SugiliteEntity){
            return ((SugiliteEntity) obj).entityId.equals(this.entityId);
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.entityId);
    }
}
