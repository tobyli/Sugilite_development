package edu.cmu.hcii.sugilite.model.operation.unary;


import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.sovite.SoviteAppNameAppInfoManager;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 3/15/20
 * @time 2:13 PM
 */
public class SugiliteLaunchAppOperation extends SugiliteUnaryOperation<String> {
    private String appPackageName;
    public SugiliteLaunchAppOperation() {
        super();
        this.setOperationType(LAUNCH_APP);
    }

    public SugiliteLaunchAppOperation(String appPackageName) {
        this();
        setParameter0(appPackageName);
    }

    @Override
    public String getPumiceUserReadableDecription() {
        SoviteAppNameAppInfoManager soviteAppNameAppInfoManager = SoviteAppNameAppInfoManager.getInstance(SugiliteData.getAppContext());
        return String.format("launch the app %s", soviteAppNameAppInfoManager.getReadableAppNameForPackageName(appPackageName));
    }

    @Override
    public void setParameter0(String appPackageName) {
        this.setAppPackageName(appPackageName);
    }

    public void setAppPackageName(String appPackageName) {
        this.appPackageName = appPackageName;
    }

    @Override
    public String getParameter0() {
        return appPackageName;
    }

    public String getAppPackageName() {
        return appPackageName;
    }

    @Override
    public boolean containsDataDescriptionQuery() {
        return false;
    }

    @Override
    public OntologyQuery getDataDescriptionQueryIfAvailable() {
        return null;
    }

    @Override
    public String toString() {
        return "(" + "call launch_app " + addQuoteToTokenIfNeeded(getParameter0().toString()) + ")";
    }


}
