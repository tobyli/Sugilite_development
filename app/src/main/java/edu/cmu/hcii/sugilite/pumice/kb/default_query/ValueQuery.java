package edu.cmu.hcii.sugilite.pumice.kb.default_query;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.value.SugiliteValue;

/**
 * @author toby
 * @date 6/12/20
 * @time 9:19 AM
 */
public interface ValueQuery<T> extends Serializable {
    T query();
    String getStringReadableQuery(T result);
    String getFeedbackMessage(T result);
}
