package edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm;

/**
 * @author toby
 * @date 12/10/17
 * @time 1:42 AM
 */
public interface SugiliteVerbalInstructionHTTPQueryInterface {
    void resultReceived(int responseCode, String result, String originalQuery);
}
