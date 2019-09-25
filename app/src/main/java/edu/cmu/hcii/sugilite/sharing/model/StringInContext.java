package edu.cmu.hcii.sugilite.sharing.model;

import com.google.api.client.repackaged.com.google.common.base.Objects;

public class StringInContext {
    public String activityName;
    public String packageName;
    public String text;

    public StringInContext(String activityName, String packageName, String text) {
        this.activityName = activityName;
        this.packageName = packageName;
        this.text = text;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(text, activityName, packageName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringInContext) {
            StringInContext sic = (StringInContext)obj;
            return text.equals(sic.text) && activityName.equals(sic.activityName) && packageName.equals(sic.packageName);
        }
        return false;
    }

    public String toJson() {
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
