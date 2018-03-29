package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import android.graphics.Rect;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.Node;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by shi on 3/22/18.
 */

public class SugiliteNodeAnnotator {

    private static final float ALIGNMENT_THRESHOLD = 0.1f;

    public SugiliteNodeAnnotator() {}

    public static class AnnotatingResult {
        private SugiliteRelation relation;
        private SugiliteEntity<Node> subject;
        private SugiliteEntity<Node> object;

        public AnnotatingResult(SugiliteRelation relation, SugiliteEntity<Node> subject, SugiliteEntity<Node> object) {
            this.relation = relation;
            this.subject = subject;
            this.object = object;
        }

        public SugiliteRelation getRelation() {return relation;}

        public SugiliteEntity<Node> getSubject() {return subject;}

        public Node getObject() {return object.getEntityValue();}
    }

    public List<AnnotatingResult> annotate(Set<SugiliteEntity<Node>> nodes) {
        List<AnnotatingResult> result = new ArrayList<>();
        for (SugiliteEntity<Node> n1 : nodes) {
            double left = 10000;
            double right = 10000;
            double above = 10000;
            double below = 10000;
            AnnotatingResult leftRes = null, rightRes = null, aboveRes = null, belowRes= null;
            for (SugiliteEntity<Node> n2 : nodes) {
                if (n1 == n2) continue;
                Rect r1 = Rect.unflattenFromString(n1.getEntityValue().getBoundsInScreen());
                Rect r2 = Rect.unflattenFromString(n2.getEntityValue().getBoundsInScreen());
                if (r1.contains(r2))
                    result.add(new AnnotatingResult(SugiliteRelation.CONTAINS, n1, n2));
                else if (r2.contains(r1)) continue;
                else {
                    double dist = distance(r1.centerX(), r1.centerY(), r2.centerX(), r2.centerY());
                    if (isRight(r1.centerX(), r1.centerY(), r2.centerX(), r2.centerY()))
                        if (dist < right) {
                            right = dist;
                            rightRes = new AnnotatingResult(SugiliteRelation.RIGHT, n1, n2);
                        }
                    if (isRight(r2.centerX(), r2.centerY(), r1.centerX(), r1.centerY()))
                        if (dist < left) {
                            left = dist;
                            leftRes = new AnnotatingResult(SugiliteRelation.LEFT, n1, n2);
                        }
                    if (isAbove(r1.centerX(), r1.centerY(), r2.centerX(), r2.centerY()))
                        if (dist < above) {
                            above = dist;
                            aboveRes = new AnnotatingResult(SugiliteRelation.ABOVE, n1, n2);
                        }
                    if (isAbove(r2.centerX(), r2.centerY(), r1.centerX(), r1.centerY()))
                        if (dist < below) {
                            below = dist;
                            belowRes = new AnnotatingResult(SugiliteRelation.BELOW, n1, n2);
                        }
                }
            }
            if (leftRes != null) result.add(leftRes);
            if (rightRes != null) result.add(rightRes);
            if (aboveRes != null) result.add(aboveRes);
            if (belowRes != null) result.add(belowRes);
        }
        return result;
    }

    private boolean isRight(int x1, int y1, int x2, int y2) {
        if (x1 <= x2) return false;
        float slope = (float)(y2 - y1)/(float)(x2 - x1);
        return Math.abs(slope) <= ALIGNMENT_THRESHOLD;
    }

    private boolean isAbove(int x1, int y1, int x2, int y2) {
        if (y2 <= y1) return false;
        float slope = (float)(x2 - x1)/(float)(y2 - y1);
        return Math.abs(slope) <= ALIGNMENT_THRESHOLD;
    }

    private double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
    }

}
