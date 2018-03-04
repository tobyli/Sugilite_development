package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

import static org.junit.Assert.*;

/**
 * Created by shi on 3/1/18.
 */
public class DurationAnnotatorTest {
    SugiliteTextAnnotator annotator;

    @Before
    public void setup() {
        annotator = new DurationAnnotator();
    }

    @Test
    public void testBasic() {
        List<SugiliteTextAnnotator.AnnotatingResult> res = annotator.annotate("5 hrs");
        assertEquals(res.size(), 1);
        assertEquals(res.get(0).getRelation(), SugiliteRelation.CONTAINS_DURATION);
        assertEquals(res.get(0).getNumericValue().intValue(), 5*3600000);
    }

    @Test
    public void testBasic1() {
        List<SugiliteTextAnnotator.AnnotatingResult> res = annotator.annotate("4 d 33.5 minutes 5 s");
        assertEquals(res.size(), 1);
        assertEquals(res.get(0).getRelation(), SugiliteRelation.CONTAINS_DURATION);
        assertEquals(res.get(0).getNumericValue().intValue(), (int)(4*86400000+33.5*60000+5*1000));
    }

    @Test
    public void testBasic2() {
        List<SugiliteTextAnnotator.AnnotatingResult> res = annotator.annotate("flight time: 17 hr 16.5 mins, " +
                "flight departs at 19:05");
        assertEquals(res.size(), 1);
        assertEquals(res.get(0).getRelation(), SugiliteRelation.CONTAINS_DURATION);
        assertEquals(res.get(0).getNumericValue().intValue(), (int)(17*3600000+16.5*60000));
    }

    @Test
    public void testMultiple() {
        List<SugiliteTextAnnotator.AnnotatingResult> res = annotator.annotate("Flights from Pittsburgh to" +
                "Havana: Delta: 9 hr 36 min, American: 10 hours, United: 9 h 40 sec");
        assertEquals(res.size(), 3);
        res.sort(Comparator.comparingDouble(SugiliteTextAnnotator.AnnotatingResult::getNumericValue));
        assertEquals(res.get(0).getNumericValue().intValue(), 9*3600000+40*1000);
        assertEquals(res.get(1).getNumericValue().intValue(), 9*3600000+36*60000);
        assertEquals(res.get(2).getNumericValue().intValue(), 10*3600000);
    }

    @Test
    public void testMultipleWithBadInput() {
        List<SugiliteTextAnnotator.AnnotatingResult> res = annotator.annotate("Durations: 5:30, 5 hr 29 min, " +
                "5:28:00");
        assertEquals(res.size(), 1);
        assertEquals(res.get(0).getNumericValue().intValue(), 5*3600000+29*60000);
    }

}