package edu.cmu.hcii.sugilite.sovite.visual.text_selection;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;


/**
 * @author toby
 * @date 4/5/20
 * @time 3:45 PM
 */
public class SoviteTextSelectionView extends AppCompatTextView {
    private Spannable spannable;
    private String currentTextString;
    private SoviteSelectionChangedListener soviteSelectionChangedListener;

    public SoviteTextSelectionView(Context context) {
        super(context);
        this.setGravity(Gravity.LEFT | Gravity.TOP);
        this.setUserUtterance("", 0, 0);
        this.setTextIsSelectable(true);
    }

    public void setUserUtterance(String text, int startIndex, int endIndex) {
        this.currentTextString = text;
        this.spannable = new SpannableString(currentTextString);
        this.spannable.setSpan(new AbsoluteSizeSpan(25, true), 0, currentTextString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        this.postDelayed(new Runnable() {
            @Override
            public void run() {
                Selection.setSelection((Spannable) getText(), startIndex, endIndex);
            }
        }, 50);
        this.setText(spannable);
        initActionMenu();
    }

    public void setSoviteSelectionChangedListener(SoviteSelectionChangedListener soviteSelectionChangedListener) {
        this.soviteSelectionChangedListener = soviteSelectionChangedListener;
    }


    private void initActionMenu() {
        this.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                if (soviteSelectionChangedListener != null) {
                    soviteSelectionChangedListener.onSelectionChanged(getSelectionStart(), getSelectionEnd());
                }
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                menu.clear();
                if (soviteSelectionChangedListener != null) {
                    soviteSelectionChangedListener.onSelectionChanged(getSelectionStart(), getSelectionEnd());
                }
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                if (soviteSelectionChangedListener != null) {
                    soviteSelectionChangedListener.onSelectionChanged(0, 0);
                }
            }
        });
    }


    public interface SoviteSelectionChangedListener {
        void onSelectionChanged(int startIndex, int endIndex);
    }
}
