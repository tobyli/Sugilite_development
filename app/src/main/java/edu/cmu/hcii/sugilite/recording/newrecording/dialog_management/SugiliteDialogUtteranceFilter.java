package edu.cmu.hcii.sugilite.recording.newrecording.dialog_management;

/**
 * @author toby
 * @date 2/20/18
 * @time 4:56 PM
 */
public abstract class SugiliteDialogUtteranceFilter {

    /**
     * get a SugiliteDialogUtteranceFilter that returns true if the utterance contains any of the arg among args (case insensitive)
     * @param args
     * @return
     */
    public static SugiliteDialogUtteranceFilter getSimpleContainingFilter(String... args) {
        return new SugiliteDialogUtteranceFilter() {
            @Override
            public boolean checkIfMatch(String utterance) {
                for (String arg : args) {
                    if (utterance.toLowerCase().contains(arg)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * get a SugiliteDialogUtteranceFilter that returns a constant boolean value
     * @param value
     * @return
     */
    public static SugiliteDialogUtteranceFilter getConstantFilter(boolean value) {
        return new SugiliteDialogUtteranceFilter() {
            @Override
            public boolean checkIfMatch(String utterance) {
                return value;
            }
        };
    }

    /**
     * this function should return true if utterance is a match, and return false otherwise
     * @param utterance
     * @return
     */
    public abstract boolean checkIfMatch(String utterance);
}
