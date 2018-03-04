package edu.cmu.hcii.sugilite.recording.newrecording.dialog_management;

/**
 * @author toby
 * @date 2/20/18
 * @time 4:56 PM
 */
public abstract class SugiliteDialogUtteranceFilter {
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
