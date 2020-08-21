package edu.cmu.hcii.sugilite.sovite.screen2vec;

import java.io.Serializable;
import java.util.List;

/**
 * @author toby
 * @date 8/18/20
 * @time 10:12 PM
 */
public class RicoActivity implements Serializable {
    RicoNode root;

    //not used
    @Deprecated
    List<RicoActivity> added_fragments;
    @Deprecated
    List<RicoActivity> active_fragments;

    public RicoActivity (RicoNode rootNode) {
        this.root = rootNode;
    }

}
