package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

/**
 * @author toby
 * @date 6/4/18
 * @time 8:57 AM
 */
public class SugiliteBooleanExpression implements Serializable {

    //should support comparison between variables & comparision between a variable and a constant -- probably won't need to support nested/composite expressions for now
    //should support common operators (e.g., >, <, <=, >=, ==, !=, stringContains)

    public Boolean evaluate(){
        //TODO: implement -- returns the eval result of this expression at runtime
        return null;
    }

    @Override
    public String toString() {
        //TODO: implement
        return null;
    }
}
