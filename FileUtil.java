package gitlet;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/* Functions to deserialize and serialize files */
public class FileUtil {

    public static void serialize(File outFile, Object obj) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outFile));
            out.writeObject(obj);
            out.close();
        } catch (IOException excp) {
            excp.printStackTrace();
        }
    }

    public static Object deSerialize(File inFile) {
        try {
            ObjectInputStream inp = new ObjectInputStream(new FileInputStream(inFile));
            Object obj = inp.readObject();
            inp.close();
            return obj;
        } catch (IOException | ClassNotFoundException excp) {
            excp.printStackTrace();
        }
        return null;
    }

    public static byte[] changeToByteArr(Object obj) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(obj);
            objectStream.close();
            return stream.toByteArray();
        } catch (IOException excp) {
            excp.printStackTrace();
        }
        return null;
    }
}
