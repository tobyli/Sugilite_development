package edu.cmu.hcii.sugilite.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author toby
 * @date 6/9/16
 * @time 3:19 PM
 */
public class SetMapEntrySerializableWrapper implements Serializable {
    public Set<Map.Entry<String, String>> set;
    public SetMapEntrySerializableWrapper(Set<Map.Entry<String, String>> set){
        this.set = set;
    }
    public SetMapEntrySerializableWrapper(){
        set = new HashSet<>();
    }
}
