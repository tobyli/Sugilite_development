package edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm;

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
    private String entityClassNameFilter;


    public VerbalInstructionServerQuery(String mode, String userInput, List<List<String>> triples, String entityClassNameFilter){
        this.mode = mode;
        this.userInput = userInput;
        this.triples = triples;
        this.entityClassNameFilter = entityClassNameFilter;
    }

    public VerbalInstructionServerQuery(String mode, String userInput, List<List<String>> triples){
        this(mode, userInput, triples, null);
    }

    public VerbalInstructionServerQuery(String userInput, List<List<String>> triples, String entityClassNameFilter){
        this("USER_COMMAND", userInput, triples, entityClassNameFilter);
    }

}
