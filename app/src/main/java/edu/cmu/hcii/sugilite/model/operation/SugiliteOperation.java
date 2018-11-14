package edu.cmu.hcii.sugilite.model.operation;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;

/**
 * @author toby
 * @date 6/10/16
 * @time 2:19 PM
 */
public abstract class SugiliteOperation implements Serializable {
    private int operationType;
    public static final int CLICK = 1, LONG_CLICK = 2, SET_TEXT = 3, RETURN = 7, SELECT = 8, READ_OUT = 9,
            LOAD_AS_VARIABLE = 10, SPECIAL_GO_HOME = 11, READOUT_CONST = 12, RESOLVE_PROCEDURE = 13, RESOLVE_VALUEQUERY = 14, RESOLVE_BOOLEXP = 15, GET = 16;

    public SugiliteOperation(){
        operationType = -1;
    }
    public SugiliteOperation(int operationType){
        this.operationType = operationType;
    }
    public int getOperationType(){
        return operationType;
    }

    @Deprecated
    public String getParameter(){
        return null;
    }

    public void setOperationType(int operationType){
        this.operationType = operationType;
    }

    public abstract boolean containsDataDescriptionQuery();
    public abstract SerializableOntologyQuery getDataDescriptionQueryIfAvailable();
}

