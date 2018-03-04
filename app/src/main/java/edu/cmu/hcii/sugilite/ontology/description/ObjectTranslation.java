package edu.cmu.hcii.sugilite.ontology.description;

import java.util.*;

/**
 * Created by Wanling Ding on 08/02/2018.
 */

public class ObjectTranslation {

    private static final HashMap<String,String> objectMap;
    static {
        objectMap = new HashMap<String,String>();
        // HAS_CLASS_NAME
        objectMap.put("android.widget.Button","button");
        objectMap.put("android.widget.ImageButton","button");
        objectMap.put("android.widget.Switch","switch");
        objectMap.put("android.widget.Checkbox","checkbox");

    }

    public static String getTranslation(String o)
    {
        if(objectMap.containsKey(o)){
            return objectMap.get(o);
        }
        else{
            return "item";
        }
    }

    public String toString()
    {
        String result = "";
        for (String s: objectMap.keySet())
        {
            result += s + " " + objectMap.get(s) + "\n";
        }
        return result;
    }


}
