package edu.cmu.hcii.sugilite.source_parsing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;

/**
 * @author toby
 * @date 3/18/18
 * @time 5:28 PM
 */
public class SugiliteScriptParser {
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;

    public SugiliteScriptParser(){
        //constructor
        this.ontologyDescriptionGenerator = null;
    }

    public SugiliteScriptParser(OntologyDescriptionGenerator ontologyDescriptionGenerator){
        this.ontologyDescriptionGenerator = ontologyDescriptionGenerator;
    }

    public SugiliteBlock parse(String source){
        return null;
    }
    /**
     * perform tokenization
     * @param source
     * @return
     */
    public static List<String> tokenize(String source){
        List<String> list = new ArrayList<String>();
        source = source.replace("\\\"", "\"");
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(source.replace("(", " ( ").replace(")", " ) "));
        while (m.find()) {
            String result = new String(m.group(1));
            if(result.startsWith("\"") && result.endsWith("\"")){
                result = result.substring(1, result.length() - 1);
            }
            list.add(result.trim());
        }
        return list;
    }

    /**
     * parse a list of tokens into a syntax tree
     * @param tokens
     * @return
     * @throws RuntimeException
     */
    private SugiliteScriptNode parseTokens(LinkedList<String> tokens) throws RuntimeException{
        SugiliteScriptNode root = new SugiliteScriptNode();
        SugiliteScriptNode current = root;
        //parse tokens
        for(String token : tokens){
            if(token.contentEquals("(")){
                SugiliteScriptNode newNode = new SugiliteScriptNode();
                newNode.setParent(current);
                current.addChild(newNode);
                current = newNode;
                current.setScriptContent(current.getScriptContent() + " " + token);
            }

            else if(token.contentEquals(")")){
                if(current.getParent() == null){
                    throw new RuntimeException("Unexpected (");
                }
                current.setScriptContent(current.getScriptContent() + " " + token);
                current.getParent().setScriptContent(current.getParent().getScriptContent() + " " + current.getScriptContent());
                current = current.getParent();
            }

            else {
                //regular atom
                SugiliteScriptNode newNode = new SugiliteScriptNode();
                newNode.setValue(token);
                newNode.setScriptContent(newNode.getScriptContent() + " " + token);
                newNode.setParent(current);
                current.addChild(newNode);
                current.setScriptContent(current.getScriptContent() + " " + newNode.getScriptContent());
            }
        }
        return root;
    }

    private List<SugiliteScriptExpression> runASTParsingPipeline(String input){
        List<String> tokenizationResult = tokenize(input);
        SugiliteScriptNode parsingResult = parseTokens(new LinkedList<>(tokenizationResult));
        List<SugiliteScriptExpression> expressionList = new ArrayList<>();
        for(SugiliteScriptNode node : parsingResult.getChildren()){
            expressionList.add(SugiliteScriptExpression.parse(node));
        }
        return expressionList;
    }

    public SugiliteStartingBlock parseBlockFromString(String input){
        List<SugiliteScriptExpression> expressionList = runASTParsingPipeline(input);
        //System.out.println("Final result: " + expressionList);
        SugiliteStartingBlock startingBlock = new SugiliteStartingBlock("test.SugiliteScript");
        SugiliteBlock currentBlock = startingBlock;
        for(SugiliteScriptExpression expression : expressionList){
            //turn each expression to a block
            SugiliteBlock block = expression.toSugiliteBlock(startingBlock, ontologyDescriptionGenerator);
            if(block instanceof SugiliteStartingBlock) {
                //contains a starting block
                startingBlock = (SugiliteStartingBlock) block;
            }
            else {
                currentBlock.setNextBlock(block);
                block.setPreviousBlock(currentBlock);
            }
            currentBlock = block;
        }
        return startingBlock;
    }

    public static String scriptToString(SugiliteBlock block){
        if(block != null) {
            String result = block.toString();
            if (block.getNextBlock() != null) {
                result += "\n";
                result += scriptToString(block.getNextBlock());
            }
            return result;
        }
        else {
            return "NULL";
        }
    }

    public static void main(String[] args){
        //an interactive parser testing environment
        SugiliteScriptParser sugiliteScriptParser = new SugiliteScriptParser();
        while (true) {
            BufferedReader screenReader = new BufferedReader(new InputStreamReader(System.in));
            String input = "";
            System.out.print("> ");
            try {
                input = screenReader.readLine();
                //System.out.println();
            }
            catch (Exception e){
                e.printStackTrace();
            }
            try {
                if(input.length() > 0) {
                    SugiliteStartingBlock script = sugiliteScriptParser.parseBlockFromString(input);
                    System.out.println(sugiliteScriptParser.scriptToString(script));
                }
            }
            catch (Exception e){
                System.out.println("Failed to parse the query");
                e.printStackTrace();
            }

        }
    }
}
