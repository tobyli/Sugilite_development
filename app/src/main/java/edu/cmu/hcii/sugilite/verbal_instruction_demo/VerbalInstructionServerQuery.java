package edu.cmu.hcii.sugilite.verbal_instruction_demo;

import java.util.List;

/**
 * @author toby
 * @date 12/11/17
 * @time 5:48 PM
 */
public class VerbalInstructionServerQuery {
    private String mode;
    private String userInput;
    private List<List<String>> triples;

    public VerbalInstructionServerQuery(String mode, String userInput, List<List<String>> triples){
        this.mode = mode;
        this.userInput = userInput;
        this.triples = triples;
    }

    public VerbalInstructionServerQuery(String userInput, List<List<String>> triples){
        this("USER_COMMAND", userInput, triples);
    }

}
