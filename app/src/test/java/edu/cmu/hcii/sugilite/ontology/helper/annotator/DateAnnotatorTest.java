package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

import static org.junit.Assert.*;

/**
 * Created by shi on 2/8/18.
 */
public class DateAnnotatorTest {
    SugiliteTextParentAnnotator annotator;

    @Before
    public void setUp() {
        annotator = new DateAnnotator();
    }

    @Test
    public void testBasic() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("02-08-2018");
        assertEquals(res.size(), 1);
        assertEquals(res.get(0).getRelation(), SugiliteRelation.CONTAINS_DATE);
        Calendar cal = new GregorianCalendar();
        cal.set(2018, 1, 8, 0, 0, 0);
        assertEquals(res.get(0).getNumericValue().longValue()/1000, cal.getTime().getTime()/1000);
    }

    @Test
    public void testBasic1() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("2/8/2018");
        assertEquals(res.size(), 1);
        Calendar cal = new GregorianCalendar();
        cal.set(2018, 1, 8, 0, 0, 0);
        assertEquals(res.get(0).getNumericValue().longValue()/1000, cal.getTime().getTime()/1000);
    }

    @Test
    public void testBasic2() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("Feb 8, 2018");
        assertEquals(res.size(), 1);
        Calendar cal = new GregorianCalendar();
        cal.set(2018, 1, 8, 0, 0, 0);
        assertEquals(res.get(0).getNumericValue().longValue()/1000, cal.getTime().getTime()/1000);
    }

    @Test
    public void testBasic3() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("2018.2.08");
        assertEquals(res.size(), 1);
        Calendar cal = new GregorianCalendar();
        cal.set(2018, 1, 8, 0, 0, 0);
        assertEquals(res.get(0).getNumericValue().longValue()/1000, cal.getTime().getTime()/1000);
    }

    @Test
    public void testBadInput() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("3/32/2018");
        assertEquals(res.size(), 0);
    }

    @Test
    public void testBadInput1() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("8 Feb 2018");
        assertEquals(res.size(), 0);
    }

    @Test
    public void testGroup() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate
                ("Start date: Feb 8, 2018  End date: 03/31/2018  Result will be announced on 2018.5.1");
        assertEquals(res.size(), 3);
        res.sort((a, b) -> Double.compare(a.getNumericValue(), b.getNumericValue()));
        Calendar cal = new GregorianCalendar();
        cal.set(2018, 1, 8, 0, 0, 0);
        assertEquals(res.get(0).getNumericValue().longValue()/1000, cal.getTime().getTime()/1000);
        cal.set(2018, 2, 31, 0, 0, 0);
        assertEquals(res.get(1).getNumericValue().longValue()/1000, cal.getTime().getTime()/1000);
        cal.set(2018, 4, 1, 0, 0, 0);
        assertEquals(res.get(2).getNumericValue().longValue()/1000, cal.getTime().getTime()/1000);
    }

    @Test
    public void testGroupWithBadInput() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate
                ("Today is February 15 2018  Tomorrow is 2/16/2018. I'll blow up the world on Feb 32, 2018 " +
                        "and fly back to mars on 13/31/2018");
        assertEquals(res.size(), 2);
        res.sort((a, b) -> Double.compare(a.getNumericValue(), b.getNumericValue()));
        Calendar cal = new GregorianCalendar();
        cal.set(2018, 1, 15, 0, 0, 0);
        assertEquals(res.get(0).getNumericValue().longValue()/1000, cal.getTime().getTime()/1000);
        cal.set(2018, 1, 16, 0, 0, 0);
        assertEquals(res.get(1).getNumericValue().longValue()/1000, cal.getTime().getTime()/1000);
    }

    @Test
    public void testLatestUpdate() {
        List<SugiliteTextParentAnnotator.AnnotatingResult> res = annotator.annotate("Mon March 3, 3/4/2018, 03/05," +
                " Wed 3/7, Thursday Mar 8, 2017, Friday 03/09/2018");
        assertEquals(res.size(), 5);
        res.sort(Comparator.comparingDouble(SugiliteTextParentAnnotator.AnnotatingResult::getNumericValue));
        Calendar cal = new GregorianCalendar();
        cal.set(2017, 2, 8, 0, 0, 0);
        assertEquals(res.get(0).getNumericValue().longValue()/1000, cal.getTime().getTime()/1000);
        cal.set(2018, 2, 3, 0, 0, 0);
        assertEquals(res.get(1).getNumericValue().longValue()/1000, cal.getTime().getTime()/1000);
        cal.set(2018, 2, 4, 0, 0, 0);
        assertEquals(res.get(2).getNumericValue().longValue()/1000, cal.getTime().getTime()/1000);
        cal.set(2018, 2, 7, 0, 0, 0);
        assertEquals(res.get(3).getNumericValue().longValue()/1000, cal.getTime().getTime()/1000);
        cal.set(2018, 2, 9, 0, 0, 0);
        assertEquals(res.get(4).getNumericValue().longValue()/1000, cal.getTime().getTime()/1000);
    }

}