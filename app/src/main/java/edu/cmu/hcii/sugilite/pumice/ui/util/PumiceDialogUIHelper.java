package edu.cmu.hcii.sugilite.pumice.ui.util;

import android.content.Context;
import android.graphics.Color;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daasuu.bl.ArrowDirection;
import com.daasuu.bl.BubbleLayout;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceUtterance;

/**
 * @author toby
 * @date 10/24/18
 * @time 12:52 PM
 */
public class PumiceDialogUIHelper {
    private Context context;

    public PumiceDialogUIHelper(Context context) {
        this.context = context;
    }

    int convertDpToPixel(int dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
    public View getDialogLayout(PumiceUtterance utterance) {
        return getDialogLayout(getBubbleLayout(utterance), utterance.getSender());
    }

    public View getDialogLayout(View contentView, PumiceDialogManager.Sender sender) {
        RelativeLayout relativeLayout = new RelativeLayout(context);
        //linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        ImageView avatarImageView = new ImageView(context);
        avatarImageView.setMaxWidth(convertDpToPixel(48));
        avatarImageView.setMaxHeight(convertDpToPixel(48));
        avatarImageView.setLayoutParams(new LinearLayout.LayoutParams(convertDpToPixel(48), convertDpToPixel(48)));
        avatarImageView.setAdjustViewBounds(true);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(convertDpToPixel(8), convertDpToPixel(8), convertDpToPixel(8), convertDpToPixel(8));

        //set the gravity of text based on the sender
        if (sender == PumiceDialogManager.Sender.AGENT) {
            avatarImageView.setPadding(convertDpToPixel(0), convertDpToPixel(0), convertDpToPixel(8), convertDpToPixel(0));
            avatarImageView.setImageDrawable(context.getResources().getDrawable(R.mipmap.bot_avatar));//SHOULD BE R.mipmap.bot_avatar
            relativeLayout.addView(avatarImageView, 0);
            relativeLayout.addView(contentView, 1);
            avatarImageView.setId(View.generateViewId());
            contentView.setId(View.generateViewId());

            RelativeLayout.LayoutParams avatarImageParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            avatarImageParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            avatarImageView.setLayoutParams(avatarImageParams);

            RelativeLayout.LayoutParams contentParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            contentParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            contentParams.addRule(RelativeLayout.RIGHT_OF, avatarImageView.getId());
            contentView.setLayoutParams(contentParams);
            contentView.setForegroundGravity(Gravity.TOP | Gravity.LEFT);




        } else if (sender == PumiceDialogManager.Sender.USER) {
            avatarImageView.setPadding(convertDpToPixel(8), convertDpToPixel(0), convertDpToPixel(0), convertDpToPixel(0));
            avatarImageView.setImageDrawable(context.getResources().getDrawable(R.mipmap.user_avatar));
            relativeLayout.addView(avatarImageView, 0);
            relativeLayout.addView(contentView, 1);
            avatarImageView.setId(View.generateViewId());
            contentView.setId(View.generateViewId());

            RelativeLayout.LayoutParams avatarImageParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            avatarImageParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            avatarImageView.setLayoutParams(avatarImageParams);

            RelativeLayout.LayoutParams contentParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            contentParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            contentParams.addRule(RelativeLayout.LEFT_OF, avatarImageView.getId());
            contentView.setLayoutParams(contentParams);
            contentView.setForegroundGravity(Gravity.TOP | Gravity.LEFT);

        }
        relativeLayout.setLayoutParams(params);


        return relativeLayout;
    }



    public View getBubbleLayout(PumiceUtterance utterance) {
        TextView textView = new TextView(context);
        textView.setText(utterance.getContent());
        textView.setTextSize(14);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        if (utterance.getSender() == PumiceDialogManager.Sender.AGENT) {
            textView.setTextColor(Color.BLACK);
        } else if (utterance.getSender() == PumiceDialogManager.Sender.USER) {
            textView.setTextColor(Color.WHITE);
        }
        return getBubbleLayout(textView, utterance.getSender());
    }


    public View getBubbleLayout(View contentView, PumiceDialogManager.Sender sender) {

        BubbleLayout bubbleLayout = new BubbleLayout(context);

        bubbleLayout.addView(contentView);

        bubbleLayout.setPadding(convertDpToPixel(16), convertDpToPixel(12), convertDpToPixel(16), convertDpToPixel(12));
        bubbleLayout.setArrowHeight(convertDpToPixel(8));
        bubbleLayout.setArrowPosition(convertDpToPixel(16));
        bubbleLayout.setArrowWidth(convertDpToPixel(8));
        bubbleLayout.setStrokeWidth(convertDpToPixel(0));
        bubbleLayout.setStrokeColor(Color.LTGRAY);
        bubbleLayout.setCornersRadius(convertDpToPixel(8));

        //set the gravity of text based on the sender
        if (sender == PumiceDialogManager.Sender.AGENT) {
            bubbleLayout.setArrowDirection(ArrowDirection.LEFT);
            bubbleLayout.setBubbleColor(Color.parseColor("#ECF1F1"));
        } else if (sender == PumiceDialogManager.Sender.USER) {
            bubbleLayout.setArrowDirection(ArrowDirection.RIGHT);
            bubbleLayout.setBubbleColor(Color.parseColor("#61C9A6"));
        }
        return bubbleLayout;
    }

}
