package edu.cmu.hcii.sugilite.sovite.communication;

import java.util.List;
import java.util.Map;

/**
 * @author toby
 * @date 2/26/20
 * @time 10:52 AM
 */
public class SoviteAppResolutionResultPacket {
    private String dataset;
    private String type;
    private List<String> available_apps;
    private Map<String, List<String>> result_map;
    private String query_type;

    public String getDataset() {
        return dataset;
    }

    public String getType() {
        return type;
    }

    public List<String> getAvailable_apps() {
        return available_apps;
    }

    public Map<String, List<String>> getResult_map() {
        return result_map;
    }

    public String getQuery_type() {
        return query_type;
    }
}
