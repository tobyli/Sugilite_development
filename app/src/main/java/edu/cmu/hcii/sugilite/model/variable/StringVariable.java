package edu.cmu.hcii.sugilite.model.variable;

import java.io.Serializable;

/**
 * @author toby
 * @date 7/11/16
 * @time 4:54 PM
 */
public class StringVariable extends Variable implements Serializable {
    private String value;

    public StringVariable(){
        super();
    }
    public StringVariable(int type){
        super(type);
    }
    public StringVariable(String name){
        super(name);
    }
    public StringVariable(String name, String value){
        super(name);
        this.value = value;
    }
    public StringVariable(int type, String name, String value){
        super(type, name);
        this.value = value;
    }
    public String getValue(){
        return value;
    }
    public void setValue(String value){
        this.value = value;
    }
}
