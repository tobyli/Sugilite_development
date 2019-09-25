package edu.cmu.hcii.sugilite.sharing.model;

import android.util.Log;
import android.widget.EditText;
import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableTriple;
import edu.cmu.hcii.sugilite.sharing.HashedSplitStringGenerator;
import edu.cmu.hcii.sugilite.sharing.debug.HasPlaintext;

import java.util.*;

/**
 * A hashed representation of the text present in a UI snapshot.
 */
public class HashedUIStrings {

    public final String packageName;
    public final String activityName;
    public final HashedString packageUserHash;
    public final List<HashedSplitString> hashedTexts;

    // note: storing activityname and packagename may be redundant
    public HashedUIStrings(String packageName, String activityName, SerializableUISnapshot snapshot, String androidID, HashedSplitStringGenerator generator) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.packageUserHash = new HashedString(packageName + androidID);
        this.hashedTexts = new ArrayList<>();

        Set<String> blacklistPackageSubjects = new HashSet<>();
        for (SugiliteSerializableTriple triple : snapshot.getTriples()) {
            if (triple.getPredicate().equals(SugiliteRelation.HAS_PACKAGE_NAME)) {
                if (Arrays.stream(Const.UI_UPLOAD_PACKAGE_BLACKLIST).anyMatch(triple.getObjectStringValue()::equals)) {
                    blacklistPackageSubjects.add(triple.getSubjectId());
                }
            }
        }

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
                        Log.i("HashedUIStrings", className);
                    }
                } catch (ClassNotFoundException e) {
                    Log.i("HashedUIStrings", className);
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
                    Log.v("HashedUIStrings", "Not adding EditText text : " + triple.getObjectStringValue());
                } else if (blacklistPackageSubjects.contains(triple.getSubjectId())) {
                    Log.v("HashedUIStrings", "Not adding blacklisted package text : " + triple.getObjectStringValue());
                } else {
                    childStrings.add(triple.getObjectStringValue());
                }
            }
        }

        for (String s : childStrings) {
            hashedTexts.add(HashedSplitStringGenerator.generate(s));
        }
    }
    private static String hashedTextToJson(HashedSplitString hashedText) {
        StringBuilder sb = new StringBuilder("{\n");

        sb.append(jsonProperty("text_hash", hashedText.preferred.toString()));
        sb.append(",\n");

        if (hashedText instanceof HasPlaintext) {
            sb.append(jsonProperty("debug_text", ((HasPlaintext)hashedText).getPlaintext()));
            sb.append(",\n");
        }

        sb.append("\"derived_hashes\": [\n");

        for (int i = 0; i < hashedText.alternatives.size(); i++) {
            HashedSubString subString = hashedText.alternatives.get(i);

            if (i != 0) sb.append(",\n");
            sb.append("{\n");

            sb.append(jsonProperty("text_hash", subString.toString()));
            sb.append(",\n");

            if (subString instanceof HasPlaintext) {
                sb.append(jsonProperty("debug_text", ((HasPlaintext)subString).getPlaintext()));
                sb.append(",\n");
            }

            sb.append(jsonProperty("tokens_removed", subString.priority));

            sb.append("}");
        }

        sb.append("\n]}");
        return sb.toString();
    }
    private static String jsonProperty(String key, String value) {
        return "\"" + key + "\": \"" + value + "\"";
    }
    private static String jsonProperty(String key, int value) {
        return "\"" + key + "\": " + value;
    }

    public String toJson(){
        StringBuilder sb = new StringBuilder("{\n");

        sb.append(jsonProperty("package", this.packageName));
        sb.append(",\n");

        sb.append(jsonProperty("activity", this.activityName));
        sb.append(",\n");

        sb.append(jsonProperty("package_user_hash", this.packageUserHash.toString()));
        sb.append(",\n");

        sb.append("\"text_hashes\":\n[\n");

        for (int i = 0; i < this.hashedTexts.size(); i++) {
            if (i != 0) sb.append(",\n");
            sb.append(hashedTextToJson(this.hashedTexts.get(i)));
        }

        sb.append("\n]\n}");
        return sb.toString();
    }
}
