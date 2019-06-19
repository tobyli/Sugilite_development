package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

/**
 * @author toby
 * @date 1/17/18
 * @time 11:37 PM
 */
public class SugiliteTextParentAnnotator implements SugiliteTextAnnotator {
    private List<SugiliteTextAnnotator> subAnnotators;
    private Map<String, List<AnnotatingResult>> cache;
    private static SugiliteTextParentAnnotator instance;

    public static SugiliteTextParentAnnotator getInstance() {
        if (instance != null) {
            return instance;
        } else {
            instance = new SugiliteTextParentAnnotator(true);
            return instance;
        }
    }

    private SugiliteTextParentAnnotator(boolean addAllAvailableAnnotator) {
        subAnnotators = new ArrayList<>();
        cache = new HashMap<>();
        if (addAllAvailableAnnotator) {
            //add all available annotator implementations
            addAnnotator(new EmailAddressAnnotator(),
                    new PhoneNumberAnnotator(),
                    new MoneyAnnotator(),
                    new TimeAnnotator(),
                    new DateAnnotator(),
                    new LengthAnnotator(),
                    new PercentageAnnotator(),
                    new DurationAnnotator(),
                    new TempAnnotator(),
                    new VolumeAnnotator()
                    //, new NumberAnnotator()
                    );
        }
    }

    private void addAnnotator(SugiliteTextAnnotator... entityTagAnnotators) {
        Collections.addAll(subAnnotators, entityTagAnnotators);
    }

    @Override
    public List<AnnotatingResult> annotate(String text) {
        if (cache.containsKey(text)){
            return cache.get(text);
        }
        List<AnnotatingResult> results = new ArrayList<>();
        for (SugiliteTextAnnotator subAnnotator : subAnnotators) {
            try {
                results.addAll(subAnnotator.annotate(text));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        cache.put(text, results);
        return results;
    }

    /**
     * used for returning the results
     */
    public static class AnnotatingResult implements Comparable {
        private SugiliteRelation relation;
        private String matchedString;
        private int startIndex;
        private int endIndex;
        private Double numericValue;

        public AnnotatingResult(SugiliteRelation relation, String matchedString, int startIndex, int endIndex, Double numericValue) {
            this.relation = relation;
            this.matchedString = matchedString;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.numericValue = numericValue;
        }

        public AnnotatingResult(SugiliteRelation relation, String matchedString, int startIndex, int endIndex) {
            this(relation, matchedString, startIndex, endIndex, null);
        }

        public SugiliteRelation getRelation() {
            return relation;
        }

        public String getMatchedString() {
            return matchedString;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        public Double getNumericValue() {
            return numericValue;
        }

        public void setRelation(SugiliteRelation relation) {
            this.relation = relation;
        }

        void setNumericValue(Double val) {
            this.numericValue = val;
        }

        public static SugiliteTextParentAnnotator.AnnotatingResult fromString(String source) {
            SugiliteTextParentAnnotator annotator = new SugiliteTextParentAnnotator(true);
            List<SugiliteTextParentAnnotator.AnnotatingResult> results = annotator.annotate(source);
            if (results.size() > 0) {
                return results.get(0);
            }
            return null;
        }

        @Override
        public int compareTo(@NonNull Object o) {
            if (o instanceof AnnotatingResult) {
                if (this.relation.equals(((AnnotatingResult) o).getRelation())) {
                    return this.numericValue.compareTo(((AnnotatingResult) o).getNumericValue());
                }
            }
            return 0;
        }
    }
}
