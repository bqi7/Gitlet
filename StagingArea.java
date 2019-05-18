package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class StagingArea implements Serializable {

    /* Stages files to be added or removed for later commits.
    Also stores a set corresponding to tracked and removed files */

    private static StagingArea stagingArea = null;
    private HashSet<String> removedFiles;
    public HashMap<String, CommitNode.Blob> stagedBlobs;

    public StagingArea() {
        removedFiles = new HashSet<>();
        stagedBlobs = new HashMap<>();
    }

    public static StagingArea getStagingArea() {
        if (stagingArea == null) {
            stagingArea = new StagingArea();
        }
        return stagingArea;
    }

    public Set<String> getAddedFiles() {
        return stagedBlobs.keySet();
    }

    public HashSet<String> getRemovedFiles() {
        return removedFiles;
    }

    public void addFile(String fileName) {
        if (removedFiles.contains(fileName)) {
            removedFiles.remove(fileName);
        }
        CommitNode.Blob stagedBlob = new CommitNode.Blob(fileName);
        stagedBlobs.put(fileName, stagedBlob);
    }

    public void rm(String filename) {
        File infile = new File(GitLet.REPO + "/commitTree");
        CommitTree tree = (CommitTree) FileUtil.deSerialize(infile);
        if (!tree.currPtr.fileNames.containsKey(filename)
                && !stagedBlobs.keySet().contains(filename)) {
            System.out.println("No reason to remove the file");
            System.exit(0);
        }
        if (tree.currPtr.fileNames.containsKey(filename)) {
            /* write code that actually DELETES the file from disk */
            removedFiles.add(filename);
            File file = new File(filename);
            Utils.restrictedDelete(filename);
        }
        if (stagedBlobs.keySet().contains(filename)) {
            stagedBlobs.remove(filename);
        }
    }


    public void commit(String commitMessage) {
        File file = new File(GitLet.REPO + "/commitTree");
        CommitTree tree = (CommitTree) FileUtil.deSerialize(file);
        CommitNode newNode = new CommitNode(commitMessage);

        newNode.blobs = tree.currPtr.blobs;

        newNode.parentName = tree.currPtr.commitID;
        newNode.fileNames = tree.currPtr.fileNames;
        for (String removed : removedFiles) {
            newNode.fileNames.remove(removed);
        }
        //  changed here ^
        for (String filename : stagedBlobs.keySet()) {
            newNode.addBlob(stagedBlobs.get(filename));
        }
        removedFiles.clear();

//        newNode.trackedFiles = trackedFiles;
        stagedBlobs.clear();
        tree.addCommitNode(newNode);
        FileUtil.serialize(file, tree);
        File commit = new File(GitLet.REPO + "/objects/" + newNode.commitID);
        FileUtil.serialize(commit, newNode);
    }

    public int getRemovedSize() {
        return removedFiles.size();
    }
    public int getAddedSize() {
        return stagedBlobs.size();
    }

    public void clearAdd() {
        stagedBlobs.clear();
    }

    public static void main(String[] args) {
       StagingArea s =  new StagingArea();
       s.removedFiles.add("hi.txt");
       s.addFile("hi.txt");
       System.out.println(s.getRemovedSize());
    }
}
