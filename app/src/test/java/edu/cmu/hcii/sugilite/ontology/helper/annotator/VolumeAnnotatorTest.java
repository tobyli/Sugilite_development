package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

import static org.junit.Assert.*;

/**
 * Created by shi on 3/6/18.
 */
public class VolumeAnnotatorTest {
    SugiliteTextAnnotator annotator;

    static final double MILLILITER = 1.0;
    static final double LITER = 1000.0;
    static final double OUNCE = 29.547;
    static final double TABLESPOON = 4.929;
    static final double CUP = 236.588;
    static final double PINT = 473.176;
    static final double QUART = 946.353;
    static final double GALLON = 3785.412;

    @Before
    public void setUp() {
        annotator = new VolumeAnnotator();
    }

    @Test
    public void testMultiple() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("Small: 100 mL, Medium: 4 fl oz, " +
                "Large: 5.55 ounce, ExLarge: 1 cp 3 oz, SuperLarge: 1.2 pt, Enormous: 3 gal 4 qt");
        assertEquals(res.size(), 6);
        assertEquals(res.get(0).getRelation(), SugiliteRelation.CONTAINS_VOLUME);
        res.sort(Comparator.comparingDouble(SugiliteTextParentAnnotator.AnnotatingResult::getNumericValue));
        assertEquals(res.get(0).getNumericValue().intValue(), (int)(100*MILLILITER));
        assertEquals(res.get(1).getNumericValue().intValue(), (int)(4*OUNCE));
        assertEquals(res.get(2).getNumericValue().intValue(), (int)(5.55*OUNCE));
        assertEquals(res.get(3).getNumericValue().intValue(), (int)(CUP+3*OUNCE));
        assertEquals(res.get(4).getNumericValue().intValue(), (int)(1.2*PINT));
        assertEquals(res.get(5).getNumericValue().intValue(), (int)(3*GALLON+4*QUART));
    }

}