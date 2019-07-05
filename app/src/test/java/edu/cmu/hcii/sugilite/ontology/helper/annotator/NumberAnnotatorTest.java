package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

import static org.junit.Assert.*;

/**
 * Created by shi on 4/5/18.
 */
public class NumberAnnotatorTest {

    SugiliteTextAnnotator annotator;

    @Before
    public void setup() {
        annotator = new NumberAnnotator();
    }

    @Test
    public void testBasic() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("34.56");
        assertEquals(res.size(), 1);
        assertEquals(res.get(0).getRelation(), SugiliteRelation.CONTAINS_NUMBER);
        assertEquals(Double.compare(res.get(0).getNumericValue(), 34.56), 0);
    }

    @Test
    public void testBasic1() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("12,345.67");
        assertEquals(res.size(), 1);
        assertEquals(res.get(0).getRelation(), SugiliteRelation.CONTAINS_NUMBER);
        assertEquals(Double.compare(res.get(0).getNumericValue(), 12345.67), 0);
    }

    @Test
    public void testMultiple() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("2, 3.05, 23,333,444.08");
        assertEquals(res.size(), 3);
        assertEquals(Double.compare(res.get(0).getNumericValue(), 2), 0);
        assertEquals(Double.compare(res.get(1).getNumericValue(), 3.05), 0);
        assertEquals(Double.compare(res.get(2).getNumericValue(), 23333444.08), 0);
    }
}