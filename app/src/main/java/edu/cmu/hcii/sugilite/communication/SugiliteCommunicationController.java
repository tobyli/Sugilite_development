package edu.cmu.hcii.sugilite.communication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

/**
 * Created by oscarr on 7/7/16.
 */
public class SugiliteCommunicationController {
    private final String TAG = SugiliteCommunicationController.class.getName();
    private Messenger sender = null; //used to make an RPC invocation
    private Messenger receiver = null; //invocation replies are processed by this Messenger (Middleware)
    private boolean isBound = false;
    private ServiceConnection connection; //receives callbacks from bind and unbind invocations
    private Context context;
    private final int REGISTER = 1;
    private final int UNREGISTER = 2;
    private final int RESPONSE = 3;
    private final int START_TRACKING = 4;
    private final int STOP_TRACKING = 5;
    private final int GET_ALL_SCRIPTS = 6;
    private final int GET_SCRIPT = 7;
    private final int APP_TRACKER_ID = 1001;


    public SugiliteCommunicationController(Context context) {
        this.connection = new RemoteServiceConnection();
        this.receiver = new Messenger(new IncomingHandler());
        this.context = context.getApplicationContext();
    }

    public void start(){
        Intent intent = createExplicitFromImplicitIntent( context,
                new Intent( "com.yahoo.inmind.services.generic.control.ExternalAppCommService" ) );
        context.bindService(intent, this.connection, Context.BIND_AUTO_CREATE);
    }

    public void stop(){
        if (this.isBound) {
            context.unbindService(connection);
            this.isBound = false;
        }
    }

    public boolean register(){
        return sendMessage(REGISTER, 0, null);
    }

    public boolean unregister(){
        return sendMessage(UNREGISTER, 0, null);
    }

    public boolean sendAllScripts(){
        return sendMessage( RESPONSE, GET_ALL_SCRIPTS, getDummyListOfScripts() );
    }

    public boolean sendScript(String scriptName){
        // you should send back the script which name is "scriptName"... now, we are using a dummy
        // script for testing purposes
        return sendMessage( RESPONSE, GET_SCRIPT, getDummyScript() );
    }

    private boolean sendMessage(int messageType, int arg2, String obj){
        if (isBound) {
            Message message = Message.obtain(null, messageType, APP_TRACKER_ID, 0);
            try {
                message.replyTo = receiver;
                message.arg2 = arg2;
                if( obj != null ){
                    Bundle bundle = new Bundle();
                    bundle.putString( "response", obj );
                    message.setData( bundle );
                }
                sender.send(message);
                return true;
            } catch (RemoteException rme) {
                return false;
            }
        } else {
            return false;
        }
    }


    private class RemoteServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName component, IBinder binder) {
            sender = new Messenger(binder);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            sender = null;
            isBound = false;
        }
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what) {
                case START_TRACKING:
                    //TODO: start app tracking service
                    Log.d( TAG, "Start Tracking");
                    break;
                case STOP_TRACKING:
                    //TODO: stop app tracking service
                    // ....
                    if( msg.arg1 == 1 ) { // send back tracking log (script)? false == 0, true == 1.
                        //TODO: replace getDummyScript by the corresponding script
                        SugiliteCommunicationController.this.sendMessage( RESPONSE, GET_SCRIPT, getDummyScript() );
                    }
                    break;
                case GET_ALL_SCRIPTS:
                    sendAllScripts();
                    break;
                case GET_SCRIPT:
                    sendScript( msg.getData().getString("request") );
                    break;
                default:
                    Log.e( TAG, "Message not supported!");
                    break;
            }
        }
    }


    public Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }


    private String getDummyListOfScripts() {
        return "{\n" +
                "    \"scripts\": [\n" +
                "        {\n" +
                "                \"name\": \"script1\",\n" +
                "                \"created\": \"124578987845\"\n" +
                "        },\n" +
                "        {\n" +
                "                \"name\": \"script2\",\n" +
                "                \"created\": \"124578987845\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
    }

    public String getDummyScript() {
        return "{\"name\":\"Starbucks\",\"next\":{\"actionType\":\"CLICK\",\"elementFilter\":{\"packageName\":\"com.starbucks.mobilecard\"},\"next\":{\"actionParameter\":\"Latte\",\"actionType\":\"SET_TEXT\",\"elementFilter\":{\"contentDescription\":\"Search bar\",\"packageName\":\"com.starbucks.mobilecard\"},\"next\":{\"actionParameter\":\"condition\",\"actionType\":\"IF_CONDITION\"}}}}";
//        return "{\n" +
//                "  \"name\": \"Starbucks\",\n" +
//                "  \"next\": {\n" +
//                "    \"actionType\": \"CLICK\",\n" +
//                "    \"elementFilter\": {\n" +
//                "      \"packageName\": \"com.starbucks.mobilecard\",\n" +
//                "      \"text\": \"Search\",\n" +
//                "      \"screenLocation\": \"somewhere\",\n" +
//                "      \"viewID\": \"something\"\n" +
//                "    },\n" +
//                "    \"next\": {\n" +
//                "      \"actionType\": \"SET_TEXT\",\n" +
//                "      \"actionParameter\": \"Latte\",\n" +
//                "      \"elementFilter\": {\n" +
//                "        \"packageName\": \"com.starbucks.mobilecard\",\n" +
//                "        \"contentDescription\": \"Search bar\"\n" +
//                "      },\n" +
//                "      \"next\": {\n" +
//                "        \"actionType\": \"IF_CONDITION\",\n" +
//                "        \"actionParameter\": \"condition\",\n" +
//                "        \"branch1\": {},\n" +
//                "        \"branch2\": {}\n" +
//                "      }\n" +
//                "    }\n" +
//                "  }\n" +
//                "}";
    }
}
