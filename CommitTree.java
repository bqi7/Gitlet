package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class CommitTree implements Serializable {
    /*  a data structure that holds CommitNodes objects.
    A headPointer points at the current CommitNode in question
    */

    private static final String LOG_FORMAT = "===\nCommit %s\n%s\n%s\n\n";

    /**
     * currPtr: the pointer pointing to the foremost node of the branch it belongs
     * BranchPointers: a map that contains all the pointers, including the above one.
     *                 It's worth noting that BranchPointers use the name of
     *                 the branch instead of the commitID to retrieve the commitNode.
     * nodeIds: the database for CommitNodes.
     */

    CommitNode currPtr;
    String currBranch;
    /* branchPointers maps the name of the branch
        to the commitId of the foremost node of the branch */
    HashMap<String, String> branchPointers = new HashMap<>();
    ArrayList<String> nodeIds = new ArrayList<>();

    public CommitTree() {
        CommitNode initial = new CommitNode("initial commit");
        CommitNode masterBranchPtr = initial;
        branchPointers.put("master", initial.commitID);
        currBranch = "master";
        currPtr = initial;
        addCommitNode(initial);
        File outFile = new File(GitLet.REPO + "/objects/" + initial.commitID);
        FileUtil.serialize(outFile, initial);
    }

    /* getCommitNode retrieves the commitNode corresponding to the commitId passed in */

    public CommitNode getCommitNode(String commitId) {
        File inFile = new File(GitLet.REPO + "/objects/" + commitId);
        CommitNode commitNode = (CommitNode) FileUtil.deSerialize(inFile);
        return commitNode;
    }

    public void addCommitNode(CommitNode newNode) {
        nodeIds.add(newNode.commitID);
        currPtr = newNode;
        branchPointers.put(currBranch, newNode.commitID);
    }
//    write content

    public void addBranch(String name) {
        if (branchPointers.containsKey(name)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        branchPointers.put(name, currPtr.commitID);
    }


    public void rmBranch(String name) {
        if (!branchPointers.containsKey(name)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (name.equals(currBranch)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        branchPointers.remove(name);
    }

    public void find(String message) {
        boolean found = false;
        for (String nodeId : nodeIds) {
            CommitNode node = getCommitNode(nodeId);
            if (node.message.equals(message)) {
                System.out.println(node.commitID);
                found = true;
            }
        }

        if (!found) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    public String log() {
        CommitNode node = currPtr;
        String result = "";
        while (node != null) {
            result += String.format(LOG_FORMAT, node.commitID, node.timeStamp, node.message);
            if (node.parentName != null) {
                node = getCommitNode(node.parentName);
            } else {
                node = null;
            }
        }
        return result;
    }

    public String globalLog() {
        String result = "";
        for (int i = nodeIds.size() - 1; i >= 0; --i) {
            CommitNode node = getCommitNode(nodeIds.get(i));
            result += String.format(LOG_FORMAT, node.commitID, node.timeStamp, node.message);
        }
        return result;
    }

    public void checkoutBranch(String name) {
        File inFile2 = new File(GitLet.REPO + "/stagingArea");
        StagingArea stage = (StagingArea) FileUtil.deSerialize(inFile2);
        File workingDir = new File(System.getProperty("user.dir"));
        if (!branchPointers.containsKey(name)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        if (name.equals(currBranch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        CommitNode branchCommit = getCommitNode(branchPointers.get(name));
        for (File file : workingDir.listFiles()) {
            if (!currPtr.fileNames.containsKey(file.getName())
                    && branchCommit.fileNames.containsKey(file.getName())) {
                System.out.println("There is an untracked file "
                        + "in the way; delete it or add it first.");
                System.exit(0);
            }
            if (!branchCommit.fileNames.containsKey(file.getName())) {
                file.delete();
            }
        }
        for (String filename : branchCommit.fileNames.keySet()) {
            String fileId = branchCommit.fileNames.get(filename);
            CommitNode.Blob commitFile = branchCommit.getBlob(fileId);
            File userFile = new File(filename);
            if (userFile.exists()) {
                Utils.writeContents(userFile, commitFile.content);
            } else {
                try {
                    File f = new File(filename);
                    f.createNewFile();
                    Utils.writeContents(f, commitFile.content);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        stage.clearAdd();
        stage.getRemovedFiles().clear();
        FileUtil.serialize(inFile2, stage);
        currBranch = name;
        currPtr = branchCommit;
    }

    public void checkoutFile(String filename) {
        if (!currPtr.fileNames.containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String fileId = currPtr.fileNames.get(filename);
        CommitNode.Blob commitFile = currPtr.getBlob(fileId);
        File userFile = new File(filename);
        Utils.writeContents(userFile, commitFile.content);
    }

    public void checkoutFile(String filename, String commitId) {
        if (commitId.length() < 40) {
            for (String hashID: nodeIds) {
                if (hashID.contains(commitId)) {
                    commitId = hashID;
                }
            }
        }
        if (!nodeIds.contains(commitId)) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        CommitNode commit = getCommitNode(commitId);
        if (!commit.fileNames.containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String fileId = commit.fileNames.get(filename);
        CommitNode.Blob commitFile = commit.getBlob(fileId);
        File userFile = new File(filename);
        Utils.writeContents(userFile, commitFile.content);
    }

    public String findSplitNode(String branchName) {
        String givenBranchId = branchPointers.get(branchName);
        List<String> currBranchHistory = new ArrayList<>();
        HashSet<String> givenBranchHistory = new HashSet<>();
        CommitNode tempCurrPtr = currPtr;
        CommitNode tempGivenPtr = getCommitNode(givenBranchId);

        if (tempCurrPtr.commitID.equals(givenBranchId)) {
            return null;
        }

        currBranchHistory.add(0, branchPointers.get(currBranch));
        givenBranchHistory.add(givenBranchId);

        while (tempCurrPtr.parentName != null) {
            if (givenBranchId.equals(tempCurrPtr.parentName)) {
                return null;
            }
            currBranchHistory.add(tempCurrPtr.parentName);
            tempCurrPtr = getCommitNode(tempCurrPtr.parentName);
        }

        while (tempGivenPtr.parentName != null) {
            givenBranchHistory.add(tempGivenPtr.parentName);
            tempGivenPtr = getCommitNode(tempGivenPtr.parentName);
        }

        for (int i = 0; i < currBranchHistory.size(); ++i) {
            if (givenBranchHistory.contains(currBranchHistory.get(i))) {
                return currBranchHistory.get(i);
            }
        }
        return null;
    }
}
