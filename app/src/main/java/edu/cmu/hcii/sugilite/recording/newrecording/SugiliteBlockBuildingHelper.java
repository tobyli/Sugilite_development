package edu.cmu.hcii.sugilite.recording.newrecording;

import android.content.Context;
import android.text.Spanned;
import android.util.Pair;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlockMetaInfo;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteReadoutOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteClickOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteLongClickOperation;
import edu.cmu.hcii.sugilite.ontology.*;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.ontology.OntologyQueryWithSubQueries;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;
import edu.cmu.hcii.sugilite.sharing.PrivateNonPrivateLeafOntologyQueryPairWrapper;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * @author toby
 * @date 1/10/18
 * @time 12:36 PM
 */

/**
 * the helper class for building a SugiliteBlock
 */
public class SugiliteBlockBuildingHelper {
    private ReadableDescriptionGenerator readableDescriptionGenerator;
    private Context context;
    private SugiliteData sugiliteData;
    private SugiliteScriptDao sugiliteScriptDao;
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;
    public SugiliteBlockBuildingHelper(Context context, SugiliteData sugiliteData){
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator();
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        }
        else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        }
        readableDescriptionGenerator = new ReadableDescriptionGenerator(context);
    }

    public SugiliteOperationBlock getUnaryOperationBlockWithOntologyQueryFromQuery(OntologyQuery query, int opeartionType, SugiliteAvailableFeaturePack featurePack, OntologyQuery alternativeQuery){
        if(opeartionType == SugiliteOperation.CLICK) {
            SugiliteClickOperation sugiliteOperation = new SugiliteClickOperation();
            sugiliteOperation.setQuery(query);
            if (alternativeQuery != null) {
                sugiliteOperation.setAlternativeTargetUIElementDataDescriptionQuery(alternativeQuery);
            }
            SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
            operationBlock.setOperation(sugiliteOperation);
            operationBlock.setFeaturePack(featurePack);
            operationBlock.setScreenshot(featurePack.screenshot);
            operationBlock.setDescription(ontologyDescriptionGenerator.getSpannedDescriptionForOperation(sugiliteOperation, query));
            return operationBlock;
        }

        else if(opeartionType == SugiliteOperation.LONG_CLICK) {
            SugiliteLongClickOperation sugiliteOperation = new SugiliteLongClickOperation();
            sugiliteOperation.setQuery(query);
            if (alternativeQuery != null) {
                sugiliteOperation.setAlternativeTargetUIElementDataDescriptionQuery(alternativeQuery);
            }
            SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
            operationBlock.setOperation(sugiliteOperation);
            operationBlock.setFeaturePack(featurePack);
            operationBlock.setScreenshot(featurePack.screenshot);
            operationBlock.setDescription(ontologyDescriptionGenerator.getSpannedDescriptionForOperation(sugiliteOperation, query));
            return operationBlock;
        }

        else {
            throw new RuntimeException("got an unsupported operation type: " + opeartionType);
        }
    }

    public SugiliteOperationBlock getBinaryOperationBlockWithOntologyQueryFromQuery(OntologyQuery query, int operationType, SugiliteAvailableFeaturePack featurePack, String arg0){
        if(operationType == SugiliteOperation.SET_TEXT) {
            SugiliteSetTextOperation sugiliteSetTextOperation = new SugiliteSetTextOperation();
            sugiliteSetTextOperation.setParameter0(arg0);
            sugiliteSetTextOperation.setQuery(query);

            SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
            operationBlock.setOperation(sugiliteSetTextOperation);
            operationBlock.setFeaturePack(featurePack);
            operationBlock.setScreenshot(featurePack.screenshot);
            operationBlock.setDescription(ontologyDescriptionGenerator.getSpannedDescriptionForOperation(sugiliteSetTextOperation, query));
            return operationBlock;
        }

        else if(operationType == SugiliteOperation.READ_OUT) {
            SugiliteReadoutOperation sugiliteReadoutOperation = new SugiliteReadoutOperation();
            sugiliteReadoutOperation.setParameter0(arg0);
            sugiliteReadoutOperation.setQuery(query);

            SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
            operationBlock.setOperation(sugiliteReadoutOperation);
            operationBlock.setFeaturePack(featurePack);
            operationBlock.setScreenshot(featurePack.screenshot);
            operationBlock.setDescription(ontologyDescriptionGenerator.getSpannedDescriptionForOperation(sugiliteReadoutOperation, query));
            return operationBlock;
        }

        else {
            throw new RuntimeException("got an unsupported operation type: " + operationType);
        }
    }
    public static OntologyQuery getFirstNonTextQuery (List<Pair<OntologyQuery, Double>> list) {
        for (Pair<OntologyQuery, Double> queryScorePair :list) {
            OntologyQuery ontologyQuery = queryScorePair.first;
            if (!checkIfOntologyQueryContainsRelations(ontologyQuery, SugiliteRelation.HAS_TEXT, SugiliteRelation.HAS_CHILD_TEXT, SugiliteRelation.HAS_CONTENT_DESCRIPTION)) {
                return ontologyQuery;
            }
        }
        return null;
    }
    public static boolean checkIfOntologyQueryContainsHashedQuery (OntologyQuery ontologyQuery) {
        if (ontologyQuery instanceof OntologyQueryWithSubQueries) {
            for (OntologyQuery subQuery : ((OntologyQueryWithSubQueries) ontologyQuery).getSubQueries()) {
                if (checkIfOntologyQueryContainsHashedQuery(subQuery)) {
                    return true;
                }
            }
        }

        if (ontologyQuery instanceof PrivateNonPrivateLeafOntologyQueryPairWrapper) {
            return checkIfOntologyQueryContainsHashedQuery(((PrivateNonPrivateLeafOntologyQueryPairWrapper) ontologyQuery).getQueryInUse());
        }

        if (ontologyQuery instanceof HashedStringLeafOntologyQuery) {
            return true;
        }

        return false;
    }


    public static boolean checkIfOntologyQueryContainsRelations (OntologyQuery ontologyQuery, SugiliteRelation... relations) {
        if (ontologyQuery instanceof OntologyQueryWithSubQueries) {
            for (OntologyQuery subQuery : ((OntologyQueryWithSubQueries) ontologyQuery).getSubQueries()) {
                if (checkIfOntologyQueryContainsRelations(subQuery, relations)) {
                    return true;
                }
            }
        }

        if (ontologyQuery instanceof LeafOntologyQuery) {
            for (SugiliteRelation relation : relations) {
                if (relation.equals(((LeafOntologyQuery) ontologyQuery).getR())) {
                    return true;
                }
            }
        }

        if (ontologyQuery instanceof HashedStringLeafOntologyQuery) {
            for (SugiliteRelation relation : relations) {
                if (relation.equals(((HashedStringLeafOntologyQuery) ontologyQuery).getR())) {
                    return true;
                }
            }
        }

        if (ontologyQuery instanceof StringAlternativeOntologyQuery) {
            for (SugiliteRelation relation : relations) {
                if (relation.equals(((StringAlternativeOntologyQuery) ontologyQuery).getR())) {
                    return true;
                }
            }
        }

        if (ontologyQuery instanceof PrivateNonPrivateLeafOntologyQueryPairWrapper) {
            return checkIfOntologyQueryContainsRelations(((PrivateNonPrivateLeafOntologyQueryPairWrapper) ontologyQuery).getQueryInUse(), relations);
        }

        return false;
    }

    public static List<Pair<OntologyQuery, Double>> newGenerateDefaultQueries(UISnapshot uiSnapshot, SugiliteEntity<Node> targetEntity, SugiliteRelation... relationsToExcludeArray){
        List<Pair<OntologyQuery, Double>> result = new ArrayList<>();

        //add All results from the legacy data description query generator
        result.addAll(generateDefaultQueries(uiSnapshot, targetEntity, relationsToExcludeArray));

        //also handle spatial relations - initially support CONTAINS, RIGHT, LEFT, ABOVE, BELOW
        SugiliteRelation[] relationsToInclude = new SugiliteRelation[]{SugiliteRelation.CONTAINS, SugiliteRelation.RIGHT, SugiliteRelation.LEFT, SugiliteRelation.ABOVE, SugiliteRelation.BELOW};
        List<Pair<SugiliteEntity<Node>, SugiliteRelation>> nodesWithSpatialRelations = new ArrayList<>();
        for (SugiliteRelation relation : relationsToInclude) {
            //query the UI graph
            Set<SugiliteTriple> allTriplesWithTheTargetAsSubject = uiSnapshot.getSubjectTriplesMap().get(targetEntity.getEntityId());
            for (SugiliteTriple triple : allTriplesWithTheTargetAsSubject) {
                if (triple.getObject().equals(targetEntity)) {
                    continue;
                }
                if (triple.getPredicate().equals(relation)) {
                    nodesWithSpatialRelations.add(new Pair<>(triple.getObject(), triple.getPredicate()));
                }
            }
        }

        //process nodesWithSpatialRelations to generate possible data description queries
        for (Pair<SugiliteEntity<Node>, SugiliteRelation> nodeWithSpatialRelation : nodesWithSpatialRelations) {
            Set<SugiliteRelation> excludedRelations = new HashSet<>(Arrays.asList(relationsToExcludeArray));
            excludedRelations.add(SugiliteRelation.HAS_PARENT_LOCATION);
            excludedRelations.add(SugiliteRelation.HAS_SCREEN_LOCATION);
            SugiliteRelation excludedRelationArray[] = new SugiliteRelation[excludedRelations.size()];
            excludedRelationArray = excludedRelations.toArray(excludedRelationArray);

            List<Pair<OntologyQuery, Double>> possibleQueries = generateDefaultQueries(uiSnapshot, nodeWithSpatialRelation.first, excludedRelationArray);
            for (Pair<OntologyQuery, Double> possibleQuery : possibleQueries) {
                //add className and packageName constraints
                CombinedOntologyQuery q = new CombinedOntologyQuery(CombinedOntologyQuery.RelationType.AND);

                if (! excludedRelations.contains(SugiliteRelation.HAS_CLASS_NAME)) {
                    if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_CLASS_NAME)) != null) {
                        //add className
                        LeafOntologyQuery subQuery = new LeafOntologyQuery();
                        Set<SugiliteEntity> object = new HashSet<>();
                        object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_CLASS_NAME))));
                        subQuery.setObjectSet(object);
                        subQuery.setQueryFunction(SugiliteRelation.HAS_CLASS_NAME);
                        q.addSubQuery(subQuery);
                    }
                }

                //add query
                CombinedOntologyQuery subQuery = new CombinedOntologyQuery(CombinedOntologyQuery.RelationType.PREV);
                subQuery.setQueryFunction(nodeWithSpatialRelation.second);
                subQuery.addSubQuery(possibleQuery.first);

                q.addSubQuery(subQuery);


                //test if the query returns and ONLY returns the target
                Set<SugiliteEntity> executionResult = q.executeOn(uiSnapshot);
                if (executionResult.size() == 1 && executionResult.contains(targetEntity)) {
                    result.add(new Pair<>(q, possibleQuery.second + 2));
                }
            }
        }

        Collections.sort(result, new Comparator<Pair<OntologyQuery, Double>>() {
            @Override
            public int compare(Pair<OntologyQuery, Double> o1, Pair<OntologyQuery, Double> o2) {
                if(o1.second > o2.second) return 1;
                else if (o1.second.equals(o2.second)) return 0;
                else return -1;
            }
        });

        return result;
    }

    private static  <T> T getValueIfOnlyOneObject (Collection<T> collection) {
        if (collection != null && collection.size() == 1) {
            List<T> list = new ArrayList<>(collection);
            return list.get(0);
        }
        return null;
    }


    public static List<Pair<OntologyQuery, Double>> generateDefaultQueries(UISnapshot uiSnapshot, SugiliteEntity<Node> targetEntity, SugiliteRelation... relationsToExcludeArray){
        Set<SugiliteRelation> relationsToExclude = new HashSet<>(Arrays.asList(relationsToExcludeArray));
        //generate parent query
        List<Pair<OntologyQuery, Double>> queries = new ArrayList<>();
        CombinedOntologyQuery q = new CombinedOntologyQuery(CombinedOntologyQuery.RelationType.AND);
        boolean hasNonBoundingBoxFeature = false;
        boolean hasNonChildFeature = false;

        SugiliteEntity<Node> foundEntity = targetEntity;


        //generate sub queries -- add the packageName and className constraints to q

        if (! relationsToExclude.contains(SugiliteRelation.HAS_PACKAGE_NAME)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_PACKAGE_NAME)) != null) {
                //add packageName
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_PACKAGE_NAME))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_PACKAGE_NAME);
                q.addSubQuery(subQuery);
            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_CLASS_NAME)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_CLASS_NAME)) != null) {
                //add className
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_CLASS_NAME))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_CLASS_NAME);
                q.addSubQuery(subQuery);
            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_TEXT)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_TEXT)) != null) {
                //add a text query
                CombinedOntologyQuery clonedQuery = q.clone();
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_TEXT))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_TEXT);
                clonedQuery.addSubQuery(subQuery);
                hasNonBoundingBoxFeature = true;
                hasNonChildFeature = true;
                queries.add(Pair.create(clonedQuery, 1.1));
            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_CONTENT_DESCRIPTION)) {
            if ((getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_CONTENT_DESCRIPTION)) != null) &&
                    (!getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_CONTENT_DESCRIPTION)).equals(getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_TEXT))))) {
                //add content description
                CombinedOntologyQuery clonedQuery = q.clone();
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_CONTENT_DESCRIPTION))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_CONTENT_DESCRIPTION);
                clonedQuery.addSubQuery(subQuery);
                hasNonBoundingBoxFeature = true;
                hasNonChildFeature = true;
                queries.add(Pair.create(clonedQuery, 1.2));
            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_VIEW_ID)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_VIEW_ID)) != null) {
                //add view id
                CombinedOntologyQuery clonedQuery = q.clone();
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_VIEW_ID))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_VIEW_ID);
                clonedQuery.addSubQuery(subQuery);
                hasNonBoundingBoxFeature = true;
                hasNonChildFeature = true;
                queries.add(Pair.create(clonedQuery, 3.2));
            }
        }


        if(foundEntity != null && uiSnapshot != null){
            //add list order
            System.out.println("Found entity and have a non-null uiSnapshot");


            if (! relationsToExclude.contains(SugiliteRelation.HAS_LIST_ORDER)) {
                Set<SugiliteTriple> triples = uiSnapshot.getSubjectPredicateTriplesMap().get(new AbstractMap.SimpleEntry<>(foundEntity.getEntityId(), SugiliteRelation.HAS_LIST_ORDER.getRelationId()));
                if (triples != null) {
                    for (SugiliteTriple triple : triples) {
                        String order = triple.getObject().getEntityValue().toString();

                        CombinedOntologyQuery clonedQuery = q.clone();
                        LeafOntologyQuery subQuery = new LeafOntologyQuery();
                        Set<SugiliteEntity> object = new HashSet<>();
                        object.add(new SugiliteEntity(-1, String.class, order));
                        subQuery.setObjectSet(object);
                        subQuery.setQueryFunction(SugiliteRelation.HAS_LIST_ORDER);
                        clonedQuery.addSubQuery(subQuery);
                        hasNonBoundingBoxFeature = true;
                        hasNonChildFeature = true;
                        queries.add(Pair.create(clonedQuery, 3.0));
                    }
                }
            }

            if (! relationsToExclude.contains(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER)) {
                Set<SugiliteTriple> triples2 = uiSnapshot.getSubjectPredicateTriplesMap().get(new AbstractMap.SimpleEntry<>(foundEntity.getEntityId(), SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER.getRelationId()));
                if (triples2 != null) {
                    for (SugiliteTriple triple : triples2) {
                        String order = triple.getObject().getEntityValue().toString();

                        CombinedOntologyQuery clonedQuery = q.clone();
                        LeafOntologyQuery subQuery = new LeafOntologyQuery();
                        Set<SugiliteEntity> object = new HashSet<>();
                        object.add(new SugiliteEntity(-1, String.class, order));
                        subQuery.setObjectSet(object);
                        subQuery.setQueryFunction(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER);
                        clonedQuery.addSubQuery(subQuery);
                        hasNonBoundingBoxFeature = true;
                        hasNonChildFeature = true;
                        queries.add(Pair.create(clonedQuery, 3.1));
                    }
                }
            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_CHILD_TEXT)) {
            //add child text
            List<String> childTexts = new ArrayList<>(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_CHILD_TEXT));
            if (childTexts != null && childTexts.size() > 0) {
                int count = 0;
                double score = 2.01 + (((double) (count++)) / (double) childTexts.size());
                Set<String> homeScreenPackageNames = new HashSet<>(Arrays.asList(Const.HOME_SCREEN_PACKAGE_NAMES));
                //TODO: in case of multiple childText queries, get all possible combinations

                for (String childText : childTexts) {
                    if (childText != null && !childText.equals(relationsToExclude.contains(SugiliteRelation.HAS_TEXT))) {
                        CombinedOntologyQuery clonedQuery = q.clone();
                        LeafOntologyQuery subQuery = new LeafOntologyQuery();
                        Set<SugiliteEntity> object = new HashSet<>();
                        object.add(new SugiliteEntity(-1, String.class, childText));
                        subQuery.setObjectSet(object);
                        subQuery.setQueryFunction(SugiliteRelation.HAS_CHILD_TEXT);
                        clonedQuery.addSubQuery(subQuery);
                        double newScore = score;
                        if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_PACKAGE_NAME)) != null
                                && homeScreenPackageNames.contains(getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_PACKAGE_NAME)))) {
                            newScore = score - 1;
                        }
                        queries.add(Pair.create(clonedQuery, newScore));
                        hasNonBoundingBoxFeature = true;
                    }
                }
            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_SCREEN_LOCATION)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_SCREEN_LOCATION)) != null) {
                CombinedOntologyQuery clonedQuery = q.clone();
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_SCREEN_LOCATION))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_SCREEN_LOCATION);
                clonedQuery.addSubQuery(subQuery);
                queries.add(Pair.create(clonedQuery, 100.0));
            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_PARENT_LOCATION)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_PARENT_LOCATION)) != null) {
                CombinedOntologyQuery clonedQuery = q.clone();
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_PARENT_LOCATION))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_PARENT_LOCATION);
                clonedQuery.addSubQuery(subQuery);
                queries.add(Pair.create(clonedQuery, 101.0));
            }
        }


        Collections.sort(queries, new Comparator<Pair<OntologyQuery, Double>>() {
            @Override
            public int compare(Pair<OntologyQuery, Double> o1, Pair<OntologyQuery, Double> o2) {
                if(o1.second > o2.second) return 1;
                else if (o1.second.equals(o2.second)) return 0;
                else return -1;
            }
        });
        // serialize the query
        return queries;
    }

    /**
     * save the block and the corresponding feature pack
     * @param block
     * @param featurePack
     */
    public void saveBlock(SugiliteOperationBlock block, SugiliteAvailableFeaturePack featurePack){
        block.setPreviousBlock(sugiliteData.getCurrentScriptBlock());
        if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteOperationBlock) {
            ((SugiliteOperationBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(block);
        }
        else if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteStartingBlock) {
            ((SugiliteStartingBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(block);
        }
        else if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteErrorHandlingForkBlock){
            ((SugiliteErrorHandlingForkBlock) sugiliteData.getCurrentScriptBlock()).setAlternativeNextBlock(block);
        }
        else if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteSpecialOperationBlock){
            ((SugiliteSpecialOperationBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(block);
        }
        else if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteConditionBlock){
            ((SugiliteConditionBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(block);
        }
        else{
            throw new RuntimeException("Unsupported Block Type!");
        }

        //construct a SugiliteBlockMetaInfo for the operation block
        SugiliteBlockMetaInfo metaInfo = new SugiliteBlockMetaInfo(block, featurePack.serializableUISnapshot, featurePack.targetNodeEntity);

        //add the SugiliteBlockMetaInfo to the operation block
        block.setSugiliteBlockMetaInfo(metaInfo);

        sugiliteData.setCurrentScriptBlock(block);


        try {
            sugiliteData.getScriptHead().relevantPackages.add(featurePack.packageName);
            sugiliteScriptDao.save(sugiliteData.getScriptHead());
        } catch (Exception e) {
            e.printStackTrace();
        }



        //save the meta info into a file to check if the metaInfo is constructed correctly
        //saveMetaInfoToFile(metaInfo);

        System.out.println("saved block");
    }


    //for testing purpose
    private void saveMetaInfoToFile(SugiliteBlockMetaInfo metaInfo){
        Gson gson = new Gson();
        String metaInfoJson = gson.toJson(metaInfo);

        PrintWriter out1 = null;
        try {
            File f = new File("/sdcard/Download/sugilite_metainfo");
            if (!f.exists() || !f.isDirectory()) {
                f.mkdirs();
                System.out.println("dir created");
            }
            System.out.println(f.getAbsolutePath());


            Date time = Calendar.getInstance().getTime();
            String timeString = Const.dateFormat.format(time);

            File metaInfoFile = new File(f.getPath() + "/metainfo_" + timeString + ".json");

            if (!metaInfoFile.exists()) {
                metaInfoFile.getParentFile().mkdirs();
                metaInfoFile.createNewFile();
                System.out.println("file created");
            }



            out1 = new PrintWriter(new FileOutputStream(metaInfoFile), true);
            out1.println(metaInfoJson);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out1 != null) out1.close();
        }
    }


    public Map<SugiliteOperationBlock, Spanned> getDescriptionsInDifferences(SugiliteOperationBlock[] blocks){
        Map<SugiliteOperationBlock, Spanned> results = new HashMap<>();
        for(SugiliteOperationBlock operationBlock : blocks){
            OntologyQuery query = operationBlock.getOperation().getDataDescriptionQueryIfAvailable();
            if(query != null) {
                results.put(operationBlock, ontologyDescriptionGenerator.getSpannedDescriptionForOperation(operationBlock.getOperation(), stripOntologyQuery(query)));
            }
        }
        return results;
    }

    public Map<OntologyQuery, String> getDescriptionsInDifferences(List<OntologyQuery> queries){
        Map<OntologyQuery, String> results = new HashMap<>();
        for(OntologyQuery query : queries){
            results.put(query, query.toString());
        }
        return results;
    }

    /**
     * returns a COPY of the query where the has_class_name and has_package_name subqueries are removed
     * @param query
     * @return
     */
    public static OntologyQuery stripOntologyQuery(OntologyQuery query){
        // TODO make this work recursively?
        if (query instanceof CombinedOntologyQuery) {
            OntologyQueryWithSubQueries queryCloned = ((CombinedOntologyQuery)query).clone();

            List<OntologyQuery> queriesToRemove = new ArrayList<>();
            for (OntologyQuery subQuery : queryCloned.getSubQueries()) {
                if (subQuery != null && subQuery instanceof LeafOntologyQuery) {
                    LeafOntologyQuery loq = (LeafOntologyQuery)subQuery;
                    SugiliteRelation r = loq.getR();
                    /*
                    if (r.equals(SugiliteRelation.HAS_CLASS_NAME)) {
                        queriesToRemove.add(subQuery);
                    }
                    */
                    if (r.equals(SugiliteRelation.HAS_PACKAGE_NAME)) {
                        queriesToRemove.add(subQuery);
                    }
                }
            }
            for (OntologyQuery queryToRemove : queriesToRemove) {
                queryCloned.getSubQueries().remove(queryToRemove);
            }
            if (queryCloned.getSubQueries().size() == 1) {
                for (OntologyQuery query1 : queryCloned.getSubQueries()) {
                    return query1;
                }
            }
            return queryCloned;
        } else {
            // TODO what if its not OntologyQueryWithSubQueries?
            return query.clone();
        }
    }

}
