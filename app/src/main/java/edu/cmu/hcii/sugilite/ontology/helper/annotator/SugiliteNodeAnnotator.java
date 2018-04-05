package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import android.graphics.Rect;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.Node;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;

import java.util.ArrayList;
import java.util.Set;
import java.util.List;

/**
 * Defined 6 possible spatial relations (ABOVE, BELOW, RIGHT, LEFT, NEXT_TO, NEAR) for all nodes on
 * the UI Snapshot. In particular, two nodes are NEXT_TO each other if they are either LEFT or RIGHT
 * to each other. Two nodes are NEAR each other if their bounding boxes are close to each other but
 * do not overlap.
 *
 * Created by shi on 3/22/18.
 */

public class SugiliteNodeAnnotator {

    private static final float ALIGNMENT_THRESHOLD = 0.1f;
    private static final float SEPARATE_THRESHOLD = 1000f;
    private static final float NEAR_THRESHOLD = 100f;
    private static final int XMAX = 1080;
    private static final int YMAX = 1920;

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
            if (!onScreen(n1)) continue;
            boolean n1Clickable = n1.getEntityValue().getClickable();
            for (SugiliteEntity<Node> n2 : nodes) {
                if (n1 == n2) continue;
                if (!(n1Clickable || n2.getEntityValue().getClickable())) continue;
                if (!onScreen(n2)) continue;
                Rect r1 = Rect.unflattenFromString(n1.getEntityValue().getBoundsInScreen());
                Rect r2 = Rect.unflattenFromString(n2.getEntityValue().getBoundsInScreen());
                if (r1.intersect(r2)) continue;
                if (r1.contains(r2))
                    result.add(new AnnotatingResult(SugiliteRelation.CONTAINS, n1, n2));
                else if (r2.contains(r1)) continue;
                else {
                    double dist = distance(r1.centerX(), r1.centerY(), r2.centerX(), r2.centerY());
                    if (dist > SEPARATE_THRESHOLD) continue;
                    if (separation(r1, r2) <= NEAR_THRESHOLD && separation(r1, r2) >= 0)
                        result.add(new AnnotatingResult(SugiliteRelation.NEAR, n1, n2));
                    if (isRight(r1.centerX(), r1.centerY(), r2.centerX(), r2.centerY())) {
                        result.add(new AnnotatingResult(SugiliteRelation.RIGHT, n1, n2));
                        result.add(new AnnotatingResult(SugiliteRelation.NEXT_TO, n1, n2));
                    }
                    if (isRight(r2.centerX(), r2.centerY(), r1.centerX(), r1.centerY())) {
                        result.add(new AnnotatingResult(SugiliteRelation.LEFT, n1, n2));
                        result.add(new AnnotatingResult(SugiliteRelation.NEXT_TO, n1, n2));
                    }
                    if (isAbove(r1.centerX(), r1.centerY(), r2.centerX(), r2.centerY()))
                        result.add(new AnnotatingResult(SugiliteRelation.ABOVE, n1, n2));
                    if (isAbove(r2.centerX(), r2.centerY(), r1.centerX(), r1.centerY()))
                        result.add(new AnnotatingResult(SugiliteRelation.BELOW, n1, n2));
                }
            }
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

    private int separation(Rect r1, Rect r2) {
        int dx, dy;
        if (r1.left >= r2.right) dx = r1.left - r2.right;
        else if (r2.left >= r1.right) dx = r2.left - r1.right;
        else dx = -1;
        if (r1.top >= r2.bottom) dy = r1.top - r2.bottom;
        else if (r2.top >= r1.bottom) dy = r2.top - r1.bottom;
        else dy = -1;
        return Math.max(dx, dy);
    }

    private boolean onScreen(SugiliteEntity<Node> node) {
        String pos = node.getEntityValue().getBoundsInScreen();
        String[] splitRes = pos.split(" ");
        int left = Integer.valueOf(splitRes[0]);
        int top = Integer.valueOf(splitRes[1]);
        int right = Integer.valueOf(splitRes[2]);
        int bottom = Integer.valueOf(splitRes[3]);
        if (right < left || bottom < top) return false;
        return right <= XMAX && bottom <= YMAX;
    }

}
