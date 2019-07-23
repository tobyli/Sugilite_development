package edu.cmu.hcii.sugilite.sharing;

import android.util.Log;
import android.widget.EditText;
import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableTriple;

import java.util.*;

/**
 * A hashed representation of the text present in a UI snapshot.
 */
public class HashedUI {

    public final String packageName;
    public final String activityName;
    public final HashedString packageUserHash;
    public final List<HashedSplitString> hashedTexts;

    // note: storing activityname and packagename may be redundant
    public HashedUI(String packageName, String activityName, SerializableUISnapshot snapshot, String androidID, HashedSplitStringGenerator generator) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.packageUserHash = new HashedString(packageName + androidID);
        this.hashedTexts = new ArrayList<>();

        // subjects that are focused edittexts (which we don't want to upload hashes of)
        Set<String> editTextSubjects = new HashSet<>();

        // find edittexts
        if (snapshot.getPredicateTriplesMap().keySet().containsAll(Arrays.asList(
                SugiliteRelation.IS_EDITABLE.getRelationName(),
                SugiliteRelation.IS_FOCUSED.getRelationName(),
                SugiliteRelation.HAS_CLASS_NAME.getRelationName()))) {

            Set<String> editableSubjects = new HashSet<>();
            for (SugiliteSerializableTriple triple : snapshot.getPredicateTriplesMap().get(SugiliteRelation.IS_EDITABLE.getRelationName())) {
                if (triple.getObjectStringValue().equals("true")) editableSubjects.add(triple.getSubjectId());
            }
            Set<String> editableClassNames = new HashSet<>();
            for (SugiliteSerializableTriple triple : snapshot.getPredicateTriplesMap().get(SugiliteRelation.HAS_CLASS_NAME.getRelationName())) {
                if (editableSubjects.contains(triple.getSubjectId())) {
                    editableClassNames.add(triple.getObjectStringValue());
                }
            }
            Set<String> textBoxSubclassNames = new HashSet<>();
            for (String className : editableClassNames) {
                try {
                    if (EditText.class.isAssignableFrom(Class.forName(className))) {
                        textBoxSubclassNames.add(className);
                        Log.i("EditTextSubclass", className);
                    }
                } catch (ClassNotFoundException e) {
                    Log.i("EditTextSubclassClassNotFound", className);
                }
            }
            for (SugiliteSerializableTriple triple : snapshot.getPredicateTriplesMap().get(SugiliteRelation.HAS_CLASS_NAME.getRelationName())) {
                if (textBoxSubclassNames.contains(triple.getObjectStringValue())) {
                    editTextSubjects.add(triple.getSubjectId());
                }
            }
            Set<String> focusedSubjects = new HashSet<>();
            for (SugiliteSerializableTriple triple : snapshot.getPredicateTriplesMap().get(SugiliteRelation.IS_FOCUSED.getRelationName())) {
                if (triple.getObjectStringValue().equals("true")) {
                    focusedSubjects.add(triple.getSubjectId());
                }
            }
            editTextSubjects.retainAll(focusedSubjects);
        }

        // TODO remove contents of focused edit texts from feedback labels? (e.g. "asdf" and "no results found for asdf")

        // use set to ensure we don't save duplicate strings
        Set<String> childStrings = new HashSet<String>();

        for (SugiliteSerializableTriple triple : snapshot.getTriples()) {
            // if we can hash this kind of relation
            if (Arrays.stream(Const.POTENTIALLY_PRIVATE_RELATIONS).anyMatch(triple.getPredicate()::equals)) {
                if (editTextSubjects.contains(triple.getSubjectId())) {
                    Log.v("EditTextSkip", "Not adding EditText text : " + triple.getObjectStringValue());
                } else {
                    childStrings.add(triple.getObjectStringValue());
                }
            }
        }

        for (String s : childStrings) {
            hashedTexts.add(HashedSplitStringGenerator.generate(s));
        }
    }
}
