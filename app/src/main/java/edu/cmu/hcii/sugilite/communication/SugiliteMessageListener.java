package edu.cmu.hcii.sugilite.communication;

/**
 * Created by oscarr on 12/1/16.
 * This class will support the communication from Sugilite to Middleware by notifying any
 * component that implements this interface
 */
public interface SugiliteMessageListener {
    void onReceiveMessage(int arg2, String obj);
}
