package edu.cmu.hcii.sugilite.sharing;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.sharing.model.HashedString;
import edu.cmu.hcii.sugilite.sharing.model.StringInContext;
import edu.cmu.hcii.sugilite.sharing.model.StringInContextWithIndexAndPriority;

public class SugiliteSharingScriptPreparer {

    private SugiliteScriptSharingHTTPQueryManager sugiliteScriptSharingHTTPQueryManager;
    private SugiliteUISnapshotPreparer sugiliteUISnapshotPreparer;

    public SugiliteSharingScriptPreparer(Context context) {
        this.sugiliteScriptSharingHTTPQueryManager = SugiliteScriptSharingHTTPQueryManager.getInstance(context);
        this.sugiliteUISnapshotPreparer = new SugiliteUISnapshotPreparer(context);
    }

    public static Map<StringInContext, StringAlternativeGenerator.StringAlternative> getReplacementsFromStringInContextSet(Set<StringInContext> originalQueryStrings, SugiliteScriptSharingHTTPQueryManager sugiliteScriptSharingHTTPQueryManager) throws Exception {
        // 2. COMPUTE ALTERNATIVE STRINGS
        Map<StringInContext, Integer> originalQueryStringsIndexMap = new HashMap<>();
        Map<Integer, StringInContext> indexOriginalQueryStringsMap = new HashMap<>();
        Multimap<HashedString, StringInContextWithIndexAndPriority> stringHashOriginalStringMap = HashMultimap.create();
        Set<StringInContextWithIndexAndPriority> queryStringSet = new HashSet<>();

        int index = 0;
        for (StringInContext s : originalQueryStrings) {
            Set<StringAlternativeGenerator.StringAlternative> alternatives = StringAlternativeGenerator.generateAlternatives(s.getText());
            StringInContextWithIndexAndPriority entry = new StringInContextWithIndexAndPriority(s.getActivityName(), s.getPackageName(), s.getText(), 0, index);
            stringHashOriginalStringMap.put(new HashedString(s.getText()), entry);
            originalQueryStringsIndexMap.put(s, index);
            indexOriginalQueryStringsMap.put(index, s);
            queryStringSet.add(entry);
            for (StringAlternativeGenerator.StringAlternative a : alternatives) {
                StringInContextWithIndexAndPriority alt = new StringInContextWithIndexAndPriority(s.getActivityName(), s.getPackageName(), a.altText, a.priority, index);
                queryStringSet.add(alt);
                stringHashOriginalStringMap.put(new HashedString(alt.getText()), alt);
            }
            index++;
        }

        //3. FILTER OUT PRIVATE STRINGS
        SugiliteScriptSharingHTTPQueryManager.GetFilteredStringsTaskResult queryResult = sugiliteScriptSharingHTTPQueryManager.getFilteredStrings(queryStringSet, originalQueryStringsIndexMap, stringHashOriginalStringMap);


        Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements = new HashMap<>();
        queryResult.getIndexStringAlternativeMap().forEach((i, s) -> replacements.put(indexOriginalQueryStringsMap.get(i), s));
        return replacements;
    }

    //TODO: create a new prepareScript method that returns the ontology queries that were replaced


    public SugiliteStartingBlock prepareScript(SugiliteStartingBlock script) throws Exception {
        // 1. get all StringInContext in the block
        Set<StringInContext> originalQueryStrings = getStringsFromScript(script);
        Log.i("PrepareScriptForSharingTask", "size of query strings" + originalQueryStrings.size());


        Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements = getReplacementsFromStringInContextSet(originalQueryStrings, sugiliteScriptSharingHTTPQueryManager);

        //4. REPLACE HAS_TEXT and HAS_CHILD_TEXT with hashed equivalents in queries
        replaceStringsInScript(script, replacements);

        for (SugiliteBlock block : script.getFollowingBlocks()) {
            block.setDescription("this is not a trustworthy description");
            if (block instanceof SugiliteOperationBlock) {
                //process the UI snapshot
                if (((SugiliteOperationBlock) block).getSugiliteBlockMetaInfo() != null) {
                    SerializableUISnapshot oldUISnapshot = ((SugiliteOperationBlock) block).getSugiliteBlockMetaInfo().getUiSnapshot();
                    if (oldUISnapshot != null) {
                        ((SugiliteOperationBlock) block).getSugiliteBlockMetaInfo().setUiSnapshot(sugiliteUISnapshotPreparer.prepareUISnapshot(oldUISnapshot));
                    }
                }
            }
        }

        return script;
    }

    private static Set<StringInContext> getStringsFromOntologyQuery(OntologyQuery query, int startIndex, String packageName, String activityName) {
        Set<StringInContext> strings = new HashSet<StringInContext>();

        if (query instanceof LeafOntologyQuery) {
            LeafOntologyQuery loq = (LeafOntologyQuery) query;
            if (Arrays.stream(POTENTIALLY_PRIVATE_RELATIONS).anyMatch(loq.getR()::equals)) {
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
            String packageName = "";
            String activityName = "";

            if (operationBlock.getSugiliteBlockMetaInfo() != null && operationBlock.getSugiliteBlockMetaInfo().getUiSnapshot() != null) {
                SerializableUISnapshot snapshot = operationBlock.getSugiliteBlockMetaInfo().getUiSnapshot();
                packageName = snapshot.getPackageName();
                activityName = snapshot.getActivityName();

            }

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

    /**
     * return a set of all strings and their contexts from a script
     * @param script
     * @return
     */
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

    /**
     * replace applicable strings the the OntologyQuery oq based on alternatives provided in replacements
     * @param oq
     * @param metaInfo
     * @param replacements
     * @return
     */
    public static final SugiliteRelation[] POTENTIALLY_PRIVATE_RELATIONS = {
            SugiliteRelation.HAS_TEXT,
            SugiliteRelation.HAS_CHILD_TEXT,
            SugiliteRelation.HAS_CONTENT_DESCRIPTION
    };
    private static OntologyQuery getStringReplacementOntologyQuery(OntologyQuery oq, SugiliteBlockMetaInfo metaInfo, Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements) {
        String activityName = metaInfo.getUiSnapshot().getActivityName();
        String packageName = metaInfo.getUiSnapshot().getPackageName();


        if (oq instanceof LeafOntologyQuery) {
            LeafOntologyQuery loq = (LeafOntologyQuery) oq;
            if (Arrays.stream(POTENTIALLY_PRIVATE_RELATIONS).anyMatch(loq.getR()::equals)) {
                StringInContext key = new StringInContextWithIndexAndPriority(activityName, packageName, loq.getObjectAsString());
                StringAlternativeGenerator.StringAlternative alt = replacements.get(key);
                if (alt == null) {
                    // TODO return generated alternative and/or hole
                    Log.v("PrepareScriptForSharingTask", "no string alternative found for \"" + loq.getObjectAsString() + "\"");
                    return null;
                } else if (alt.type == StringAlternativeGenerator.ORIGINAL_TYPE) {
                    Log.v("PrepareScriptForSharingTask", "not altering LeafOntologyQuery with object \"" + loq.getObjectAsString() + "\"");
                    return oq;
                } else if (alt.type == StringAlternativeGenerator.PATTERN_MATCH_TYPE) {
                    Log.v("PrepareScriptForSharingTask", "replacing \"" + loq.getObjectAsString() + "\" with \"" + alt.altText + "\"");
                    return new StringAlternativeOntologyQuery(loq.getR(), alt);
                } else if (alt.type == StringAlternativeGenerator.HASH_TYPE) {
                    Log.v("PrepareScriptForSharingTask", "replacing \"" + loq.getObjectAsString() + "\" with \"" + alt.altText + "\"");
                    return new HashedStringOntologyQuery(loq.getR(), HashedString.fromEncodedString(alt.altText, true));
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
                if (replacement == null) {
                    return null;
                }
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

    /**
     * replace applicable strings the the OntologyQuery oq based on alternatives provided in replacements
     * @param oq
     * @param metaInfo
     * @param replacements
     * @return
     */
    private static OntologyQuery getReplacementOntologyQuery(OntologyQuery oq, SugiliteBlockMetaInfo metaInfo, Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements) {
        OntologyQuery replacement = getStringReplacementOntologyQuery(oq, metaInfo, replacements);

        // uh oh this should be a UISnapshot not a SerializableUISnapshot
        //for (Pair<OntologyQuery, Double> pair : SugiliteBlockBuildingHelper.newGenerateDefaultQueries(metaInfo.getUiSnapshot(), metaInfo.getTargetEntity())

        if (replacement == null) {
            //no replacement available

            //throw new RuntimeException("not sure how to replace " + oq.toString());
        }
        return replacement;
    }

    /**
     * replace applicable strings in the block based on alternatives provided in replacements
     * @param block
     * @param replacements
     */
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

    /**
     * replace applicable strings in the script based on alternatives provided in replacements
     * @param script
     * @param replacements
     */
    private static void replaceStringsInScript(SugiliteStartingBlock script, Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements) {
        for (SugiliteBlock block : script.getFollowingBlocks()) {
            replaceStringsInBlock(block, replacements);
        }
    }
}
