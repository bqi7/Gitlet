package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

/**
 * @author Marco Lyu
 * Content: DataPool is a database for all the files committed.
 *          It avoids the problem of redundancy (same files stored twice.),
 *          and it keeps users from retrieving serialized files. which should
 *          be inaccessible to them and modifying these files
 */

public class DataPool implements Serializable {
    private static DataPool data = null;
    private HashMap<String, File> blobs = new HashMap<String, File>();

    /* Only one dataPool can be created */
    private DataPool() { }

    /* Returns a HashMap containing all the commits */
    public static DataPool getPool() {
        if (data == null) {
            data = new DataPool();
        }
        return data;
    }

    /* Puts an entry of a fileId and a file into the HashMap */
    public void put(String fileId, File outFile) {
        blobs.put(fileId, outFile);
    }

    /* Retrieves the file according to its fileId */
    public File get(String fileId) {
        return blobs.get(fileId);
    }

    /* Checks whether a fileId already exists (avoid redundancy) */
    public boolean containsKey(String fileID) {
        return blobs.containsKey(fileID);
    }

    /* Returns the size of the current database */
    public int size() {
        return blobs.size();
    }
}
