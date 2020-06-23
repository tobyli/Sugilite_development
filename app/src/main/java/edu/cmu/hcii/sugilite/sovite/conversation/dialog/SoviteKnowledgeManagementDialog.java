package edu.cmu.hcii.sugilite.sovite.conversation.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dao.PumiceKnowledgeDao;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceBooleanExpKnowledge;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceValueQueryKnowledge;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 3/10/20
 * @time 12:59 AM
 */
public class SoviteKnowledgeManagementDialog {

    private Context context;
    private PumiceKnowledgeManager pumiceKnowledgeManager;
    private LayoutInflater layoutInflater;
    private PumiceKnowledgeDao pumiceKnowledgeDao;
    private PumiceDialogManager pumiceDialogManager;
    private SugiliteData sugiliteData;

    private Dialog soviteKnowledgeManagementDialog;
    private View dialogView;
    private ListView proceduralKnowledgeListView;
    private ListView booleanKnowledgeListView;
    private ListView valueKnowledgeListView;

    public SoviteKnowledgeManagementDialog(Context context, PumiceKnowledgeManager pumiceKnowledgeManager, PumiceDialogManager pumiceDialogManager, SugiliteData sugiliteData) {
        this.context = context;
        this.pumiceKnowledgeManager = pumiceKnowledgeManager;
        this.pumiceDialogManager = pumiceDialogManager;
        this.layoutInflater = LayoutInflater.from(context);
        this.pumiceKnowledgeDao = new PumiceKnowledgeDao(context, sugiliteData);
        this.sugiliteData = sugiliteData;

        this.dialogView = layoutInflater.inflate(R.layout.dialog_knowledge_management, null);
        this.proceduralKnowledgeListView = dialogView.findViewById(R.id.listview_procedural_knowledge);
        this.booleanKnowledgeListView = dialogView.findViewById(R.id.listview_boolean_knowledge);
        this.valueKnowledgeListView = dialogView.findViewById(R.id.listview_value_knowledge);
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);


        //init the procedural knowledge ListView
        List<PumiceProceduralKnowledge> pumiceProceduralKnowledges = pumiceKnowledgeManager.getPumiceProceduralKnowledges();
        String[] proceduralKnowledgeLabelArray = new String[pumiceProceduralKnowledges.size()];
        int i = 0;
        for (PumiceProceduralKnowledge pumiceProceduralKnowledge : pumiceProceduralKnowledges) {
            proceduralKnowledgeLabelArray[i++] = pumiceProceduralKnowledge.getProcedureDescription(pumiceKnowledgeManager, true);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.simple_list_item_with_icon, proceduralKnowledgeLabelArray) {
            //override the arrayadapter to show HTML-styled textviews in the listview
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;
                if (convertView == null) {
                    row = layoutInflater.inflate(R.layout.simple_list_item_with_icon, null);
                } else {
                    row = convertView;
                }
                String text = getItem(position);
                TextView tv1 = (TextView) row.findViewById(android.R.id.text1);
                tv1.setText(Html.fromHtml(text));
                ImageView iv1 = row.findViewById(android.R.id.icon1);
                if (iv1 != null) {
                    iv1.setImageDrawable(context.getDrawable(R.mipmap.ic_delete_red));
                }
                tv1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PumiceProceduralKnowledge targetKnowledge = pumiceProceduralKnowledges.get(position);
                        SoviteProcedureKnowledgeConfigureDialog soviteProcedureKnowledgeConfigureDialog = new SoviteProcedureKnowledgeConfigureDialog(context, targetKnowledge, pumiceKnowledgeManager, pumiceDialogManager, sugiliteData);
                        soviteProcedureKnowledgeConfigureDialog.show();
                    }
                });

                iv1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PumiceProceduralKnowledge targetKnowledge = pumiceProceduralKnowledges.get(position);
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage("Are you sure that you want to delete the knowledge: " + proceduralKnowledgeLabelArray[position] + "?");
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    pumiceKnowledgeManager.removePumiceProceduralKnowledgeByName(targetKnowledge.getProcedureName());
                                    pumiceKnowledgeDao.savePumiceKnowledge(pumiceKnowledgeManager);
                                    dismiss();
                                    SoviteKnowledgeManagementDialog newSoviteKnowledgeManagementDialog = new SoviteKnowledgeManagementDialog(context, pumiceKnowledgeManager, pumiceDialogManager, sugiliteData);
                                    newSoviteKnowledgeManagementDialog.show();
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
                //textViews.put(tv1, ontologyQueryArray[position]);
                return row;
            }

        };
        proceduralKnowledgeListView.setAdapter(adapter);


        //init the boolean knowledge ListView
        List<PumiceBooleanExpKnowledge> pumiceBooleanExpKnowledges = pumiceKnowledgeManager.getPumiceBooleanExpKnowledges();
        String[] booleanKnowledgeLabelArray = new String[pumiceBooleanExpKnowledges.size()];
        int j = 0;
        for (PumiceBooleanExpKnowledge pumiceBooleanExpKnowledge : pumiceBooleanExpKnowledges) {
            booleanKnowledgeLabelArray[j++] = pumiceBooleanExpKnowledge.getBooleanDescription();
        }
        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(context, R.layout.simple_list_item_with_icon, booleanKnowledgeLabelArray) {
            //override the arrayadapter to show HTML-styled textviews in the listview
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;
                if (convertView == null) {
                    row = layoutInflater.inflate(R.layout.simple_list_item_with_icon, null);
                } else {
                    row = convertView;
                }
                String text = getItem(position);
                TextView tv1 = (TextView) row.findViewById(android.R.id.text1);
                tv1.setText(Html.fromHtml(text));
                ImageView iv1 = row.findViewById(android.R.id.icon1);
                if (iv1 != null) {
                    iv1.setImageDrawable(context.getDrawable(R.mipmap.ic_delete_red));
                }
                iv1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PumiceBooleanExpKnowledge targetKnowledge = pumiceBooleanExpKnowledges.get(position);
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage("Are you sure that you want to delete the knowledge: " + booleanKnowledgeLabelArray[position] + "?");
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    pumiceKnowledgeManager.removePumiceBooleanExpKnowledgesByName(targetKnowledge.getExpName());
                                    pumiceKnowledgeDao.savePumiceKnowledge(pumiceKnowledgeManager);
                                    dismiss();
                                    SoviteKnowledgeManagementDialog newSoviteKnowledgeManagementDialog = new SoviteKnowledgeManagementDialog(context, pumiceKnowledgeManager, pumiceDialogManager, sugiliteData);
                                    newSoviteKnowledgeManagementDialog.show();
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
                //textViews.put(tv1, ontologyQueryArray[position]);
                return row;
            }

        };
        booleanKnowledgeListView.setAdapter(adapter1);


        //init the value query knowledge ListView
        List<PumiceValueQueryKnowledge> pumiceValueQueryKnowledges = pumiceKnowledgeManager.getPumiceValueQueryKnowledges();
        String[] valueKnowledgeLabelArray = new String[pumiceValueQueryKnowledges.size()];
        int k = 0;
        for (PumiceValueQueryKnowledge pumiceValueQueryKnowledge : pumiceValueQueryKnowledges) {
            valueKnowledgeLabelArray[k++] = pumiceValueQueryKnowledge.getValueDescription();
        }
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(context, R.layout.simple_list_item_with_icon, valueKnowledgeLabelArray) {
            //override the arrayadapter to show HTML-styled textviews in the listview
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;
                if (convertView == null) {
                    row = layoutInflater.inflate(R.layout.simple_list_item_with_icon, null);
                } else {
                    row = convertView;
                }
                String text = getItem(position);
                TextView tv1 = (TextView) row.findViewById(android.R.id.text1);
                tv1.setText(Html.fromHtml(text));
                ImageView iv1 = row.findViewById(android.R.id.icon1);
                if (iv1 != null) {
                    iv1.setImageDrawable(context.getDrawable(R.mipmap.ic_delete_red));
                }
                iv1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PumiceValueQueryKnowledge targetKnowledge = pumiceValueQueryKnowledges.get(position);
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage("Are you sure that you want to delete the knowledge: " + valueKnowledgeLabelArray[position] + "?");
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    pumiceKnowledgeManager.removepumiceValueQueryKnowledgeByName(targetKnowledge.getValueName());
                                    pumiceKnowledgeDao.savePumiceKnowledge(pumiceKnowledgeManager);
                                    dismiss();
                                    SoviteKnowledgeManagementDialog newSoviteKnowledgeManagementDialog = new SoviteKnowledgeManagementDialog(context, pumiceKnowledgeManager, pumiceDialogManager, sugiliteData);
                                    newSoviteKnowledgeManagementDialog.show();
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
                //textViews.put(tv1, ontologyQueryArray[position]);
                return row;
            }

        };
        valueKnowledgeListView.setAdapter(adapter2);



        builder.setView(dialogView);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
        builder.setNegativeButton("Clear All Knowledge", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Are you sure that you want to delete all knowledge?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            pumiceDialogManager.clearPumiceKnowledgeAndSaveToDao();
                            pumiceDialogManager.getPumiceKnowledgeManager().initWithBuiltInKnowledge();
                            pumiceDialogManager.savePumiceKnowledgeToDao();
                            dismiss();
                            //SoviteKnowledgeManagementDialog newSoviteKnowledgeManagementDialog = new SoviteKnowledgeManagementDialog(context, pumiceKnowledgeManager, pumiceDialogManager, sugiliteData);
                            //newSoviteKnowledgeManagementDialog.show();
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
        soviteKnowledgeManagementDialog = builder.create();
    }

    public void show() {
        initDialog();
        if(soviteKnowledgeManagementDialog != null) {
            if (soviteKnowledgeManagementDialog.getWindow() != null) {
                soviteKnowledgeManagementDialog.getWindow().setType(OVERLAY_TYPE);
            }
            soviteKnowledgeManagementDialog.show();
        }
    }

    public void dismiss() {
        if (soviteKnowledgeManagementDialog != null && soviteKnowledgeManagementDialog.isShowing()) {
            soviteKnowledgeManagementDialog.dismiss();
        }
    }
}
