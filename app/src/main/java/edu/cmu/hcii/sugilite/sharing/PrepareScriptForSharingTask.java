package edu.cmu.hcii.sugilite.sharing;

import android.util.Log;
import com.google.api.client.repackaged.com.google.common.base.Objects;
import com.google.common.base.Function;
import com.google.common.collect.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteBinaryOperation;
import edu.cmu.hcii.sugilite.model.operation.trinary.SugiliteTrinaryOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteUnaryOperation;
import edu.cmu.hcii.sugilite.ontology.*;

import javax.annotation.Nullable;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;

public class PrepareScriptForSharingTask implements Callable<SugiliteStartingBlock> {

    private SugiliteStartingBlock script;

    public SugiliteStartingBlock getScript() {
        return script;
    }

    public void setScript(SugiliteStartingBlock script) {
        this.script = script;
    }

    private static class StringInContext {
        public String activityName;
        public String packageName;
        public String text;
        public int priority = -1;
        public int index = -1;

        public StringInContext(String activityName, String packageName, String text) {
            this.activityName = activityName;
            this.packageName = packageName;
            this.text = text;
        }

        public StringInContext(String activityName, String packageName, String text, int priority, int index) {
            this.activityName = activityName;
            this.packageName = packageName;
            this.text = text;
            this.priority = priority;
            this.index = index;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(text, activityName, packageName);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof StringInContext) {
                StringInContext sic = (StringInContext)obj;
                return text.equals(sic.text) && activityName.equals(sic.activityName) && packageName.equals(sic.packageName);
            }
            return false;
        }

        public String toJson() {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"package\": \"" + packageName + "\"");
            sb.append(",\"activity\": \"" + activityName + "\"");
            sb.append(",\"text_hash\": \"" + new HashedString(text).toString() + "\"");
            //sb.append(",\"priority\": " + this.priority);
            //sb.append(",\"index\": " + index);
            sb.append("}");
            return sb.toString();
        }

    }

    public static Set<StringInContext> getStringsFromOntologyQuery(OntologyQuery query, int startIndex, String packageName, String activityName) {
        Set<StringInContext> strings = new HashSet<StringInContext>();

        if (query instanceof LeafOntologyQuery) {
            LeafOntologyQuery loq = (LeafOntologyQuery) query;
            if (Arrays.stream(Const.POTENTIALLY_PRIVATE_RELATIONS).anyMatch(loq.getR()::equals)) {
                strings.add(new StringInContext(activityName, packageName, loq.getObjectAsString(), 0, startIndex++));
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

    public static Set<StringInContext> getStringsFromBlock(SugiliteBlock block, int startIndex) {
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
    public static Set<StringInContext> getStringsFromScript(SugiliteStartingBlock script) {

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

    public static OntologyQuery getReplacementOntologyQuery(OntologyQuery oq, String packageName, String activityName, Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements) {
        if (oq instanceof LeafOntologyQuery) {
            LeafOntologyQuery loq = (LeafOntologyQuery) oq;
            if (Arrays.stream(Const.POTENTIALLY_PRIVATE_RELATIONS).anyMatch(loq.getR()::equals)) {
                StringInContext key = new StringInContext(activityName, packageName, loq.getObjectAsString());
                StringAlternativeGenerator.StringAlternative alt = replacements.get(key);
                if (alt.altText == loq.getObjectAsString()) {
                    Log.v("PrepareScriptForSharingTask", "not altering LeafOntologyQuery with object \"" + loq.getObjectAsString() + "\"");
                    return oq;
                }
                if (alt != null) {
                    Log.v("PrepareScriptForSharingTask", "replacing \"" + loq.getObjectAsString() + "\" with \"" + alt.altText + "\"");
                    return new StringAlternativeOntologyQuery(loq.getR(), alt);
                } else {
                    // TODO return generated alternative and/or hole
                    throw new RuntimeException("not sure how to replace a query");
                }
            } else {
                return oq;
            }
        }
        if (oq instanceof OntologyQueryWithSubQueries) {
            Set<OntologyQuery> subQs = ((OntologyQueryWithSubQueries) oq).getSubQueries();
            Set<OntologyQuery> newSubQs = new HashSet<OntologyQuery>();
            for (OntologyQuery subQ : subQs) {
                newSubQs.add(getReplacementOntologyQuery(subQ, packageName, activityName, replacements));
            }
            return ((OntologyQueryWithSubQueries) oq).cloneWithTheseSubQueries(newSubQs);
        }
        if (oq instanceof HashedStringOntologyQuery) {
            return oq;
        }
        throw new RuntimeException("not sure how to replace queries in " + oq.getClass().getCanonicalName());
    }

    public static void replaceStringsInBlock(SugiliteBlock block, Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements) {
        // TODO replace strings in graph
        if (block instanceof SugiliteOperationBlock) {
            SugiliteOperationBlock operationBlock = (SugiliteOperationBlock)block;
            SugiliteOperation op = ((SugiliteOperationBlock) block).getOperation();
            SerializableUISnapshot snapshot = operationBlock.getFeaturePack().serializableUISnapshot;
            String packageName = snapshot.getPackageName();
            String activityName = snapshot.getActivityName();
            if (op instanceof SugiliteUnaryOperation) {
                SugiliteUnaryOperation unary = (SugiliteUnaryOperation)op;
                if (unary.getParameter0() instanceof OntologyQuery) {
                    unary.setParameter0(getReplacementOntologyQuery((OntologyQuery) unary.getParameter0(), packageName, activityName, replacements));
                }
            }
            if (op instanceof SugiliteBinaryOperation) {
                SugiliteBinaryOperation binary = (SugiliteBinaryOperation)op;
                if (binary.getParameter0() instanceof OntologyQuery) {
                    binary.setParameter0(getReplacementOntologyQuery((OntologyQuery) binary.getParameter0(), packageName, activityName, replacements));
                }
                if (binary.getParameter1() instanceof OntologyQuery) {
                    binary.setParameter1(getReplacementOntologyQuery((OntologyQuery) binary.getParameter1(), packageName, activityName, replacements));
                }
            }
            if (op instanceof SugiliteTrinaryOperation) {
                SugiliteTrinaryOperation trinary = (SugiliteTrinaryOperation)op;
                if (trinary.getParameter0() instanceof OntologyQuery) {
                    trinary.setParameter0(getReplacementOntologyQuery((OntologyQuery) trinary.getParameter0(), packageName, activityName, replacements));
                }
                if (trinary.getParameter1() instanceof OntologyQuery) {
                    trinary.setParameter1(getReplacementOntologyQuery((OntologyQuery) trinary.getParameter1(), packageName, activityName, replacements));
                }
                if (trinary.getParameter2() instanceof OntologyQuery) {
                    trinary.setParameter2(getReplacementOntologyQuery((OntologyQuery) trinary.getParameter2(), packageName, activityName, replacements));
                }
            }
        }
    }

    public static void replaceStringsInScript(SugiliteStartingBlock script, Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements) {
        for (SugiliteBlock block : script.getFollowingBlocks()) {
            replaceStringsInBlock(block, replacements);
        }
    }

    @Override
    public SugiliteStartingBlock call() throws Exception {

        Log.i("PrepareScriptForSharingTask", "AAAAAAAA we've started");

        // get strings
        Set<StringInContext> queryStrings = getStringsFromScript(script);

        Log.i("PrepareScriptForSharingTask", "size of querystrings " + queryStrings.size());

        // TODO debug only
        for (StringInContext s : queryStrings) {
            Log.v("PrepareScriptForSharingTask", s.index + " : " + s.text + " \t\t\t( " + s.packageName + " , " + s.activityName + " )");
        }

        // COMPUTE ALTERNATIVE STRINGS
        Map<StringInContext, Integer> originalQueryStringsIndex = new HashMap<>();
        Multimap<HashedString, StringInContext> decodedStrings = HashMultimap.create();
        Set<StringInContext> alternateQueriesTemp = new HashSet<StringInContext>();
        for (StringInContext s : queryStrings) {
            Set<StringAlternativeGenerator.StringAlternative> alternatives = StringAlternativeGenerator.generateAlternatives(s.text);
            decodedStrings.put(new HashedString(s.text), s);
            originalQueryStringsIndex.put(s, s.index);
            for (StringAlternativeGenerator.StringAlternative a : alternatives) {
                StringInContext alt = new StringInContext(s.activityName, s.packageName, a.altText, a.priority, s.index);
                alternateQueriesTemp.add(alt);
                decodedStrings.put(new HashedString(alt.text), alt);
            }
        }
        queryStrings.addAll(alternateQueriesTemp);

        // FILTER OUT PRIVATE STRINGS
        URL filterStringUrl = new URL(Const.SHARING_SERVER_BASE_URL + Const.FILTER_UI_STRING_ENDPOINT);
        HttpURLConnection urlConnection = (HttpURLConnection) filterStringUrl.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setReadTimeout(1 * 3000);
        urlConnection.setConnectTimeout(1 * 3000);

        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setChunkedStreamingMode(0); // this might increase performance?
        BufferedOutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

        writer.write("[");
        boolean first = true;
        for (StringInContext s : queryStrings) {
            if (!first) writer.write(',');
            writer.write(s.toJson());
            first = false;
        }
        writer.write("]");

        writer.flush();
        writer.close();
        out.close();
        urlConnection.connect();

        int responseCode = urlConnection.getResponseCode();

        if (responseCode != HttpURLConnection.HTTP_OK) {
            // TODO scream or something
            Log.e("PrepareScriptForSharingTask", "Uh oh");
            throw new Exception("aaa");
        }

        // SELECT BEST ALTERNATIVE FROM RESPONSES
        StringAlternativeGenerator.StringAlternative[] bestMatch = new StringAlternativeGenerator.StringAlternative[originalQueryStringsIndex.size()];
        Gson g = new Gson();
        JsonArray filteredResponses = g.fromJson(new InputStreamReader(urlConnection.getInputStream()), JsonArray.class);
        for (JsonElement elt : filteredResponses){
            if (elt instanceof JsonObject) {
                JsonObject o = (JsonObject)elt;
                for (StringInContext s : decodedStrings.get(HashedString.fromEncodedString(o.get("text_hash").getAsString()))) {
                    if (o.get("activity").getAsString().equals(s.activityName) && o.get("package").getAsString().equals(s.packageName)) {
                        if (bestMatch[s.index] == null || s.priority <= bestMatch[s.index].priority) {
                            bestMatch[s.index] = new StringAlternativeGenerator.StringAlternative(s.text, s.priority);
                        }
                    }
                }
            }
        }

        Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements = new HashMap<>();
        for (StringInContext s : originalQueryStringsIndex.keySet()) {
            if (bestMatch[s.index] != null) {
                Log.v("PrepareScriptForSharingTask", "\"" + s.text + "\" ---> \"" + bestMatch[s.index].altText + "\"");
                replacements.put(s, bestMatch[s.index]);
            } else {
                Log.v("PrepareScriptForSharingTask", "\"" + s.text + "\" no replacement found");
            }
        }

        // TODO REPLACE HAS_TEXT and HAS_CHILD_TEXT with hashed equivalents in graph

        // REPLACE HAS_TEXT and HAS_CHILD_TEXT with hashed equivalents in queries
        replaceStringsInScript(script, replacements);

        return script;
    }
}
