package edu.cmu.hcii.sugilite.sharing;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlockMetaInfo;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteBinaryOperation;
import edu.cmu.hcii.sugilite.model.operation.trinary.SugiliteTrinaryOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteUnaryOperation;
import edu.cmu.hcii.sugilite.ontology.HashedStringOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.LeafOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.OntologyQueryWithSubQueries;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.StringAlternativeOntologyQuery;
import edu.cmu.hcii.sugilite.recording.newrecording.SugiliteBlockBuildingHelper;
import edu.cmu.hcii.sugilite.sharing.model.HashedString;
import edu.cmu.hcii.sugilite.sharing.model.StringInContext;
import edu.cmu.hcii.sugilite.sharing.model.StringInContextWithIndexAndPriority;

public class SugiliteSharingScriptPreparer {

    //private SugiliteBlockBuildingHelper helper;
    private SugiliteScriptSharingHTTPQueryManager sugiliteScriptSharingHTTPQueryManager;


    public SugiliteSharingScriptPreparer(Context context) {
        //this.helper = helper;
        this.sugiliteScriptSharingHTTPQueryManager = SugiliteScriptSharingHTTPQueryManager.getInstance(context);
    }

    private static Set<StringInContext> getStringsFromOntologyQuery(OntologyQuery query, int startIndex, String packageName, String activityName) {
        Set<StringInContext> strings = new HashSet<StringInContext>();

        if (query instanceof LeafOntologyQuery) {
            LeafOntologyQuery loq = (LeafOntologyQuery) query;
            if (Arrays.stream(Const.POTENTIALLY_PRIVATE_RELATIONS).anyMatch(loq.getR()::equals)) {
                strings.add(new StringInContextWithIndexAndPriority(activityName, packageName, loq.getObjectAsString(), 0, startIndex++));
            }
        }

        if (query instanceof OntologyQueryWithSubQueries) {
            for (OntologyQuery oq : ((OntologyQueryWithSubQueries) query).getSubQueries()) {
                Set<StringInContext> sub = getStringsFromOntologyQuery(oq, startIndex, packageName, activityName);
                startIndex -= strings.size();
                strings.addAll(sub);
                startIndex += strings.size();
            }
        }

        return strings;
    }

    private static Set<StringInContext> getStringsFromBlock(SugiliteBlock block, int startIndex) {
        Set<StringInContext> strings = new HashSet<StringInContext>();
        if (block instanceof SugiliteOperationBlock) {
            SugiliteOperationBlock operationBlock = (SugiliteOperationBlock)block;
            SugiliteOperation op = ((SugiliteOperationBlock) block).getOperation();
            SerializableUISnapshot snapshot = operationBlock.getFeaturePack().serializableUISnapshot;
            String packageName = snapshot.getPackageName();
            String activityName = snapshot.getActivityName();
            if (op instanceof SugiliteUnaryOperation) {
                SugiliteUnaryOperation unary = (SugiliteUnaryOperation)op;
                if (unary.getParameter0() instanceof OntologyQuery) {
                    Set<StringInContext> oqStrings = getStringsFromOntologyQuery((OntologyQuery) unary.getParameter0(), startIndex, packageName, activityName);
                    startIndex -= strings.size();
                    strings.addAll(oqStrings);
                    startIndex += strings.size();
                }
            }
            if (op instanceof SugiliteBinaryOperation) {
                SugiliteBinaryOperation binary = (SugiliteBinaryOperation)op;
                if (binary.getParameter0() instanceof OntologyQuery) {
                    Set<StringInContext> oqStrings = getStringsFromOntologyQuery((OntologyQuery) binary.getParameter0(), startIndex, packageName, activityName);
                    startIndex -= strings.size();
                    strings.addAll(oqStrings);
                    startIndex += strings.size();
                }
                if (binary.getParameter1() instanceof OntologyQuery) {
                    Set<StringInContext> oqStrings = getStringsFromOntologyQuery((OntologyQuery) binary.getParameter1(), startIndex, packageName, activityName);
                    startIndex -= strings.size();
                    strings.addAll(oqStrings);
                    startIndex += strings.size();
                }
            }
            if (op instanceof SugiliteTrinaryOperation) {
                SugiliteTrinaryOperation trinary = (SugiliteTrinaryOperation)op;
                if (trinary.getParameter0() instanceof OntologyQuery) {
                    Set<StringInContext> oqStrings = getStringsFromOntologyQuery((OntologyQuery) trinary.getParameter0(), startIndex, packageName, activityName);
                    startIndex -= strings.size();
                    strings.addAll(oqStrings);
                    startIndex += strings.size();
                }
                if (trinary.getParameter1() instanceof OntologyQuery) {
                    Set<StringInContext> oqStrings = getStringsFromOntologyQuery((OntologyQuery) trinary.getParameter1(), startIndex, packageName, activityName);
                    startIndex -= strings.size();
                    strings.addAll(oqStrings);
                    startIndex += strings.size();
                }
                if (trinary.getParameter2() instanceof OntologyQuery) {
                    Set<StringInContext> oqStrings = getStringsFromOntologyQuery((OntologyQuery) trinary.getParameter2(), startIndex, packageName, activityName);
                    startIndex -= strings.size();
                    strings.addAll(oqStrings);
                    startIndex += strings.size();
                }
            }
        }
        return strings;
    }

    // TODO should this method be in its own class?
    private static Set<StringInContext> getStringsFromScript(SugiliteStartingBlock script) {

        int currentIndex = 0;
        Set<StringInContext> strings = new HashSet<StringInContext>();

        for (SugiliteBlock block : script.getFollowingBlocks()) {
            Set<StringInContext> blockStrings = getStringsFromBlock(block, currentIndex);
            currentIndex -= strings.size();
            strings.addAll(blockStrings);
            currentIndex += strings.size();
        }

        return strings;
    }

    private static OntologyQuery getStringReplacementOntologyQuery(OntologyQuery oq, SugiliteBlockMetaInfo metaInfo, Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements) {
        String activityName = metaInfo.getUiSnapshot().getActivityName();
        String packageName = metaInfo.getUiSnapshot().getPackageName();

        if (oq instanceof LeafOntologyQuery) {
            LeafOntologyQuery loq = (LeafOntologyQuery) oq;
            if (Arrays.stream(Const.POTENTIALLY_PRIVATE_RELATIONS).anyMatch(loq.getR()::equals)) {
                StringInContext key = new StringInContextWithIndexAndPriority(activityName, packageName, loq.getObjectAsString());
                StringAlternativeGenerator.StringAlternative alt = replacements.get(key);
                if (alt == null) {
                    // TODO return generated alternative and/or hole
                    Log.v("PrepareScriptForSharingTask", "no string alternative found for \"" + loq.getObjectAsString() + "\"");
                    return null;
                } else if (alt.altText == loq.getObjectAsString()) {
                    Log.v("PrepareScriptForSharingTask", "not altering LeafOntologyQuery with object \"" + loq.getObjectAsString() + "\"");
                    return oq;
                } else {
                    Log.v("PrepareScriptForSharingTask", "replacing \"" + loq.getObjectAsString() + "\" with \"" + alt.altText + "\"");
                    return new StringAlternativeOntologyQuery(loq.getR(), alt);
                }
            } else {
                return oq;
            }
        }
        if (oq instanceof OntologyQueryWithSubQueries) {
            Set<OntologyQuery> subQs = ((OntologyQueryWithSubQueries) oq).getSubQueries();
            Set<OntologyQuery> newSubQs = new HashSet<OntologyQuery>();
            for (OntologyQuery subQ : subQs) {
                OntologyQuery replacement = getStringReplacementOntologyQuery(subQ, metaInfo, replacements);
                if (replacement == null) return null;
                newSubQs.add(replacement);
            }
            return ((OntologyQueryWithSubQueries) oq).cloneWithTheseSubQueries(newSubQs);
        }
        if (oq instanceof HashedStringOntologyQuery) {
            return oq;
        }
        Log.v("PrepareScriptForSharingTask", "not sure how to replace queries in " + oq.getClass().getCanonicalName());
        return null;
    }

    private static OntologyQuery getReplacementOntologyQuery(OntologyQuery oq, SugiliteBlockMetaInfo metaInfo, Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements) {
        OntologyQuery replacement = getStringReplacementOntologyQuery(oq, metaInfo, replacements);

        // uh oh this should be a UISnapshot not a SerializableUISnapshot
        //for (Pair<OntologyQuery, Double> pair : SugiliteBlockBuildingHelper.newGenerateDefaultQueries(metaInfo.getUiSnapshot(), metaInfo.getTargetEntity())

        if (replacement == null) throw new RuntimeException("not sure how to replace " + oq.toString());
        return replacement;
    }

    private static void replaceStringsInBlock(SugiliteBlock block, Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements) {
        if (block instanceof SugiliteOperationBlock) {
            SugiliteOperationBlock operationBlock = (SugiliteOperationBlock)block;
            SugiliteOperation op = ((SugiliteOperationBlock) block).getOperation();
            SugiliteBlockMetaInfo metaInfo = operationBlock.getSugiliteBlockMetaInfo();

            // replace strings in ontologyquery
            if (op instanceof SugiliteUnaryOperation) {
                SugiliteUnaryOperation unary = (SugiliteUnaryOperation)op;
                if (unary.getParameter0() instanceof OntologyQuery) {
                    unary.setParameter0(getReplacementOntologyQuery((OntologyQuery) unary.getParameter0(), metaInfo, replacements));
                }
            }
            if (op instanceof SugiliteBinaryOperation) {
                SugiliteBinaryOperation binary = (SugiliteBinaryOperation)op;
                if (binary.getParameter0() instanceof OntologyQuery) {
                    binary.setParameter0(getReplacementOntologyQuery((OntologyQuery) binary.getParameter0(), metaInfo, replacements));
                }
                if (binary.getParameter1() instanceof OntologyQuery) {
                    binary.setParameter1(getReplacementOntologyQuery((OntologyQuery) binary.getParameter1(), metaInfo, replacements));
                }
            }
            if (op instanceof SugiliteTrinaryOperation) {
                SugiliteTrinaryOperation trinary = (SugiliteTrinaryOperation)op;
                if (trinary.getParameter0() instanceof OntologyQuery) {
                    trinary.setParameter0(getReplacementOntologyQuery((OntologyQuery) trinary.getParameter0(), metaInfo, replacements));
                }
                if (trinary.getParameter1() instanceof OntologyQuery) {
                    trinary.setParameter1(getReplacementOntologyQuery((OntologyQuery) trinary.getParameter1(), metaInfo, replacements));
                }
                if (trinary.getParameter2() instanceof OntologyQuery) {
                    trinary.setParameter2(getReplacementOntologyQuery((OntologyQuery) trinary.getParameter2(), metaInfo, replacements));
                }
            }
        }
    }

    private static void replaceStringsInScript(SugiliteStartingBlock script, Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements) {
        for (SugiliteBlock block : script.getFollowingBlocks()) {
            replaceStringsInBlock(block, replacements);
        }
    }

    public SugiliteStartingBlock prepareScript(SugiliteStartingBlock script) throws Exception {
        Log.i("PrepareScriptForSharingTask", "PrepareScriptForSharingTask started");

        // get strings
        Set<StringInContext> originalQueryStrings = getStringsFromScript(script);

        Log.i("PrepareScriptForSharingTask", "size of query strings" + originalQueryStrings.size());

        // COMPUTE ALTERNATIVE STRINGS
        Map<StringInContext, Integer> originalQueryStringsIndex = new HashMap<>();
        Multimap<HashedString, StringInContextWithIndexAndPriority> decodedStrings = HashMultimap.create();

        Set<StringInContextWithIndexAndPriority> queryStrings = new HashSet<>();

        int index = 0;

        for (StringInContext s : originalQueryStrings) {
            Set<StringAlternativeGenerator.StringAlternative> alternatives = StringAlternativeGenerator.generateAlternatives(s.getText());
            StringInContextWithIndexAndPriority entry = new StringInContextWithIndexAndPriority(s.getActivityName(), s.getPackageName(), s.getText(), 0, index);
            decodedStrings.put(new HashedString(s.getText()), entry);
            originalQueryStringsIndex.put(s, index);
            queryStrings.add(entry);
            for (StringAlternativeGenerator.StringAlternative a : alternatives) {
                StringInContextWithIndexAndPriority alt = new StringInContextWithIndexAndPriority(s.getActivityName(), s.getPackageName(), a.altText, a.priority, index);
                queryStrings.add(alt);
                decodedStrings.put(new HashedString(alt.getText()), alt);
            }

            index++;
        }

        // FILTER OUT PRIVATE STRINGS
        StringAlternativeGenerator.StringAlternative[] bestMatch = sugiliteScriptSharingHTTPQueryManager.getFilteredStrings(queryStrings, originalQueryStringsIndex, decodedStrings);


        Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements = new HashMap<>();
        for (StringInContext s : originalQueryStrings) {
            int i = originalQueryStringsIndex.get(s);
            if (bestMatch[i] != null) {
                Log.v("PrepareScriptForSharingTask", "\"" + s.getText() + "\" ---> \"" + bestMatch[i].altText + "\"");
                replacements.put(s, bestMatch[i]);
            } else {
                Log.v("PrepareScriptForSharingTask", "\"" + s.getText() + "\" no replacement found");
            }
        }

        // REPLACE HAS_TEXT and HAS_CHILD_TEXT with hashed equivalents in queries
        //TODO: solve the "not sure how to replace" problem
        replaceStringsInScript(script, replacements);

        for (SugiliteBlock block : script.getFollowingBlocks()) {
            block.setDescription("this is not a trustworthy description");
            if (block instanceof SugiliteOperationBlock) {
                // TODO REPLACE HAS_TEXT and HAS_CHILD_TEXT with hashed equivalents in graph
                // in the meantime we'll just remove the ui snapshots
                // SerializableUISnapshot snapshot = ((SugiliteOperationBlock) block).getSugiliteBlockMetaInfo().getUiSnapshot();
                // UISnapshotShareUtils.prepareSnapshotForSharing(snapshot, replacements);
                ((SugiliteOperationBlock) block).getSugiliteBlockMetaInfo().setUiSnapshot(null);
            }
        }

        return script;
    }
}
