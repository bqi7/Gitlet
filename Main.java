package gitlet;

import java.io.File;

/* Driver class for Gitlet, the tiny stupid version-control system.
   @author Brian Qi, Marco Lyu, Michael Huang, David Yi
*/

public class Main {

    /* Usage: java gitlet.Main ARGS, where ARGS contains
       <COMMAND> <OPERAND> .... */
    private static void checkInit(boolean init) {
        File g = new File(".gitlet");
        if (!g.exists()) {
            System.out.println("Not in an initialized gitlet directory.");
            System.exit(0);
        }
    }

    private static void checkArgs(String[] args, int correctArgs) {
        if ((args.length != correctArgs + 1)
                || (args.length > 1 && !(args[1] instanceof String))) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    private static void checkArgsCheckout(String[] args) {
        if ((args.length > 4 || args.length == 1)
                || (args.length == 2 && !(args[1] instanceof String))
                || (args.length == 3 && (!args[1].equals("--") || !(args[2] instanceof String)))
                || (args.length == 4 && (!(args[1] instanceof String)
                || !args[2].equals("--") || !(args[3] instanceof String)))) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
        }
        boolean init = false;

        GitLet gitLit = new GitLet();
        String command = args[0];
        switch (command) {
            case "init":
                gitLit.init();
                break;
            case "add":
                checkArgs(args, 1);
                checkInit(init);
                gitLit.add(args[1]);
                break;
            case "commit":
                checkArgs(args, 1);
                checkInit(init);
                gitLit.commit(args[1]);
                break;
            case "rm":
                checkArgs(args, 1);
                checkInit(init);
                gitLit.rm(args[1]);
                break;
            case "log":
                checkArgs(args, 0);
                checkInit(init);
                gitLit.log();
                break;
            case "global-log":
                checkArgs(args, 0);
                checkInit(init);
                gitLit.globalLog();
                break;
            case "find":
                checkArgs(args, 1);
                checkInit(init);
                gitLit.find(args[1]);
                break;
            case "status":
                checkArgs(args, 0);
                checkInit(init);
                gitLit.status();
                break;
            case "checkout":
                checkArgsCheckout(args);
                checkInit(init);
                gitLit.checkout(args);
                break;
            case "branch":
                checkArgs(args, 1);
                checkInit(init);
                gitLit.branch(args[1]);
                break;
            case "rm-branch":
                checkArgs(args, 1);
                checkInit(init);
                gitLit.rmBranch(args[1]);
                break;
            case "reset":
                checkArgs(args, 1);
                checkInit(init);
                gitLit.reset(args[1]);
                break;
            case "merge":
                checkArgs(args, 1);
                checkInit(init);
                gitLit.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }
}
