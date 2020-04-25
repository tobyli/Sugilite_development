package edu.cmu.hcii.sugilite.sovite.visual;

import java.util.List;

import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;

/**
 * @author toby
 * @date 3/29/20
 * @time 12:35 PM
 */
public interface SoviteVariableUpdateCallback {
    void onGetProcedureOperationUpdated (SugiliteGetProcedureOperation sugiliteGetProcedureOperation, List<VariableValue> changedNewVariableValues, boolean toShowNewScreenshot);
}
