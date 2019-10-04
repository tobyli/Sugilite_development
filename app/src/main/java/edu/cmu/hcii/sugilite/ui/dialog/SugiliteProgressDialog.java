package edu.cmu.hcii.sugilite.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 9/25/19
 * @time 3:03 PM
 */
public class SugiliteProgressDialog {
    private AlertDialog dialog;

    public SugiliteProgressDialog(Context context, String message) {
        SugiliteData.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                LayoutInflater inflater = LayoutInflater.from(context);
                View dialogView = inflater.inflate(R.layout.dialog_progress, null);
                TextView messageTextView = dialogView.findViewById(R.id.progress_message);
                messageTextView.setText(message);
                builder.setView(dialogView);
                dialog = builder.create();
                dialog.getWindow().setType(OVERLAY_TYPE);
                dialog.setCanceledOnTouchOutside(false);
            }
        });
    }

    public SugiliteProgressDialog(Context context, int resId) {
        this(context, context.getString(resId));
    }

    /**
     * show the dialog using the main UI thread
     */
    public void show() {
        SugiliteData.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        });
    }
    /**
     * dismiss the dialog using the main UI thread
     */
    public void dismiss() {
        SugiliteData.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        });
    }

    public boolean isShowing() {
        return dialog.isShowing();
    }
}
