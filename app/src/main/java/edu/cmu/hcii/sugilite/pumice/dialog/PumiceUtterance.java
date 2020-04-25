package edu.cmu.hcii.sugilite.pumice.dialog;

import android.text.SpannableString;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @author toby
 * @date 4/23/20
 * @time 9:30 PM
 */
public class PumiceUtterance implements Serializable {
    private PumiceDialogManager.Sender sender;
    private CharSequence content;
    private long timeStamp;
    private boolean requireUserResponse;
    private boolean isSpoken;

    public CharSequence getContent() {
        return content;
    }

    public PumiceDialogManager.Sender getSender() {
        return sender;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public boolean isRequireUserResponse() {
        return requireUserResponse;
    }

    public boolean isSpoken() {
        return isSpoken;
    }

    //if set to true, this utterance will trigger a recording of user input

    public PumiceUtterance(PumiceDialogManager.Sender sender, String content, long timeStamp, boolean isSpoken, boolean requireUserResponse){
        this.sender = sender;
        this.content = new SpannableString(content);
        this.timeStamp = timeStamp;
        this.isSpoken = isSpoken;
        this.requireUserResponse = requireUserResponse;
    }

    public PumiceUtterance(PumiceDialogManager.Sender sender, CharSequence content, long timeStamp, boolean isSpoken, boolean requireUserResponse){
        this.sender = sender;
        this.content = content;
        this.timeStamp = timeStamp;
        this.isSpoken = isSpoken;
        this.requireUserResponse = requireUserResponse;
    }

    private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException {
        this.content = content.toString();
        out.defaultWriteObject();
    }

}
