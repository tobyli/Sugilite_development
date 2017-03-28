package edu.cmu.hcii.sugilite.dao;

import java.util.List;

import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

/**
 * Created by toby on 3/28/17.
 */

public interface SugiliteScriptDao {

    /**
     * save the sugiliteBlock
     * @param sugiliteBlock
     * @throws Exception
     */
    void save(SugiliteStartingBlock sugiliteBlock) throws Exception;

    /**
     * @return the number of total scripts
     */
    int size();

    /**
     * return the script with name key
     * @param key
     * @return the result script
     */
    SugiliteStartingBlock read(String key);

    /**
     * delete the script with name key
     * @param key
     * @return the number of scripts deleted
     */
    int delete(String key);

    /**
     * delete all scripts
     * @return the number of scripts deleted
     */
    int clear();

    /**
     *
     * @return the names of all scripts
     */
    List<String> getAllNames();

    /**
     *
     * @return all scripts
     */
    List<SugiliteStartingBlock> getAllScripts();

    /**
     *
     * @return get the next available default name (Untitled Script N)
     */
    String getNextAvailableDefaultName();


}
