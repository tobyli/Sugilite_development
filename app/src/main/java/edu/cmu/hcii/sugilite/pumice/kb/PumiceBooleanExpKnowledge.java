package edu.cmu.hcii.sugilite.pumice.kb;
import com.google.gson.Gson;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;

/**
 * @author toby
 * @date 10/29/18
 * @time 11:54 AM
 */
public class PumiceBooleanExpKnowledge {
    private String expName;
    private String utterance;


    //initial version

    //can be a constant, a resolve call or a get call
    private SugiliteValue arg0;
    private SugiliteBooleanExpressionNew.BoolOperator boolOperator;

    //can be a constant, a resolve call or a get call
    private SugiliteValue arg1;

    public PumiceBooleanExpKnowledge(){

    }

    public PumiceBooleanExpKnowledge (String expName, String utterance, SugiliteValue arg0, SugiliteBooleanExpressionNew.BoolOperator boolOperator, SugiliteValue arg1){
        this.expName = expName;
        this.utterance = utterance;
        this.arg0 = arg0;
        this.boolOperator = boolOperator;
        this.arg1 = arg1;
    }

    public PumiceBooleanExpKnowledge (String expName, String utterance, SugiliteBooleanExpressionNew sugiliteBooleanExpression){
        this(expName, utterance, sugiliteBooleanExpression.getArg0(), sugiliteBooleanExpression.getBoolOperator(), sugiliteBooleanExpression.getArg1());
    }

    public void copyFrom(PumiceBooleanExpKnowledge pumiceBooleanExpKnowledge){
        this.expName = pumiceBooleanExpKnowledge.expName;
        this.utterance = pumiceBooleanExpKnowledge.utterance;
        this.arg0 = pumiceBooleanExpKnowledge.arg0;
        this.boolOperator = pumiceBooleanExpKnowledge.boolOperator;
        this.arg1 = pumiceBooleanExpKnowledge.arg1;
    }

    /**
     * evaluate this boolean exp knowledge
     * @return
     */
    public boolean evaluate(SugiliteData sugiliteData){
        return SugiliteBooleanExpressionNew.evaluate(sugiliteData, arg0, arg1, boolOperator);
    }

    public void setExpName(String expName) {
        this.expName = expName;
    }

    public String getExpName() {
        return expName;
    }

    public String getProcedureDescription(){
        String description = "How to know whether " + expName;
        if (arg0 != null) {
            description = description + " using " + arg0.toString();
        }
        if (utterance != null) {
            description = description + " by checking if " + utterance;
        }
        return description;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
