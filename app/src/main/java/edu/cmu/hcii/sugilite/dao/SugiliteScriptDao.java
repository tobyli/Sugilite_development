package edu.cmu.hcii.sugilite.dao;

import java.util.List;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
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
     * write the changes to the disk
     * @throws Exception
     */
    void commitSave() throws Exception;

    /**
     * @return the number of total scripts
     */
    int size() throws Exception;

    /**
     * return the script with name key
     * @param key
     * @return the result script
     */
    SugiliteStartingBlock read(String key) throws Exception;

    /**
     * delete the script with name key
     * @param key
     * @return the number of scripts deleted
     */
    int delete(String key) throws Exception;

    /**
     * delete all scripts
     * @return the number of scripts deleted
     */
    int clear() throws Exception;

    /**
     *
     * @return the names of all scripts
     */
    List<String> getAllNames() throws Exception;

    /**
     *
     * @return all scripts
     */
    List<SugiliteStartingBlock> getAllScripts() throws Exception;

    /**
     *
     * @return get the next available default name (Untitled Script N)
     */
    String getNextAvailableDefaultName();


}
