package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

import static org.junit.Assert.*;

/**
 * Created by shi on 2/22/18.
 */
public class PercentageAnnotatorTest {
    SugiliteTextAnnotator annotator;

    @Before
    public void setup() {
        annotator = new PercentageAnnotator();
    }

    @Test
    public void testMultiple() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("35 percent of the class" +
                "get an A while 25.95% get a B");
        assertEquals(res.size(), 2);
        assertEquals(res.get(0).getRelation(), SugiliteRelation.CONTAINS_PERCENTAGE);
        assertEquals(res.get(1).getRelation(), SugiliteRelation.CONTAINS_PERCENTAGE);
        res.sort(Comparator.comparingDouble(SugiliteTextParentAnnotator.AnnotatingResult::getNumericValue));
        assertEquals(res.get(0).getNumericValue().intValue(), 25);
        assertEquals(res.get(1).getNumericValue().intValue(), 35);
    }

    @Test
    public void testMultipleWithBadInput() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("stock market growth rate:" +
                "-550.1 %, 4, 3.% and 0.0006 percent");
        assertEquals(res.size(), 2);
        res.sort(Comparator.comparingDouble(SugiliteTextParentAnnotator.AnnotatingResult::getNumericValue));
        assertEquals(res.get(0).getNumericValue().intValue(), -550);
        assertEquals(res.get(1).getNumericValue().intValue(), 0);
    }
}