package edu.cmu.hcii.sugilite.sharing.model;
import java.util.Objects;

public class StringInContext {
    private final String activityName;
    private final String packageName;
    private final String text;

    public StringInContext(String activityName, String packageName, String text) {
        this.activityName = activityName;
        this.packageName = packageName;
        this.text = text;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getText() {
        return text;
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, activityName, packageName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringInContext) {
            StringInContext sic = (StringInContext)obj;
            return Objects.equals(text, sic.text) && Objects.equals(activityName, sic.activityName) && Objects.equals(packageName, sic.packageName);
        }
        return false;
    }

    public String toHashedJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"package\": \"" + packageName + "\"");
        sb.append(",\"activity\": \"" + activityName + "\"");
        sb.append(",\"text_hash\": \"" + new HashedString(text).toString() + "\"");
        //sb.append(",\"priority\": " + this.priority);
        //sb.append(",\"index\": " + index);
        sb.append("}");
        return sb.toString();
    }
}
