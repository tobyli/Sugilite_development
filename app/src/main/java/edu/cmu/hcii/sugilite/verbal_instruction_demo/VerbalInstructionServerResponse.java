package edu.cmu.hcii.sugilite.verbal_instruction_demo;

/**
 * @author toby
 * @date 12/11/17
 * @time 5:48 PM
 */
public class VerbalInstructionServerResponse {
    private String mode;
    private String feedbackFormula;
    private int feedbackFormulaId;

    public VerbalInstructionServerResponse(String mode, String feedbackFormula, int feedbackFormulaId){
        this.mode = mode;
        this.feedbackFormula = feedbackFormula;
        this.feedbackFormulaId = feedbackFormulaId;
    }

    public VerbalInstructionServerResponse(String feedbackFormula, int feedbackFormulaId){
        this("ACCEPT_PARSE", feedbackFormula, feedbackFormulaId);
    }
}
