package edu.cmu.hcii.sugilite.verbal_instruction_demo.study;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import edu.cmu.hcii.sugilite.ontology.SugiliteTriple;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteNodeAnnotator;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteTextAnnotator;

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
        SugiliteTextAnnotator sugiliteTextAnnotator = new SugiliteTextAnnotator(true);
        if (!(stringEntity.getEntityValue() instanceof String)) {
            return;
        }
        List<SugiliteTextAnnotator.AnnotatingResult> results = sugiliteTextAnnotator.annotate(stringEntity.getEntityValue());
        for (SugiliteTextAnnotator.AnnotatingResult result : results) {

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

    static private SugiliteStudyPacket reParse(SugiliteStudyPacket packet) {
        SerializableUISnapshot uiSnapshot = packet.getUiSnapshot();
        //work on triples

        //add text-based relations
        for (SugiliteSerializableEntity serializableEntity : uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().values()) {
            parseAndAddNewTextRelations(serializableEntity, uiSnapshot);
        }

        //add spatial relations
        Set<SugiliteEntity<Node>> tempNodeEntities = new HashSet<>();
        for(SugiliteSerializableEntity sugiliteSerializableEntity : uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().values()){
            if (sugiliteSerializableEntity.getEntityValue() instanceof Map){
                try{
                    String jsonString = new Gson().toJson(sugiliteSerializableEntity.getEntityValue());
                    Node node = new Gson().fromJson(jsonString, Node.class);
                    SugiliteEntity<Node> newEntity = new SugiliteEntity<>(sugiliteSerializableEntity.getEntityId(), Node.class, node);
                    tempNodeEntities.add(newEntity);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        SugiliteNodeAnnotator nodeAnnotator = new SugiliteNodeAnnotator();
        for (SugiliteNodeAnnotator.AnnotatingResult res : nodeAnnotator.annotate(tempNodeEntities)) {
            uiSnapshot.addTriple(new SugiliteSerializableTriple("@" + res.getSubject().getEntityId().toString(), "@" + res.getObjectEntity().getEntityId().toString(), res.getRelation().getRelationName()));
        }


        //clear indexes
        uiSnapshot.setSubjectTriplesMap(null);
        uiSnapshot.setObjectTriplesMap(null);
        uiSnapshot.setPredicateTriplesMap(null);
        uiSnapshot.setSugiliteRelationIdSugiliteRelationMap(null);
        return packet;
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
        System.out.println("hello world");
        File testFile = new File("/Users/toby/20180301/sugilite_study_packets/packet_2018-03-01=15_21_56-739.json");
        SugiliteStudyPacket packet = readPacket(testFile);
        writeFile(new File("test.json"), new Gson().toJson(reParse(packet)));
    }
}