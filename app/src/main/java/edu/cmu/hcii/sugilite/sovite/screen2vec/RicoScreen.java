package edu.cmu.hcii.sugilite.sovite.screen2vec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableTriple;
import edu.cmu.hcii.sugilite.ontology.SugiliteTriple;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;

/**
 * @author toby
 * @date 8/18/20
 * @time 10:11 PM
 */
public class RicoScreen implements Serializable {
    String activity_name;
    String request_id;
    RicoActivity activity;
    boolean is_keyboard_deployed;

    /**
     * create a new RicoScreen from a UISnapshot
     * @param uiSnapshot
     * @return
     */
    public static RicoScreen fromSugiliteUISnapshot(SerializableUISnapshot uiSnapshot) {
        RicoScreen ricoScreen = new RicoScreen();
        if (uiSnapshot.getActivityName() != null) {
            ricoScreen.activity_name = uiSnapshot.getActivityName();
        }
        try {
            SugiliteSerializableEntity<Node> rootNodeEntity = findRootNode(uiSnapshot);
            Set<Integer> processedNodes = new HashSet<>();
            ricoScreen.activity = new RicoActivity(RicoNode.fromSugiliteNode(rootNodeEntity, uiSnapshot, processedNodes));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ricoScreen;
    }

    /**
     * find the root node in a UISnapshot
     * @param uiSnapshot
     * @return
     * @throws Exception
     */
    private static SugiliteSerializableEntity<Node> findRootNode(SerializableUISnapshot uiSnapshot) throws Exception {
        Set<SugiliteSerializableTriple> hasParentTriples = uiSnapshot.getPredicateTriplesMap().get(SugiliteRelation.HAS_PARENT.getRelationName());
        Set<String> entityIdWithParents = new HashSet<>();
        List<SugiliteSerializableEntity<Node>> potentialRootNodes = new ArrayList<>();

        for (SugiliteSerializableTriple hasParentTriple : hasParentTriples) {
            entityIdWithParents.add(hasParentTriple.getSubjectId());
        }


        for (SugiliteSerializableEntity entity : uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().values()) {
            if (entity.getEntityValue() instanceof Node) {
                if (! entityIdWithParents.contains("@" + String.valueOf(entity.getEntityId()))) {
                    //potentially root node
                    potentialRootNodes.add(entity);
                }

            }
        }
        if (potentialRootNodes.size() != 1) {
            throw new Exception(String.format("Can't find the root node -- the number of potential root nodes is %d.", potentialRootNodes.size()));
        } else {
            return potentialRootNodes.get(0);
        }
    }
}
