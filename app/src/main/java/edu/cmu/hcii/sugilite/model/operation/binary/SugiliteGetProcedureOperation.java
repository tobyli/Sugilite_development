package edu.cmu.hcii.sugilite.model.operation.binary;

import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dao.PumiceKnowledgeDao;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 3/21/18
 * @time 6:19 PM
 */

/**
 * the operation used for getting a procedure from the KB
 */
public class SugiliteGetProcedureOperation extends SugiliteGetOperation<String> implements Serializable, SugiliteValue<String> {

    public SugiliteGetProcedureOperation(){
        super();
        this.variableValues = new ArrayList<>();
    }
    private List<StringVariable> variableValues;


    public SugiliteGetProcedureOperation(String name){
        super(name, SugiliteGetOperation.PROCEDURE_NAME);
        this.variableValues = new ArrayList<>();
    }

    /**
     * @return the subscript name
     */
    @Override
    public String evaluate(@Nullable SugiliteData sugiliteData) {
        PumiceKnowledgeDao pumiceKnowledgeDao = new PumiceKnowledgeDao(SugiliteData.getAppContext(), sugiliteData);
        try {
            PumiceKnowledgeManager pumiceKnowledgeManager = pumiceKnowledgeDao.getPumiceKnowledgeOrANewInstanceIfNotAvailable(false);
            if (pumiceKnowledgeManager != null) {
                for (PumiceProceduralKnowledge proceduralKnowledge : pumiceKnowledgeManager.getPumiceProceduralKnowledges()) {
                    if (getName().equals(proceduralKnowledge.getProcedureName())) {
                        //found;
                        return proceduralKnowledge.getTargetScriptName(pumiceKnowledgeManager);
                    }
                }
            } else {
                throw new RuntimeException("null knowledge manager!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            //TODO: retrive the knowledge manager when sugiliteData.pumiceDialogManager is null
            PumiceDemonstrationUtil.showSugiliteAlertDialog("Error when finding the procedure knowledge: " + getName());
            return null;
        }
        PumiceDemonstrationUtil.showSugiliteAlertDialog("Error when finding the procedure knowledge: " + getName());
        return null;
    }

    /**
     * @return the variable values of the get procedure operation
     */
    public List<StringVariable> getVariableValues() {
        return variableValues;
    }

    @Override
    public String getPumiceUserReadableDecription() {
        List<String> parameterStringList = new ArrayList<>();
        variableValues.forEach(stringVariable -> parameterStringList.add(String.format("the value of %s is %s", addQuoteToTokenIfNeeded(stringVariable.getName()), addQuoteToTokenIfNeeded(stringVariable.getValue()))));
        String parameters = PumiceDemonstrationUtil.joinListGrammatically(parameterStringList, "and");
        if (variableValues.size() > 0) {
            return String.format("perform the action \"%s\", where %s", getName(), parameters);
        } else {
            return String.format("perform the action \"%s\"", getName());
        }
    }

    public String getParameterValueReplacedDescription() {
        String description = getName();
        for (StringVariable stringVariable : variableValues) {
            description = description.replace("[" + stringVariable.getName() + "]", "[" + stringVariable.getValue() + "]");
        }
        return description;
    }

    @Override
    public String toString() {
        List<String> parameterStringList = new ArrayList<>();
        variableValues.forEach(stringVariable -> parameterStringList.add(addQuoteToTokenIfNeeded((String.format("(call set_param %s %s)", addQuoteToTokenIfNeeded(stringVariable.getName()), addQuoteToTokenIfNeeded(stringVariable.getValue()))))));
        String parameters = StringUtils.join(parameterStringList, " ");
        if (variableValues.size() > 0) {
            return "(" + "call get " + addQuoteToTokenIfNeeded(getParameter0()) + " " + addQuoteToTokenIfNeeded(getParameter1()) + " " + parameters + ")";
        } else {
            return "(" + "call get " + addQuoteToTokenIfNeeded(getParameter0()) + " " + addQuoteToTokenIfNeeded(getParameter1()) + ")";
        }
    }

}
