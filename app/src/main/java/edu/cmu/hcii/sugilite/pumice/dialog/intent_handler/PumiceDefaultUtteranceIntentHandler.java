package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.Activity;
import android.widget.ImageView;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Calendar;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.communication.SkipPumiceJSONSerialization;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceParsingConfirmationHandler;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

/**
 * @author toby
 * @date 10/26/18
 * @time 1:33 PM
 */

public class PumiceDefaultUtteranceIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface {
    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private Calendar calendar;

    private PumiceDefaultUtteranceIntentHandler pumiceDefaultUtteranceIntentHandler;

    public PumiceDefaultUtteranceIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context){
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.calendar = Calendar.getInstance();
        this.pumiceDefaultUtteranceIntentHandler = this;
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    /**
     * detect the intent type from a given user utterance
     * @param utterance
     * @return
     */
    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance){
        String text = utterance.getContent().toLowerCase();

        //**test***
        /*
        if (text.contains("weather")) {
            return PumiceIntent.TEST_WEATHER;
        }*/

        if (text.contains("start again") || text.contains("start over")) {
            return PumiceIntent.START_OVER;
        }

        /*if (text.contains("undo") || text.contains("go back to the last") || text.contains("go back to the previous")) {
            return PumiceIntent.UNDO_STEP;
        }

        if (text.contains("show existing")) {
            return PumiceIntent.SHOW_KNOWLEDGE;
        }

        if (text.contains("show raw")) {
            return PumiceIntent.SHOW_RAW_KNOWLEDGE;
        }*/

        else {
            return PumiceIntent.USER_INIT_INSTRUCTION;
        }
    }

    /**
     * handle the intent when the user is in the default state
     * @param dialogManager
     * @param pumiceIntent
     * @param utterance
     */
    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        switch (pumiceIntent) {
            case START_OVER:
                dialogManager.sendAgentMessage("I understand you want to start over: " + utterance.getContent(), true, false);
                dialogManager.startOverState();
                break;
            case UNDO_STEP:
                dialogManager.sendAgentMessage("I understand you want to undo: " + utterance.getContent(), true, false);
                dialogManager.revertToLastState();
                break;
            case TEST_WEATHER:
                ImageView imageView = new ImageView(context);
                imageView.setImageDrawable(context.getResources().getDrawable(R.mipmap.user_avatar));//SHOULD BE R.mipmap.demo_card
                dialogManager.sendAgentViewMessage(imageView, "Here is the weather", true, false);
                break;
            case USER_INIT_INSTRUCTION:
                //dialogManager.sendAgentMessage("I have received your instruction: " + utterance.getContent(), true, false);
                PumiceInstructionPacket pumiceInstructionPacket = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.USER_INIT_INSTRUCTION, calendar.getTimeInMillis(), utterance.getContent(), "ROOT");
                //dialogManager.sendAgentMessage("Sending out the server query below...", true, false);
                //dialogManager.sendAgentMessage(pumiceInstructionPacket.toString(), false, false);
                try {
                    dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket, this);
                } catch (Exception e){
                    //TODO: error handling
                    e.printStackTrace();
                    pumiceDialogManager.sendAgentMessage("Failed to send the query", true, false);
                }
                System.out.println(pumiceInstructionPacket.toString());
                break;
            case SHOW_KNOWLEDGE:
                dialogManager.sendAgentMessage("Below are the existing knowledge...", true, false);
                dialogManager.sendAgentMessage(dialogManager.getPumiceKnowledgeManager().getKnowledgeInString(), false, false);
                break;
            case SHOW_RAW_KNOWLEDGE:
                dialogManager.sendAgentMessage("Below are the raw knowledge..." + dialogManager.getPumiceKnowledgeManager().getRawKnowledgeInString(), true, false);
                dialogManager.sendAgentMessage(dialogManager.getPumiceKnowledgeManager().getRawKnowledgeInString(), false, false);
                break;
            default:
                dialogManager.sendAgentMessage("I don't understand this intent", true, false);
                break;
        }
    }

    @Override
    public void resultReceived(int responseCode, String result, String originalQuery) {
        //handle server response from the semantic parsing server
        Gson gson = new GsonBuilder()
                .addSerializationExclusionStrategy(new ExclusionStrategy()
                {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f)
                    {
                        return f.getAnnotation(SkipPumiceJSONSerialization.class) != null;
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz)
                    {
                        return false;
                    }
                })
                .create();
        try {
            PumiceSemanticParsingResultPacket resultPacket = gson.fromJson(result, PumiceSemanticParsingResultPacket.class);
            resultPacket.cleanFormula();
            if (resultPacket.utteranceType != null) {
                switch (PumiceUtteranceIntentHandler.PumiceIntent.valueOf(resultPacket.utteranceType)) {
                    case USER_INIT_INSTRUCTION:
                        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            // send the result to a PumiceScriptExecutingConfirmationIntentHandler
                            PumiceParsingConfirmationHandler parsingConfirmationHandler = new PumiceParsingConfirmationHandler(context, pumiceDialogManager, 0);
                            parsingConfirmationHandler.handleParsingResult(resultPacket, new Runnable() {
                                @Override
                                public void run() {
                                    //runnable for retry
                                    pumiceDialogManager.updateUtteranceIntentHandlerInANewState(pumiceDefaultUtteranceIntentHandler);
                                    sendPromptForTheIntentHandler();

                                }
                            }, new PumiceParsingConfirmationHandler.ConfirmedParseRunnable() {
                                @Override
                                public void run(String confirmedFormula) {
                                    //runnable for confirmed parse
                                    pumiceDialogManager.getExecutorService().submit(new Runnable() {
                                        @Override
                                        public void run() {
                                            //parse and process the server response
                                            pumiceDialogManager.getPumiceInitInstructionParsingHandler().parseFromNewInitInstruction(confirmedFormula, resultPacket.userUtterance);
                                        }
                                    });
                                }
                            }, false);

                        } else {
                            throw new RuntimeException("empty server result");
                        }
                        break;
                    default:
                        throw new RuntimeException("wrong type of result");
                }
            }
        } catch (Exception e){
            //TODO: error handling
            pumiceDialogManager.sendAgentMessage("Can't read from the server response", true, false);

            pumiceDialogManager.sendAgentMessage("OK. Let's try again.", true, false);
            pumiceDialogManager.updateUtteranceIntentHandlerInANewState(pumiceDefaultUtteranceIntentHandler);
            sendPromptForTheIntentHandler();
            e.printStackTrace();
        }
    }

    @Override
    public void sendPromptForTheIntentHandler() {
        pumiceDialogManager.getSugiliteVoiceRecognitionListener().setContextPhrases(Const.INIT_INSTRUCTION_CONTEXT_WORDS);
        pumiceDialogManager.sendAgentMessage(String.format("Hi I'm %s bot! How can I help you?", Const.appName), true, true);
    }

}
