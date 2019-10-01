package edu.cmu.hcii.sugilite.sharing.model;

public class HashedSubString extends HashedString {
    /**
     * Lower values have higher priority.
     * Semantically should always be greater than zero.
     * Loosely represents notion of "tokens removed".
     */
    private final int priority;

    public HashedSubString(String string, int priority) {
        super(string);
        this.priority = priority;
    }
    public HashedSubString(byte[] bytes, int priority, boolean isServerSalted) {
        super(bytes, isServerSalted);
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
