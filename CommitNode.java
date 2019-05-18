package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Content: Each CommitNode is connected to a database (DataPool)
 *          used to store ALL the information. One can retrieve the
 *          file only through its hashCode (commitID). The only two
 *          functions available for users are addFile and getBlob.
 */

public class CommitNode implements Serializable {

    /**
     * parentName: parent's ID.
     * commitID: the hashCode of the current CommitNode
     * message: the message of the commit
     * timeStamp: creation time
     * blobs: a DataPool instance containing all the blobs,
     *        regardless of whether these blobs belong to this commit;
     * fileIds: the hashCode of all the files of the current commit.
     * fileNames: map filenames to fileIds;
     */

    String parentName;
    String commitID;
    String message;
    String timeStamp;
    DataPool blobs;
    //    HashSet<String> trackedFiles;
    HashMap<String, String> fileNames;

//    private static int hash = 0;
//    private static String hashS = Integer.toString(hash);

    /* Creates a commit Node */
    public CommitNode(String mes) {
        message = mes;
        timeStamp = convertDate(new Date());
        blobs = DataPool.getPool();
        fileNames = new HashMap<>();
        /* this should work practically since timestamp must be unique */
        commitID = Utils.sha1(parentName + message + timeStamp);
    }

    public String getHash() {

//        byte[] toBeHashed = FileUtil.changeToByteArr(this);
//        return Utils.sha1(toBeHashed);
        return "";
    }

    /* Adds a blob to current commit */
    public void addBlob(Blob blob) {
        String fileId = Utils.sha1(blob.content);
        blob.commitId = commitID;
        fileNames.put(blob.filename, fileId);
        if (!blobs.containsKey(fileId)) {
            File outFile = new File(GitLet.REPO + "/objects/" + fileId);
            FileUtil.serialize(outFile, blob);
            blobs.put(fileId, outFile);
        }
    }

    public String getFileId(String fileName) {
        return fileNames.get(fileName);
    }

    /* Adds a file to the current commit via addBlob method */
    public void addFile(String...files) {
        for (String file : files) {
            addBlob(new Blob(file));
        }
    }

    /* Retrieves the content of a Blob */
    public Blob getBlob(String fileId) {
        File inFile = blobs.get(fileId);
        return (Blob) FileUtil.deSerialize(inFile);
    }

    private String convertDate(Date date) {
        StringBuilder sb = new StringBuilder();
        sb.append("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdf = new SimpleDateFormat(sb.toString());
        return sdf.format(date);
    }

    static class Blob implements Serializable {
        String filename;
        String commitId;
        byte[] content;

        /* Creates a blob instance */
        Blob(String name) {
            filename = name;
            commitId = null;
            File f = new File(filename);
            content = Utils.readContents(f);
        }
    }
    public static void main(String[] args) {
        GitLet g = new GitLet();
        g.init();
        g.add("hi.txt");
        g.commit("whatsup");
        g.add("magic_word.txt");
        g.commit("whatsup");
    }
}
