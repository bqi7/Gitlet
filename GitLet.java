package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/* Performs the functionality of Main class. Avoids having the Main class become too clustered. */
public class GitLet {

    private static File GIT_DIR;
    private static File OBJ_DIR;
    private static String WORKING_DIR = System.getProperty("user.dir");
    public static final String REPO = ".gitlet";
    private static boolean merged = true;

    /* Creates a new gitlet version-control system in the current directory. */
    public void init() {
        GIT_DIR = new File(REPO);
        if (GIT_DIR.exists()) {
            System.out.println("A gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        }
        GIT_DIR.mkdir();

        OBJ_DIR = new File(REPO + "/objects");
        OBJ_DIR.mkdir();

        CommitTree tree = new CommitTree();
        File cTree = new File(REPO + "/commitTree");
        FileUtil.serialize(cTree, tree);

        StagingArea stage = StagingArea.getStagingArea();
        File sArea = new File(REPO + "/stagingArea");
        FileUtil.serialize(sArea, stage);

        DataPool data = DataPool.getPool();
        File dPool = new File(REPO + "/objects/dataPool");
        FileUtil.serialize(dPool, data);
    }

    public void commit(String commitMessage) {
        /* Saves a snapshot of certain files in the current commit
          and staging area so they can be restored at a later time,
          creating a new commit. */
        File inFile = new File(REPO + "/stagingArea");
        StagingArea stage = (StagingArea) FileUtil.deSerialize(inFile);
        if (commitMessage.length() == 0) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        } else if (stage.getAddedSize() == 0 && stage.getRemovedSize() == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        stage.commit(commitMessage);
        File sArea = new File(REPO + "/stagingArea");
        FileUtil.serialize(sArea, stage);
    }

    public void checkout(String[] commands) {
        File inFile = new File(REPO + "/commitTree");
        CommitTree tree = (CommitTree) FileUtil.deSerialize(inFile);

        if (commands.length == 2) {
            /* java gitlet.Main checkout [branch name] */
            tree.checkoutBranch(commands[1]);
        } else if (commands.length == 3) {
            /* java gitlet.Main checkout -- [file name] */
            tree.checkoutFile(commands[2]);
        } else if (commands.length == 4) {
            /* java gitlet.Main checkout [commit id] -- [file name] */
            tree.checkoutFile(commands[3], commands[1]);
        } else {
            /* Undefined method */
            System.out.println("Check git --help for help");
            System.exit(0);
        }
        FileUtil.serialize(inFile, tree);
    }

    public void reset(String commitID) {
//        may need to check on reset edge case. If rm file.
        File inTree = new File(REPO + "/commitTree");
        File inStage = new File(REPO + "/stagingArea");
        CommitTree tree = (CommitTree) FileUtil.deSerialize(inTree);
        StagingArea stage = (StagingArea) FileUtil.deSerialize(inStage);

        if (commitID.length() < 40) {
            for (String id: tree.nodeIds) {
                if (id.contains(commitID)) {
                    commitID = id;
                }
            }
        }

        if (!tree.nodeIds.contains(commitID)) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        CommitNode givenCommitNode = tree.getCommitNode(commitID);

        for (String filename : givenCommitNode.fileNames.keySet()) {
            String fileId = givenCommitNode.fileNames.get(filename);
            CommitNode.Blob commitFile = givenCommitNode.getBlob(fileId);
            File userFile = new File(filename);
            if (userFile.exists()) {
                if (!tree.currPtr.fileNames.containsKey(filename)) {
                    System.out.println("There is an untracked file "
                            + "in the way; delete it or add it first.");
                    System.exit(0);
                } else {
                    Utils.writeContents(userFile, commitFile.content);
                }
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

   /*     for (String trackedFile : trackedFiles) {
            if (!givenCommitNode.fileNames.keySet().contains(trackedFile)) {
                stage.rm(trackedFile);
            } else {
                File track = new File(trackedFile);
                Utils.writeContents(track,
                        givenCommitNode.getBlob(givenCommitNode.getFileId(trackedFile)).content);
            }
        }
*/
        tree.branchPointers.put(tree.currBranch, commitID);
        tree.currPtr = tree.getCommitNode(commitID);
        stage.clearAdd();
//        Do i clear removedFiles from stage too??

        File cTree = new File(REPO + "/commitTree");
        FileUtil.serialize(cTree, tree);
        File sArea = new File(REPO + "/stagingArea");
        FileUtil.serialize(sArea, stage);
    }

    public void status() {
        /* Displays what branches currently exist, and marks the current branch with a *.
           Also displays what files have been staged or marked for untracking. */
        File inFile = new File(REPO + "/commitTree");
        CommitTree tree = (CommitTree) FileUtil.deSerialize(inFile);
        File inFile2 = new File(REPO + "/stagingArea");
        StagingArea stage = (StagingArea) FileUtil.deSerialize(inFile2);
        statusHelper("Branches");
        String currBranchName = tree.currBranch;
        System.out.println("*" + currBranchName);
        for (String branchName : tree.branchPointers.keySet()) {
            if (!branchName.equals(currBranchName)) {
                System.out.println(branchName);
            }
        }
        System.out.println();
        statusHelper("Staged Files");
        for (String stagedFile : stage.getAddedFiles()) {
            System.out.println(stagedFile);
        }
        System.out.println();
        statusHelper("Removed Files");
        for (String removedFile : stage.getRemovedFiles()) {
            System.out.println(removedFile);
        }
        System.out.println();
        statusHelper("Modifications Not Staged For Commit");
        System.out.println();
        statusHelper("Untracked Files");
        System.out.println();
    }

    private static void statusHelper(String name) {
        System.out.println("=== " + name + " ===");
    }

    public void log() {
        File inFile = new File(REPO + "/commitTree");
        CommitTree tree = (CommitTree) FileUtil.deSerialize(inFile);
        System.out.println(tree.log());
    }

    public void globalLog() {
        File inFile = new File(REPO + "/commitTree");
        CommitTree tree = (CommitTree) FileUtil.deSerialize(inFile);
        String globalLog = tree.globalLog();
        System.out.println(globalLog);
    }

    public void find(String commitMessage) {
        File inFile = new File(REPO + "/commitTree");
        CommitTree tree = (CommitTree) FileUtil.deSerialize(inFile);
        tree.find(commitMessage);
    }

//    public void merge(String branchName) {
//        File inFile = new File(REPO + "/commitTree");
//        CommitTree tree = (CommitTree) FileUtil.deSerialize(inFile);
//        File inFileStage = new File(REPO + "/stagingArea");
//        StagingArea stage = (StagingArea) FileUtil.deSerialize(inFileStage);
//
//        if (!tree.branchPointers.containsKey(branchName)) {
//            System.out.println("A branch with that name does not exist.");
//            System.exit(0);
//        }
//
//        if ((stage.getAddedSize() > 0) || (stage.getRemovedSize()>0)) {
//            System.out.println("You have uncommitted changes");
//            System.exit(0);
//
//        }
//
//        if(tree.currBranch.equals(branchName)) {
//            System.out.println("Cannot merge a branch with itself");
//            System.exit(0);
//        }
//
//        String splitPoint = tree.findSplitNode(branchName);
//        Boolean merged = true;
//        if (splitPoint == null) {
//            System.out.println("Given branch is an ancestor of the current branch.");
//            System.exit(0);
//        }
//        if (splitPoint.equals(tree.branchPointers.get(tree.currBranch))) {
//            tree.branchPointers.put(tree.currBranch, tree.branchPointers.get(branchName));
//            String currPtrId = tree.branchPointers.get(tree.currBranch);
//            tree.currPtr = tree.getCommitNode(currPtrId);
//            System.out.println("Current branch fast-forwarded.");
//            File cTree = new File(REPO + "/commitTree");
//            FileUtil.serialize(cTree, tree);
//            System.exit(0);
//        }
//        CommitNode split = tree.getCommitNode(splitPoint);
//        CommitNode given = tree.getCommitNode(tree.branchPointers.get(branchName));
//        CommitNode curr = tree.getCommitNode(tree.branchPointers.get(tree.currBranch));
//        for (String file : split.fileNames.keySet()) {
//            byte[] splitFile = split.getBlob(split.fileNames.get(file)).content;
//            if (given.fileNames.containsKey(file) && curr.fileNames.containsKey(file)) {
//                byte[] currFile = curr.getBlob(curr.fileNames.get(file)).content;
//                byte[] givFile = given.getBlob(given.fileNames.get(file)).content;
//                if (currFile.equals(splitFile) && !givFile.equals(splitFile)) {
//                    tree.checkoutFile(file, given.commitID);
//                    add(file);
//                } else if (!currFile.equals(splitFile) && !givFile.equals(splitFile)
//                        && !currFile.equals(givFile)) {
//                    mergeHelper(file, currFile, givFile);
//                    merged = false;
//                }
//            } else if (!given.fileNames.containsKey(file) && curr.fileNames.containsKey(file)) {
//                byte[] currFile = curr.getBlob(curr.fileNames.get(file)).content;
////                if (currFile.equals(splitFile)) {
//                if(Arrays.equals(currFile, splitFile)) {
//                    rm(file);
//                } else if (!currFile.equals(splitFile)) {
//                    mergeHelper(file, currFile, new byte[0]);
//                    merged = false;
//                }
//            } else if (given.fileNames.containsKey(file) && !curr.fileNames.containsKey(file)) {
//                byte[] givFile = given.getBlob(given.fileNames.get(file)).content;
////                if (!givFile.equals(splitFile)) {
//                if (!Arrays.equals(givFile, splitFile)) {
//                    mergeHelper(file, new byte[0], givFile);
//                    merged = false;
//                }
//            }
//        }
//        for (String gfile : given.fileNames.keySet()) {
//            byte[] givFile = given.getBlob(given.fileNames.get(gfile)).content;
//            if (!split.fileNames.containsKey(gfile) && !curr.fileNames.containsKey(gfile)) {
//                tree.checkoutFile(gfile, given.commitID);
//                add(gfile);
//            } else if (!split.fileNames.containsKey(gfile)) {
//                byte[] currFile = curr.getBlob(curr.fileNames.get(gfile)).content;
//                if (!currFile.equals(givFile)) {
//                    mergeHelper(gfile, currFile, givFile);
//                    merged = false;
//                }
//            }
//        }
//        if (merged) {
//            commit("Merged " + tree.currBranch + " with" + branchName + ".");
//        } else {
//            System.out.println("Encountered a merge conflict.");
//        }
//        File cTree = new File(REPO + "/commitTree");
//        FileUtil.serialize(cTree, tree);
//    }

    public void merge(String branchName) {
        File inFile = new File(REPO + "/commitTree");
        CommitTree tree = (CommitTree) FileUtil.deSerialize(inFile);
        File inFileStage = new File(REPO + "/stagingArea");
        StagingArea stage = (StagingArea) FileUtil.deSerialize(inFileStage);
        if (!tree.branchPointers.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if ((stage.getAddedSize() > 0) || (stage.getRemovedSize() > 0)) {
            System.out.println("You have uncommitted changes");
            System.exit(0);
        }
        if (tree.currBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself");
            System.exit(0);
        }
        File pwd = new File(System.getProperty("user.dir"));
        CommitNode given = tree.getCommitNode(tree.branchPointers.get(branchName));
        CommitNode curr = tree.getCommitNode(tree.branchPointers.get(tree.currBranch));
        for (File file : pwd.listFiles()) {
            if (!tree.currPtr.fileNames.containsKey(file.getName())
                    && given.fileNames.containsKey(file.getName())) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it or add it first.");
                System.exit(0);
            }
        }
        String splitPoint = tree.findSplitNode(branchName);
        if (splitPoint == null) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitPoint.equals(tree.branchPointers.get(tree.currBranch))) {
            tree.branchPointers.put(tree.currBranch, tree.branchPointers.get(branchName));
            String currPtrId = tree.branchPointers.get(tree.currBranch);
            tree.currPtr = tree.getCommitNode(currPtrId);
            System.out.println("Current branch fast-forwarded.");
            File cTree = new File(REPO + "/commitTree");
            FileUtil.serialize(cTree, tree);
            System.exit(0);
        }
        CommitNode split = tree.getCommitNode(splitPoint);
        for (String file : split.fileNames.keySet()) {
            byte[] splitFile = split.getBlob(split.fileNames.get(file)).content;
            if (given.fileNames.containsKey(file) && curr.fileNames.containsKey(file)) {
                byte[] currFile = curr.getBlob(curr.fileNames.get(file)).content;
                byte[] givFile = given.getBlob(given.fileNames.get(file)).content;
                if (Arrays.equals(currFile, splitFile) && !Arrays.equals(givFile, splitFile)) {
                    tree.checkoutFile(file, given.commitID);
                    add(file);
                } else if (!Arrays.equals(currFile, splitFile) && !Arrays.equals(givFile, splitFile)
                        && !Arrays.equals(currFile, givFile)) {
                    mergeHelper(file, currFile, givFile);
                    merged = false;
                }
            } else if (!given.fileNames.containsKey(file) && curr.fileNames.containsKey(file)) {
                byte[] currFile = curr.getBlob(curr.fileNames.get(file)).content;
                if (Arrays.equals(currFile, splitFile)) {
                    rm(file);
                } else if (!Arrays.equals(currFile, splitFile)) {
                    mergeHelper(file, currFile, new byte[0]);
                    merged = false;
                }
            } else if (given.fileNames.containsKey(file) && !curr.fileNames.containsKey(file)) {
                byte[] givFile = given.getBlob(given.fileNames.get(file)).content;
                if (!Arrays.equals(givFile, splitFile)) {
                    mergeHelper(file, new byte[0], givFile);
                    merged = false;
                }
            }
        }
        mergeExtension(split, curr, given);
        if (merged) {
            commit("Merged " + tree.currBranch + " with " + branchName + ".");
        } else {
            System.out.println("Encountered a merge conflict.");
        }
    }

    private void mergeHelper(String filename, byte[] currContent, byte[] givContent) {
        try {
            Files.write(Paths.get(filename), ("<<<<<<< HEAD" + System.lineSeparator()).getBytes());
            Files.write(Paths.get(filename), currContent, StandardOpenOption.APPEND);
            Files.write(Paths.get(filename), ("=======" + System.lineSeparator()).getBytes(),
                    StandardOpenOption.APPEND);
            Files.write(Paths.get(filename), givContent, StandardOpenOption.APPEND);
            Files.write(Paths.get(filename),
                    (">>>>>>>" + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mergeExtension(CommitNode split,
                                CommitNode curr, CommitNode given) {
        for (String gfile : given.fileNames.keySet()) {
            CommitTree tree = (CommitTree) FileUtil.deSerialize(
                    new File(REPO + "/commitTree"));
            byte[] givFile = given.getBlob(given.fileNames.get(gfile)).content;
            if (!split.fileNames.containsKey(gfile) && !curr.fileNames.containsKey(gfile)) {
                tree.checkoutFile(gfile, given.commitID);
                add(gfile);
            } else if (!split.fileNames.containsKey(gfile)) {
                byte[] currFile = curr.getBlob(curr.fileNames.get(gfile)).content;
                //if (!currFile.equals(givFile)) {
                if (!Arrays.equals(currFile, givFile)) {
                    mergeHelper(gfile, currFile, givFile);
                    merged = false;
                }
            }
        }
    }



    public void add(String fileName) {
        File inFile = new File(REPO + "/commitTree");
        CommitTree tree = (CommitTree) FileUtil.deSerialize(inFile);
        CommitNode curr = tree.currPtr;
        File inFile2 = new File(REPO + "/stagingArea");
        StagingArea stage = (StagingArea) FileUtil.deSerialize(inFile2);
        if (!checkInWorkingDir(fileName)) {
            System.out.println("File does not exist");
            System.exit(0);
        } else if (curr.fileNames.containsKey(fileName)) {
            String newHash = Utils.sha1(Utils.readContents(new File(fileName)));
            String currHash = Utils.sha1(curr.getBlob(curr.fileNames.get(fileName)).content);
            if (newHash.equals(currHash)) {
                if (stage.getRemovedFiles().contains(fileName)) {
                    stage.getRemovedFiles().remove(fileName);
                }
                File sArea = new File(REPO + "/stagingArea");
                FileUtil.serialize(sArea, stage);
                System.exit(0);
            }
        }
        stage.addFile(fileName);
        File sArea = new File(REPO + "/stagingArea");
        FileUtil.serialize(sArea, stage);
        File cTree = new File(REPO + "/commitTree");
        FileUtil.serialize(cTree, tree);
    }

    private boolean checkInWorkingDir(String fileName) {
        File folder = new File(WORKING_DIR);
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isFile() && file.getName().equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    public void rm(String fileName) {
        File inFile = new File(REPO + "/stagingArea");
        StagingArea stage = (StagingArea) FileUtil.deSerialize(inFile);
        stage.rm(fileName);
        File sArea = new File(REPO + "/stagingArea");
        FileUtil.serialize(sArea, stage);
    }

    public void rmBranch(String branchName) {
        File inFile = new File(REPO + "/commitTree");
        CommitTree tree = (CommitTree) FileUtil.deSerialize(inFile);

        tree.rmBranch(branchName);

        File cTree = new File(REPO + "/commitTree");
        FileUtil.serialize(cTree, tree);
    }

    public void branch(String branchName) {
        File inFile = new File(REPO + "/commitTree");
        CommitTree tree = (CommitTree) FileUtil.deSerialize(inFile);

        tree.addBranch(branchName);

        File cTree = new File(REPO + "/commitTree");
        FileUtil.serialize(cTree, tree);
    }

    public static void main(String[] args) {
        GitLet g = new GitLet();
        g.init();
        g.add("hi.txt");
        g.branch("other");
        g.add("hey.txt");
        g.commit("version 1");
        g.rm("hi.txt");
        g.checkout(new String[]{"branch ", "other"});
        g.add("hey.txt");
        g.commit("version 2");
        g.checkout(new String[]{"branch ", "master"});
        File f = new File("hi.txt");
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
