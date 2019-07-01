package edu.cmu.hcii.sugilite.recording.newrecording;

import android.content.Context;
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
import edu.cmu.hcii.sugilite.model.block.special_operation.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteClickOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteUnaryOperation;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteTriple;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;

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
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator(context);
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        }
        else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        }
        readableDescriptionGenerator = new ReadableDescriptionGenerator(context);
    }

    public SugiliteOperationBlock getOperationBlockFromQuery(SerializableOntologyQuery query, int opeartionType, SugiliteAvailableFeaturePack featurePack){
        if(opeartionType == SugiliteOperation.CLICK) {
            SugiliteClickOperation sugiliteOperation = new SugiliteClickOperation();
            sugiliteOperation.setOperationType(opeartionType);
            final SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
            operationBlock.setOperation(sugiliteOperation);
            operationBlock.setFeaturePack(featurePack);
            sugiliteOperation.setQuery(query);
            operationBlock.setScreenshot(featurePack.screenshot);

            //description is set
            //TODO: fix
            //operationBlock.setDescription(operationBlock.toString());
            operationBlock.setDescription(ontologyDescriptionGenerator.getDescriptionForOperation(sugiliteOperation, query));
            return operationBlock;
        } else {
            throw new RuntimeException("got an unsupported operation type");
        }
    }

    public static List<Pair<SerializableOntologyQuery, Double>> generateDefaultQueries(SugiliteAvailableFeaturePack featurePack, UISnapshot uiSnapshot, boolean excludeHasTextRelation){
        //generate parent query
        List<Pair<SerializableOntologyQuery, Double>> queries = new ArrayList<>();
        OntologyQuery q = new OntologyQuery(OntologyQuery.relationType.AND);
        boolean hasNonBoundingBoxFeature = false;
        boolean hasNonChildFeature = false;

        SugiliteEntity<Node> foundEntity = null;
        if(uiSnapshot != null) {
            for (Map.Entry<Node, SugiliteEntity<Node>> entityEntry : uiSnapshot.getNodeSugiliteEntityMap().entrySet()) {
                if (entityEntry.getKey().getBoundsInScreen().equals(featurePack.boundsInScreen) &&
                        entityEntry.getKey().getClassName().equals(featurePack.className)) {
                    //found
                    foundEntity = entityEntry.getValue();
                    break;
                }
            }
        }


        //generate sub queries -- add the packageName and className constraints to q

        if(featurePack.packageName != null && (!featurePack.packageName.equals("NULL"))){
            //add packageName
            OntologyQuery subQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
            Set<SugiliteEntity> object = new HashSet<>();
            object.add(new SugiliteEntity(-1, String.class, featurePack.packageName));
            subQuery.setObject(object);
            subQuery.setQueryFunction(SugiliteRelation.HAS_PACKAGE_NAME);
            q.addSubQuery(subQuery);
        }

        if(featurePack.className != null && (!featurePack.className.equals("NULL"))){
            //add className
            OntologyQuery subQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
            Set<SugiliteEntity> object = new HashSet<>();
            object.add(new SugiliteEntity(-1, String.class, featurePack.className));
            subQuery.setObject(object);
            subQuery.setQueryFunction(SugiliteRelation.HAS_CLASS_NAME);
            q.addSubQuery(subQuery);
        }

        if (!excludeHasTextRelation) {
            if (featurePack.text != null && (!featurePack.text.equals("NULL"))) {
                //add a text query
                OntologyQuery clonedQuery = new OntologyQuery(new SerializableOntologyQuery(q));
                OntologyQuery subQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, featurePack.text));
                subQuery.setObject(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_TEXT);
                clonedQuery.addSubQuery(subQuery);
                hasNonBoundingBoxFeature = true;
                hasNonChildFeature = true;
                queries.add(Pair.create(new SerializableOntologyQuery(clonedQuery), 1.1));
            }
        }

        if(featurePack.contentDescription != null && (!featurePack.contentDescription.equals("NULL")) && (!featurePack.contentDescription.equals(featurePack.text))){
            //add content description
            OntologyQuery clonedQuery = new OntologyQuery(new SerializableOntologyQuery(q));
            OntologyQuery subQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
            Set<SugiliteEntity> object = new HashSet<>();
            object.add(new SugiliteEntity(-1, String.class, featurePack.contentDescription));
            subQuery.setObject(object);
            subQuery.setQueryFunction(SugiliteRelation.HAS_CONTENT_DESCRIPTION);
            clonedQuery.addSubQuery(subQuery);
            hasNonBoundingBoxFeature = true;
            hasNonChildFeature = true;
            queries.add(Pair.create(new SerializableOntologyQuery(clonedQuery), 1.2));
        }

        if(featurePack.viewId != null && (!featurePack.viewId.equals("NULL"))){
            //add view id
            OntologyQuery clonedQuery = new OntologyQuery(new SerializableOntologyQuery(q));
            OntologyQuery subQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
            Set<SugiliteEntity> object = new HashSet<>();
            object.add(new SugiliteEntity(-1, String.class, featurePack.viewId));
            subQuery.setObject(object);
            subQuery.setQueryFunction(SugiliteRelation.HAS_VIEW_ID);
            clonedQuery.addSubQuery(subQuery);
            hasNonBoundingBoxFeature = true;
            hasNonChildFeature = true;
            queries.add(Pair.create(new SerializableOntologyQuery(clonedQuery), 3.2));
        }


        if(foundEntity != null && uiSnapshot != null){
            //add list order
            System.out.println("Found entity and have a non-null uiSnapshot");
            Set<SugiliteTriple> triples = uiSnapshot.getSubjectPredicateTriplesMap().get(new AbstractMap.SimpleEntry<>(foundEntity.getEntityId(), SugiliteRelation.HAS_LIST_ORDER.getRelationId()));
            if(triples != null){
                for(SugiliteTriple triple : triples){
                    String order = triple.getObject().getEntityValue().toString();

                    OntologyQuery clonedQuery = new OntologyQuery(new SerializableOntologyQuery(q));
                    OntologyQuery subQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
                    Set<SugiliteEntity> object = new HashSet<>();
                    object.add(new SugiliteEntity(-1, String.class, order));
                    subQuery.setObject(object);
                    subQuery.setQueryFunction(SugiliteRelation.HAS_LIST_ORDER);
                    clonedQuery.addSubQuery(subQuery);
                    hasNonBoundingBoxFeature = true;
                    hasNonChildFeature = true;
                    queries.add(Pair.create(new SerializableOntologyQuery(clonedQuery), 3.0));
                }
            }

            Set<SugiliteTriple> triples2 = uiSnapshot.getSubjectPredicateTriplesMap().get(new AbstractMap.SimpleEntry<>(foundEntity.getEntityId(), SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER.getRelationId()));
            if(triples2 != null){
                for(SugiliteTriple triple : triples2){
                    String order = triple.getObject().getEntityValue().toString();

                    OntologyQuery clonedQuery = new OntologyQuery(new SerializableOntologyQuery(q));
                    OntologyQuery subQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
                    Set<SugiliteEntity> object = new HashSet<>();
                    object.add(new SugiliteEntity(-1, String.class, order));
                    subQuery.setObject(object);
                    subQuery.setQueryFunction(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER);
                    clonedQuery.addSubQuery(subQuery);
                    hasNonBoundingBoxFeature = true;
                    hasNonChildFeature = true;
                    queries.add(Pair.create(new SerializableOntologyQuery(clonedQuery), 3.1));
                }
            }
        }


        //add child text
        List<String> childTexts = featurePack.childTexts;
        if(childTexts != null && childTexts.size() > 0){
            int count = 0;
            double score = 2.01 + (((double)(count++)) / (double) childTexts.size());
            Set<String> homeScreenPackageNames = new HashSet<>(Arrays.asList(Const.HOME_SCREEN_PACKAGE_NAMES));
            OntologyQuery clonedQuery = new OntologyQuery(new SerializableOntologyQuery(q));
            for(String childText : childTexts){
                if(childText != null && !childText.equals(featurePack.text)) {
                    OntologyQuery subQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
                    Set<SugiliteEntity> object = new HashSet<>();
                    object.add(new SugiliteEntity(-1, String.class, childText));
                    subQuery.setObject(object);
                    subQuery.setQueryFunction(SugiliteRelation.HAS_CHILD_TEXT);
                    clonedQuery.addSubQuery(subQuery);
                    double newScore = score;
                    if(featurePack.packageName != null && homeScreenPackageNames.contains(featurePack.packageName)){
                        newScore = score - 1;
                    }
                    queries.add(Pair.create(new SerializableOntologyQuery(clonedQuery), newScore));
                    hasNonBoundingBoxFeature = true;
                }
            }
            queries.add(Pair.create(new SerializableOntologyQuery(clonedQuery), 2.0));
        }

        if(featurePack.boundsInScreen != null && (!featurePack.boundsInScreen.equals("NULL"))){
            OntologyQuery clonedQuery = new OntologyQuery(new SerializableOntologyQuery(q));
            OntologyQuery subQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
            Set<SugiliteEntity> object = new HashSet<>();
            object.add(new SugiliteEntity(-1, String.class, featurePack.boundsInScreen));
            subQuery.setObject(object);
            subQuery.setQueryFunction(SugiliteRelation.HAS_SCREEN_LOCATION);
            clonedQuery.addSubQuery(subQuery);
            queries.add(Pair.create(new SerializableOntologyQuery(clonedQuery), 5.1));
        }

        if(featurePack.boundsInParent != null && (!featurePack.boundsInParent.equals("NULL"))){
            OntologyQuery clonedQuery = new OntologyQuery(new SerializableOntologyQuery(q));
            OntologyQuery subQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
            Set<SugiliteEntity> object = new HashSet<>();
            object.add(new SugiliteEntity(-1, String.class, featurePack.boundsInParent));
            subQuery.setObject(object);
            subQuery.setQueryFunction(SugiliteRelation.HAS_PARENT_LOCATION);
            clonedQuery.addSubQuery(subQuery);
            queries.add(Pair.create(new SerializableOntologyQuery(clonedQuery), 8.2));
        }


        Collections.sort(queries, new Comparator<Pair<SerializableOntologyQuery, Double>>() {
            @Override
            public int compare(Pair<SerializableOntologyQuery, Double> o1, Pair<SerializableOntologyQuery, Double> o2) {
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
        sugiliteData.setCurrentScriptBlock(block);

        try {
            sugiliteData.getScriptHead().relevantPackages.add(featurePack.packageName);
            sugiliteScriptDao.save(sugiliteData.getScriptHead());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //construct a SugiliteBlockMetaInfo
        SugiliteBlockMetaInfo metaInfo = new SugiliteBlockMetaInfo(block, featurePack.serializableUISnapshot, featurePack.targetNodeEntity);

        //save the meta info into a file to check if the metaInfo is constructed correctly
        saveMetaInfoToFile(metaInfo);

        System.out.println("saved block");
    }


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


    public Map<SugiliteOperationBlock, String> getDescriptionsInDifferences(SugiliteOperationBlock[] blocks){
        Map<SugiliteOperationBlock, String> results = new HashMap<>();
        for(SugiliteOperationBlock operationBlock : blocks){
            SerializableOntologyQuery query = operationBlock.getOperation().getDataDescriptionQueryIfAvailable();
            if(query != null) {
                results.put(operationBlock, ontologyDescriptionGenerator.getDescriptionForOperation(operationBlock.getOperation(), stripSerializableOntologyQuery(query)));
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
    public static SerializableOntologyQuery stripSerializableOntologyQuery(SerializableOntologyQuery query){
        SerializableOntologyQuery queryCloned = new SerializableOntologyQuery(new OntologyQuery(query));
        List<SerializableOntologyQuery> queriesToRemove = new ArrayList<>();
        for(SerializableOntologyQuery subQuery : queryCloned.getSubQueries()){
            if(subQuery != null && subQuery.getR() != null) {
                /*
                if (subQuery.getR().equals(SugiliteRelation.HAS_CLASS_NAME)) {
                    queriesToRemove.add(subQuery);
                }
                */
                if (subQuery.getR().equals(SugiliteRelation.HAS_PACKAGE_NAME)) {
                    queriesToRemove.add(subQuery);
                }
            }
        }
        for(SerializableOntologyQuery queryToRemove : queriesToRemove){
            queryCloned.getSubQueries().remove(queryToRemove);
        }
        if(queryCloned.getSubQueries().size() == 1){
            for(SerializableOntologyQuery query1 : queryCloned.getSubQueries()){
                return query1;
            }
        }
        return queryCloned;
    }
}
