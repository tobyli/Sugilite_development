package edu.cmu.hcii.sugilite.ontology;

import java.util.*;

/**
 * Created by Wanling Ding on 08/02/2018.
 */

public class ObjectTranslation {

    public static final HashMap<String,String> objectMap;
    static {
        objectMap = new HashMap<String,String>();
        // HAS_CLASS_NAME
        objectMap.put("android.widget.TextView","");
        objectMap.put("android.widget.Button","button");
        objectMap.put("android.widget.ImageButton","button");
        objectMap.put("android.widget.LinearLayout","");
        objectMap.put("android.widget.Switch","switch");
        objectMap.put("android.widget.Checkbox","checkbox");
        // HAS_PACKAGE_NAME
//        objectMap.put("com.android.chrome","Chrome");
        objectMap.put("com.android.launcher3","home screen");
//        objectMap.put("com.google.android.gm","Gmail");
//        objectMap.put("com.android.calculator2","Calculator");
        objectMap.put("com.google.android.googlequicksearchbox","home screen");
    }

    public String getTranslation(String o)
    {
        return objectMap.get(o);
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

    public static void main(String[] args)
    {
        ObjectTranslation test = new ObjectTranslation();
        System.out.println(test);
    }

}
