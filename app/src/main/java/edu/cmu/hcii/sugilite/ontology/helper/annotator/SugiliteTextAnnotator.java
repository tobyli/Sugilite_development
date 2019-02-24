package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import java.util.List;

/**
 * @author toby
 * @date 2/24/19
 * @time 3:04 PM
 */
public interface SugiliteTextAnnotator {
    List<SugiliteTextParentAnnotator.AnnotatingResult> annotate(String text);
}
