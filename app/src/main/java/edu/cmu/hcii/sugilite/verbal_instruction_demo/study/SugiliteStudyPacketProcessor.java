package edu.cmu.hcii.sugilite.verbal_instruction_demo.study;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableTriple;
import edu.cmu.hcii.sugilite.ontology.helper.ListOrderResolver;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteNodeAnnotator;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteTextParentAnnotator;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.util.MyRect;

/**
 * @author toby
 * @date 4/18/18
 * @time 5:24 PM
 */
public class SugiliteStudyPacketProcessor {
    static private SugiliteStudyPacket readPacket(File sourceFile) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(sourceFile))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
            String jsonString = contentBuilder.toString();
            SugiliteStudyPacket sugiliteStudyPacket = new Gson().fromJson(jsonString, SugiliteStudyPacket.class);
            return sugiliteStudyPacket;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void parseAndAddNewTextRelations(SugiliteSerializableEntity<String> stringEntity, SerializableUISnapshot uiSnapshot) {
        SugiliteTextParentAnnotator sugiliteTextParentAnnotator = SugiliteTextParentAnnotator.getInstance();
        if (!(stringEntity.getEntityValue() instanceof String)) {
            return;
        }
        List<SugiliteTextParentAnnotator.AnnotatingResult> results = sugiliteTextParentAnnotator.annotate(stringEntity.getEntityValue());
        for (SugiliteTextParentAnnotator.AnnotatingResult result : results) {

            //insert the new triples for the relations in the annotating results
            String objectString = result.getMatchedString();
            if (result.getNumericValue() != null) {
                objectString = String.valueOf(result.getNumericValue());
            }
            SugiliteRelation sugiliteRelation = result.getRelation();

            //TODO: add triples for all parents of the string Entity
            Set<String> parentNodeEntitySubjectId = new HashSet<>();
            Map<String, Set<SugiliteSerializableTriple>> tripleMap = uiSnapshot.getObjectTriplesMap();
            if (tripleMap != null) {
                Set<SugiliteSerializableTriple> allTriplesWithMatchedObject = tripleMap.get("@" + stringEntity.getEntityId().toString());
                if (allTriplesWithMatchedObject != null) {
                    for (SugiliteSerializableTriple triple : allTriplesWithMatchedObject) {
                        if (triple.getPredicateStringValue().equals(SugiliteRelation.HAS_CHILD_TEXT.getRelationName()) || triple.getPredicateStringValue().equals(SugiliteRelation.HAS_TEXT.getRelationName())) {
                            parentNodeEntitySubjectId.add(triple.getSubjectId());
                        }
                    }
                }
            }

            //only add text-based relations to string entities
            uiSnapshot.addTriple(new SugiliteSerializableTriple("@" + stringEntity.getEntityId().toString(), objectString, sugiliteRelation.getRelationName()));
            /*
            for (String entitySubjectId : parentNodeEntitySubjectId) {
                uiSnapshot.addTriple(new SugiliteSerializableTriple(entitySubjectId, objectString, sugiliteRelation.getRelationName()));
            }
            */
        }
    }

    static private SugiliteStudyPacket reParse(SugiliteStudyPacket packet, int screenWidth, int screenHeight) {
        SerializableUISnapshot uiSnapshot = packet.getUiSnapshot();
        //work on triples

        //add text-based relations
        for (SugiliteSerializableEntity serializableEntity : uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().values()) {
            parseAndAddNewTextRelations(serializableEntity, uiSnapshot);
        }

        //add spatial relations
        Map<String, SugiliteEntity<Node>> tempNodeEntities = new HashMap<>();
        for(SugiliteSerializableEntity sugiliteSerializableEntity : uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().values()){
            if (sugiliteSerializableEntity.getEntityValue() instanceof Map){
                try{
                    String jsonString = new Gson().toJson(sugiliteSerializableEntity.getEntityValue());
                    Node node = new Gson().fromJson(jsonString, Node.class);
                    SugiliteEntity<Node> newEntity = new SugiliteEntity<>(sugiliteSerializableEntity.getEntityId(), Node.class, node);
                    tempNodeEntities.put(newEntity.getEntityId().toString(), newEntity);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        Map<String, SugiliteEntity<Node>> entityIdEntityMap = new HashMap<>();
        entityIdEntityMap.putAll(tempNodeEntities);

        SugiliteNodeAnnotator nodeAnnotator = SugiliteNodeAnnotator.getInstance();

        for (SugiliteNodeAnnotator.NodeAnnotatingResult res : nodeAnnotator.annotate(tempNodeEntities.values(), screenWidth, screenHeight)) {
            uiSnapshot.addTriple(new SugiliteSerializableTriple("@" + res.getSubject().getEntityId().toString(), "@" + res.getObjectEntity().getEntityId().toString(), res.getRelation().getRelationName()));
        }

        //add index-based relations
        for(SugiliteEntity<Node> nodeEntity : entityIdEntityMap.values()){
            if(nodeEntity == null || nodeEntity.getEntityValue() == null){
                continue;
            }
            // TODO: add order in list info
            ListOrderResolver listOrderResolver = new ListOrderResolver();

            //get all triples whose subject is node entity, and predicate is HAS_CHILD
            Set<SugiliteSerializableTriple> triples = new HashSet<>();
            for(SugiliteSerializableTriple triple : uiSnapshot.getTriples()){
                if(triple.getSubjectId().equals("@" + nodeEntity.getEntityId()) && triple.getPredicateStringValue().equals(SugiliteRelation.HAS_CHILD.getRelationName())){
                    triples.add(triple);
                }
            }

            Map<Node, String> childNodes = new HashMap<>();
            if(triples != null) {
                for (SugiliteSerializableTriple triple : triples) {
                    try {
                        String childEntityId = triple.getObjectStringValue().replace("@", "");
                        MyRect rect = MyRect.unflattenFromString(entityIdEntityMap.get(childEntityId).getEntityValue().getBoundsInScreen());
                        int size = rect.width() * rect.height();
                        if (size > 0) {
                            childNodes.put(entityIdEntityMap.get(childEntityId).getEntityValue(), childEntityId);
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }

                if (listOrderResolver.isAList(nodeEntity.getEntityValue(), childNodes.keySet())) {
                    uiSnapshot.addTriple(new SugiliteSerializableTriple("@" + nodeEntity.getEntityId().toString(), "true", SugiliteRelation.IS_A_LIST.getRelationName()));
                    addOrderForChildren(childNodes, uiSnapshot, entityIdEntityMap);
                }
            }
        }


        //clear indexes
        uiSnapshot.setSubjectTriplesMap(null);
        uiSnapshot.setObjectTriplesMap(null);
        uiSnapshot.setPredicateTriplesMap(null);
        uiSnapshot.setSugiliteRelationIdSugiliteRelationMap(null);
        return packet;
    }

    private static void addOrderForChildren(Map<Node, String> children, SerializableUISnapshot uiSnapshot, Map<String, SugiliteEntity<Node>> tempNodeEntities){
        //add list order for list items
        List<Map.Entry<Node, Integer>> childNodeYValueList = new ArrayList<>();
        for(Node childNode : children.keySet()){
            childNodeYValueList.add(new AbstractMap.SimpleEntry<>(childNode, Integer.valueOf(childNode.getBoundsInScreen().split(" ")[1])));
        }
        childNodeYValueList.sort(new Comparator<Map.Entry<Node, Integer>>() {
            @Override
            public int compare(Map.Entry<Node, Integer> o1, Map.Entry<Node, Integer> o2) {
                return o1.getValue() - o2.getValue();
            }
        });
        int counter = 0;
        for(Map.Entry<Node, Integer> entry : childNodeYValueList){
            counter ++;
            Node childNode = entry.getKey();
            String subjectEntityId = children.get(childNode);
            uiSnapshot.addTriple(new SugiliteSerializableTriple("@" + subjectEntityId.toString(), String.valueOf(counter), SugiliteRelation.HAS_LIST_ORDER.getRelationName()));

            SugiliteSerializableEntity<Node> childEntity = uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().get("@" + entry.getValue());
            if(childEntity != null){
                for(String entityId : getAllChildEntityIds(childEntity, new HashSet<String>(), uiSnapshot, tempNodeEntities)){
                    //addEntityStringTriple(entity, String.valueOf(counter), SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER);
                    uiSnapshot.addTriple(new SugiliteSerializableTriple(entityId, String.valueOf(counter), SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER.getRelationName()));
                }
            }
        }
    }

    private static Set<String> getAllChildEntityIds(SugiliteSerializableEntity<Node> node, Set<String> coveredNodes, SerializableUISnapshot uiSnapshot, Map<String, SugiliteEntity<Node>> tempNodeEntities){
        Set<String> results = new HashSet<>();
        Set<SugiliteSerializableTriple> triples = new HashSet<>();
        for(SugiliteSerializableTriple triple : uiSnapshot.getTriples())
        {
            if(triple.getSubjectId().equals("@" + node.getEntityId().toString()) && triple.getPredicateStringValue().equals(SugiliteRelation.HAS_CHILD.getRelationName())){
                triples.add(triple);
            }
        }
        if(triples != null) {
            for (SugiliteSerializableTriple triple : triples) {
                try {
                    if (coveredNodes.contains(triple.getObjectStringValue())) {
                        continue;
                    }
                    results.add(triple.getObjectStringValue());
                    coveredNodes.add(triple.getObjectStringValue());
                    results.addAll(getAllChildEntityIds(new SugiliteSerializableEntity<>(tempNodeEntities.get(triple.getObjectStringValue().replace("@", ""))), coveredNodes, uiSnapshot, tempNodeEntities));
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        coveredNodes.addAll(results);
        return results;
    }

    private static void writeFile(File file, String string) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(string);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e) {
            }
        }
    }

    public static void main(String[] args) {

        File dir = new File("/Users/toby/20180301/sugilite_study_packets");
        int count = 0;
        if(dir.isDirectory()){
            for(File file : dir.listFiles()){
                if(! file.getName().endsWith(".json")){
                    continue;
                }
                if(file.getName().startsWith("RECONSTRUCTED")){
                    continue;
                }
                try{
                    SugiliteStudyPacket packet = readPacket(file);
                    String fileName = "RECONSTRUCTED_" + file.getName();
                    //System.out.println(dir.getAbsolutePath());
                    writeFile(new File(dir.getAbsolutePath() + "/" + fileName), new Gson().toJson(reParse(packet, 1080, 1920)));
                    System.out.println("Wrote " + count++ + " JSON files");
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }


        /*
        File testFile = new File("/Users/toby/20180301/sugilite_study_packets/packet_2018-03-09=19_47_29-235.json");
        SugiliteStudyPacket packet = readPacket(testFile);
        SugiliteStudyPacket newPacket = reParse(packet);
        //System.out.println(dir.getAbsolutePath());
        writeFile(new File("test3.json"), new Gson().toJson(reParse(packet)));
        */
    }
}