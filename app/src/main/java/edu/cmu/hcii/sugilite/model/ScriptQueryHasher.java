package edu.cmu.hcii.sugilite.model;

import android.app.Activity;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteBinaryOperation;
import edu.cmu.hcii.sugilite.model.operation.trinary.SugiliteTrinaryOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteUnaryOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.ontology.HashedStringOntologyQuery;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class ScriptQueryHasher {
    private Activity context;
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;

    public ScriptQueryHasher(Activity context) {
        this.context = context;
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator();
    }

    public void hashOntologyQueries(SugiliteStartingBlock sugiliteStartingBlock) {
        for (SugiliteBlock block : getAllOperationBlocks(sugiliteStartingBlock)) {
            if (block instanceof SugiliteOperationBlock) {
                SugiliteOperationBlock operationBlock = (SugiliteOperationBlock)block;
                SugiliteOperation op = ((SugiliteOperationBlock) block).getOperation();
                if (op instanceof SugiliteUnaryOperation) {
                    SugiliteUnaryOperation unary = (SugiliteUnaryOperation)op;
                    if (unary.getParameter0() instanceof OntologyQuery) {
                        unary.setParameter0(HashedStringOntologyQuery.hashQuery((OntologyQuery) unary.getParameter0()));
                    }
                }
                if (op instanceof SugiliteBinaryOperation) {
                    SugiliteBinaryOperation binary = (SugiliteBinaryOperation)op;
                    if (binary.getParameter0() instanceof OntologyQuery) {
                        binary.setParameter0(HashedStringOntologyQuery.hashQuery((OntologyQuery) binary.getParameter0()));
                    }
                    if (binary.getParameter1() instanceof OntologyQuery) {
                        binary.setParameter1(HashedStringOntologyQuery.hashQuery((OntologyQuery) binary.getParameter1()));
                    }
                }
                if (op instanceof SugiliteTrinaryOperation) {
                    SugiliteTrinaryOperation trinary = (SugiliteTrinaryOperation)op;
                    if (trinary.getParameter0() instanceof OntologyQuery) {
                        trinary.setParameter0(HashedStringOntologyQuery.hashQuery((OntologyQuery) trinary.getParameter0()));
                    }
                    if (trinary.getParameter1() instanceof OntologyQuery) {
                        trinary.setParameter1(HashedStringOntologyQuery.hashQuery((OntologyQuery) trinary.getParameter1()));
                    }
                    if (trinary.getParameter2() instanceof OntologyQuery) {
                        trinary.setParameter2(HashedStringOntologyQuery.hashQuery((OntologyQuery) trinary.getParameter2()));
                    }
                }
                operationBlock.setDescription(ontologyDescriptionGenerator.getSpannedDescriptionForOperation(operationBlock.getOperation(), operationBlock.getOperation().getDataDescriptionQueryIfAvailable()));
            }
        }
    }


    /**
     * get operation blocks in all the subsequent blocks of a block
     * @param block
     * @return
     */
    private static List<SugiliteOperationBlock> getAllOperationBlocks(SugiliteBlock block) {
        List<SugiliteOperationBlock> operationBlocks = new ArrayList<>();
        if (block == null) {
            return operationBlocks;
        }
        if (block instanceof SugiliteOperationBlock) {
            operationBlocks.add((SugiliteOperationBlock)block);
        }
        if (block instanceof SugiliteConditionBlock) {
            operationBlocks.addAll(getAllOperationBlocks(((SugiliteConditionBlock) block).getThenBlock()));
            operationBlocks.addAll(getAllOperationBlocks(((SugiliteConditionBlock) block).getElseBlock()));
        }
        operationBlocks.addAll(getAllOperationBlocks(block.getNextBlock()));
        return operationBlocks;
    }
}
