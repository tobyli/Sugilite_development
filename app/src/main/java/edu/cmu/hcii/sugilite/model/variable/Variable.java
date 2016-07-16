package edu.cmu.hcii.sugilite.model.variable;

import java.io.Serializable;

/**
 * @author toby
 * @date 7/11/16
 * @time 4:53 PM
 */
public class Variable implements Serializable {
    private String name;
    public Variable(String name){
        this.name = name;
    }
    public Variable(){

    }
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }
}
