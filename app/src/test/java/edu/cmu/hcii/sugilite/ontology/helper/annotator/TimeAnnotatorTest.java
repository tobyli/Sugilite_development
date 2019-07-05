package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

import static org.junit.Assert.*;

/**
 * Created by shi on 2/1/18.
 */
public class TimeAnnotatorTest {

    SugiliteTextParentAnnotator annotator = null;

    @Before
    public void setUp() {
        annotator = new TimeAnnotator();
    }

    @Test
    public void testBasicInput1() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> results =
                annotator.annotate("Current time: 11:59 pm");
        assertTrue(results.size() == 1);
        assertEquals(results.get(0).getNumericValue().intValue(), (23*3600)+(59*60));//(23*60+59)*1000);
        assertTrue(results.get(0).getRelation().equals(SugiliteRelation.CONTAINS_TIME));
    }

    @Test
    public void testBasicInput2() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> results =
                annotator.annotate("Current time: 13:59");
        assertTrue(results.size() == 1);
        assertEquals(results.get(0).getNumericValue().intValue(), (13*3600)+(59*60));//(13*60+59)*1000);
        assertTrue(results.get(0).getRelation().equals(SugiliteRelation.CONTAINS_TIME));
    }

    @Test
    public void testBasicInput3() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> results =
                annotator.annotate("Current time: 3 PM");
        assertTrue(results.size() == 1);
        assertEquals(results.get(0).getNumericValue().intValue(), 15*3600);//(15*60+0)*1000);
        assertTrue(results.get(0).getRelation().equals(SugiliteRelation.CONTAINS_TIME));
    }

    @Test
    public void testBadInput1() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> results =
                annotator.annotate("Bad time: 0:60 pm");
        assertTrue(results.size() == 0);
    }

    @Test
    public void testBadInput2() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> results =
                annotator.annotate("Bad time: 24:00");
        assertTrue(results.size() == 0);
    }

    @Test
    public void testMultipleInputs1() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> results =
                annotator.annotate("Current bus: 12:00 pm, next bus: 13:59, next next bus: 4PM");
        assertTrue(results.size() == 3);
        results.sort((a, b) -> Double.compare(a.getNumericValue(), b.getNumericValue()));
        assertEquals(results.get(0).getNumericValue().intValue(), (12*3600));//(12*60+0)*1000);
        assertEquals(results.get(1).getNumericValue().intValue(), (13*3600)+(59*60));//(13*60+59)*1000);
        assertEquals(results.get(2).getNumericValue().intValue(), (16*3600));//(16*60+0)*1000);
    }

    @Test
    public void testMultipleInputs2() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> results =
                annotator.annotate("Flight takes off at 4PM and boarding stops at 3:45pm." +
                        "The flight takes 32:15 and arrives at 00:00");
        assertTrue(results.size() == 3);
        results.sort((a, b) -> Double.compare(a.getNumericValue(), b.getNumericValue()));
        assertEquals(results.get(0).getNumericValue().intValue(), (0));//(0*60+0)*1000);
        assertEquals(results.get(1).getNumericValue().intValue(), (15*3600)+(45*60));//(15*60+45)*1000);
        assertEquals(results.get(2).getNumericValue().intValue(), (16*3600));//(16*60+0)*1000);
    }
}