package edu.cmu.hcii.sugilite.verbal_instruction_demo;

import java.util.List;

/**
 * @author toby
 * @date 12/10/17
 * @time 2:09 AM
 */
public class VerbalInstructionResults {

    private List<VerbalInstructionResult> queries;

    public class VerbalInstructionResult {
        private int id;
        private String formula;
        private List<String> grounding;

        public int getId() {
            return id;
        }

        public String getFormula() {
            return formula;
        }

        public List<String> getGrounding() {
            return grounding;
        }
    }

    public List<VerbalInstructionResult> getQueries() {
        return queries;
    }
}
