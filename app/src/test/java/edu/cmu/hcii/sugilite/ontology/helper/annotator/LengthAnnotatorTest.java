package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

import static org.junit.Assert.*;

/**
 * Created by shi on 2/15/18.
 */
public class LengthAnnotatorTest {

    SugiliteTextAnnotator annotator;

    @Before
    public void setup() {
        annotator = new LengthAnnotator();
    }

    private static final int MILE = 1609344;
    private static final int KILOMETER = 1000000;
    private static final int METER = 1000;
    private static final double YARD = 914.4;
    private static final double FEET = 304.8;
    private static final double INCH = 25.4;
    private static final int CENTIMETER = 10;

    @Test
    public void testBasic() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("Distance to school: 5 miles");
        assertEquals(res.size(), 1);
        assertEquals(res.get(0).getRelation(), SugiliteRelation.CONTAINS_LENGTH);
        assertEquals(res.get(0).getNumericValue().intValue(), 5 * MILE);
    }

    @Test
    public void testBasic1() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("My height is 5 ft 11.5 inch");
        assertEquals(res.size(), 1);
        assertEquals(res.get(0).getNumericValue().intValue(), (int)(5 * FEET + 11.5 * INCH));
    }

    @Test
    public void testBasic2() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("Today I ran 5 km 432.10 m");
        assertEquals(res.size(), 1);
        assertEquals(res.get(0).getNumericValue().intValue(), (int)(5.4321 * KILOMETER));
    }

    @Test
    public void testBasic3() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("I want a 12 inch pizza");
        assertEquals(res.size(), 1);
        assertEquals(res.get(0).getNumericValue().intValue(), (int)(12 * INCH));
    }

    @Test
    public void testBadInput() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("6.a km, 3m, 1.2 in");
        //assertEquals(res.size(), 0);
    }

    @Test
    public void testGroup() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("To downtown: 5.5 mi, " +
                "To CMU: 6 km 666 m, To hell: 1 ft 2.34 inch");
        assertEquals(res.size(), 3);
        res.sort((a, b) -> Double.compare(a.getNumericValue(), b.getNumericValue()));
        assertEquals(res.get(0).getNumericValue().intValue(), (int)(FEET + 2.34 * INCH));
        assertEquals(res.get(1).getNumericValue().intValue(), 6 * KILOMETER + 666 * METER);
        assertEquals(res.get(2).getNumericValue().intValue(), (int)(5.5 * MILE));
    }
}