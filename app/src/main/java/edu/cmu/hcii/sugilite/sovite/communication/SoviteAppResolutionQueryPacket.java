package edu.cmu.hcii.sugilite.sovite.communication;

import java.util.List;

import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;

/**
 * @author toby
 * @date 2/26/20
 * @time 10:33 AM
 */
public class SoviteAppResolutionQueryPacket {
    private String dataset;
    private List<String> texts;
    private List<String> available_apps;
    private String query_type;
    private String package_name;
    private String activity_name;
    private SerializableUISnapshot ui_snapshot;


    public SoviteAppResolutionQueryPacket(String query_type) {
        this.query_type = query_type;
    }

    public SoviteAppResolutionQueryPacket(String dataset, String query_type, List<String> texts, List<String> available_apps) {
        this.dataset = dataset;
        this.query_type = query_type;
        this.texts = texts;
        this.available_apps = available_apps;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public void setAvailable_apps(List<String> available_apps) {
        this.available_apps = available_apps;
    }

    public void setTexts(List<String> texts) {
        this.texts = texts;
    }

    public void setQuery_type(String query_type) {
        this.query_type = query_type;
    }

    public List<String> getAvailable_apps() {
        return available_apps;
    }

    public List<String> getTexts() {
        return texts;
    }

    public String getDataset() {
        return dataset;
    }

    public String getQuery_type() {
        return query_type;
    }

    public void setPackage_name(String package_name) {
        this.package_name = package_name;
    }

    public void setActivity_name(String activity_name) {
        this.activity_name = activity_name;
    }

    public void setUi_snapshot(SerializableUISnapshot ui_snapshot) {
        this.ui_snapshot = ui_snapshot;
    }
}
