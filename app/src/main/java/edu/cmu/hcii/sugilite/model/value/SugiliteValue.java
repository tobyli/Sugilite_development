package edu.cmu.hcii.sugilite.model.value;

import edu.cmu.hcii.sugilite.SugiliteData;

/**
 * @author toby
 * @date 11/14/18
 * @time 12:53 AM
 */
public interface SugiliteValue<T>  {
    T evaluate(SugiliteData sugiliteData);
    String getReadableDescription();
}
