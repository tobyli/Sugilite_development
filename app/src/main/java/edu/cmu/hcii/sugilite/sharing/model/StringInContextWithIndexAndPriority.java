package edu.cmu.hcii.sugilite.sharing.model;

/**
 * @author toby
 * @date 9/25/19
 * @time 11:55 AM
 */
public class StringInContextWithIndexAndPriority extends StringInContext {
    private final Integer priority;
    private final Integer index;

    public StringInContextWithIndexAndPriority(String activityName, String packageName, String text) {
        super(activityName, packageName, text);
        this.priority = -1;
        this.index = -1;
    }

    public StringInContextWithIndexAndPriority(String activityName, String packageName, String text, int priority, int index) {
        super(activityName, packageName, text);
        this.priority = priority;
        this.index = index;
    }

    public int getPriority() {
        return priority;
    }

    public int getIndex() {
        return index;
    }
}
