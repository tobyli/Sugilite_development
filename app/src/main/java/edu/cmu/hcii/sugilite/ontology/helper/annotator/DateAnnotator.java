package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import android.util.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

/**
 * Given the input as a string containing dates of the specified formats, parse it and store the
 * dates as the number of milliseconds passed from 1970-01-01 until 12 am on that date.
 *
 * Created by shi on 2/8/18.
 */

public class DateAnnotator extends SugiliteTextAnnotator {
    public DateAnnotator() { super(); }

    @Override
    public List<AnnotatingResult> annotate(String text) {
        List<AnnotatingResult> results = new ArrayList<>();
        String mmddyyyy = "\\b(0?[1-9]|1[0-2])[/-](0?[1-9]|[1-2][0-9]|3[0-1])[/-][0-9]{4}\\b";
        String complex = "\\b(Jan(uary)?|Feb(ruary)?|Mar(ch)?|Apr(il)?|May|Jun(e)?|Jul(y)?|Aug(ust)?" +
                "|Sep(tember)?|Oct(ober)?|Nov(ember)?|Dec(ember)?)[. ](0?[1-9]|[1-2][0-9]|3[0-1])(,)? ([0-9]{4})\\b";
        String yyyymmdd = "\\b[0-9]{4}[-.](0?[1-9]|1[0-2])[-.](0?[1-9]|[1-2][0-9]|3[0-1])\\b";
        Pattern pattern1 = Pattern.compile(mmddyyyy);
        Pattern pattern2 = Pattern.compile(complex);
        Pattern pattern3 = Pattern.compile(yyyymmdd);

        Matcher matcher = pattern1.matcher(text);
        while (matcher.find()) {
            String matchedString = text.substring(matcher.start(), matcher.end());
            String[] parsed = matchedString.split("[/-]");
            int month = Integer.valueOf(parsed[0]);
            int date = Integer.valueOf(parsed[1]);
            int year = Integer.valueOf(parsed[2]);
            if (year < 1970) continue;

            Calendar cal = new GregorianCalendar();
            cal.set(year, month-1, date, 0, 0, 0);
            double value = (double)(cal.getTime().getTime());
            AnnotatingResult res = new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()),
                    matcher.start(), matcher.end(), value);
            results.add(res);
        }

        matcher = pattern2.matcher(text);
        while (matcher.find()) {
            String matchedString = text.substring(matcher.start(), matcher.end());
            String[] parsed = matchedString.split("[, .]");
            String m = parsed[0].substring(0, 3);
            String d = parsed[1];
            if (d.length() < 2) d = "0" + d;
            String y = parsed[parsed.length-1];
            String comb = m + " " + d + " " + y;
            try {
                Date date = new SimpleDateFormat("MMM dd yyyy").parse(comb);
                double value = (double)(date.getTime());
                AnnotatingResult res = new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()),
                        matcher.start(), matcher.end(), value);
                results.add(res);
            } catch (java.text.ParseException e) { }
        }

        matcher = pattern3.matcher(text);
        while (matcher.find()) {
            String matchedString = text.substring(matcher.start(), matcher.end());
            String[] parsed = matchedString.split("[-.]");
            int year = Integer.valueOf(parsed[0]);
            int month = Integer.valueOf(parsed[1]);
            int date = Integer.valueOf(parsed[2]);
            if (year < 1970) continue;

            Calendar cal = new GregorianCalendar();
            cal.set(year, month-1, date, 0, 0, 0);
            double value = (double)(cal.getTime().getTime());
            AnnotatingResult res = new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()),
                    matcher.start(), matcher.end(), value);
            results.add(res);
        }

        return results;
    }

    private static final SugiliteRelation RELATION = SugiliteRelation.CONTAINS_DATE;

}
