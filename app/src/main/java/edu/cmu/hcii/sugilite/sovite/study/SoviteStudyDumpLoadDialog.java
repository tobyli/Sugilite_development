package edu.cmu.hcii.sugilite.sovite.study;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.sovite.conversation.dialog.SoviteItemSelectionDialog;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 3/9/20
 * @time 5:53 PM
 */
public class SoviteStudyDumpLoadDialog extends SoviteItemSelectionDialog{

    private SoviteStudyDumpManager soviteStudyDumpManager;

    public SoviteStudyDumpLoadDialog(Activity context, SugiliteData sugiliteData, PumiceDialogManager pumiceDialogManager) {
        super(context, pumiceDialogManager);
        this.soviteStudyDumpManager = new SoviteStudyDumpManager(context, sugiliteData, pumiceDialogManager);
    }

    @Override
    public void show() {
        SugiliteProgressDialog progressDialog = new SugiliteProgressDialog(SugiliteData.getAppContext(), R.string.loading_sovite_dump_message);
        progressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<SoviteStudyDumpPacket> soviteStudyDumpPackets = soviteStudyDumpManager.getAllStoredSoviteStudyDumpPacket();
                if (soviteStudyDumpPackets.size() == 0) {
                    progressDialog.dismiss();
                    PumiceDemonstrationUtil.showSugiliteAlertDialog("No Sovite dump file available!");
                    return;
                }
                List<Pair<String, Runnable>> listItemLabelRunnableList = new ArrayList<>();
                Map<String, Runnable> listItemLabelIconRunnableMap = new HashMap<>();
                for (SoviteStudyDumpPacket soviteStudyDumpPacket : soviteStudyDumpPackets) {
                    listItemLabelRunnableList.add(new Pair<>(soviteStudyDumpPacket.getName(), new Runnable() {
                        @Override
                        public void run() {
                            dismiss();
                            try {
                                soviteStudyDumpManager.loadDump(soviteStudyDumpPacket);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    }));
                    listItemLabelIconRunnableMap.put(soviteStudyDumpPacket.getName(), new Runnable() {
                        @Override
                        public void run() {
                            String fileName = soviteStudyDumpPacket.getName();
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setMessage("Are you sure that you want to delete the dump: " + fileName + "?");
                            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        soviteStudyDumpManager.deleteSoviteStudyDumpPacketFromFile(fileName);
                                        soviteItemSelectionDialog.dismiss();
                                        show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            AlertDialog deleteDialog = builder.create();
                            if (deleteDialog != null) {
                                if (deleteDialog.getWindow() != null) {
                                    deleteDialog.getWindow().setType(OVERLAY_TYPE);
                                }
                                deleteDialog.show();
                            }
                        }
                    });
                }
                progressDialog.dismiss();
                SugiliteData.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initDialog(listItemLabelRunnableList, context.getDrawable(R.mipmap.ic_delete_red), listItemLabelIconRunnableMap,  "Select which dump to load", null, false);
                        if(soviteItemSelectionDialog != null) {
                            if (soviteItemSelectionDialog.getWindow() != null) {
                                soviteItemSelectionDialog.getWindow().setType(OVERLAY_TYPE);
                            }
                            soviteItemSelectionDialog.show();
                        }
                        //initiate the dialog manager when the dialog is shown
                        initDialogManager();
                    }
                });
            }
        }).start();

    }



}
