package de.diavololoop.io;

import de.diavololoop.util.Util;

import java.io.*;
import java.util.function.BiFunction;

/**
 * Created by Peer on 21.04.2017.
 */
public class FileUtil {

    public static void executeOnAllFiles(File cwd, String relPath, BiFunction<File, String, Boolean> consumer, boolean pre){
        boolean recursive = true;
        if(pre){
            recursive = consumer.apply(cwd, relPath);
        }

        if(recursive && cwd.exists() && cwd.isDirectory()){
            File[] files = cwd.listFiles();

            for(File f: files){
                executeOnAllFiles(f, relPath+"/"+f.getName(), consumer, pre);
            }
        }

        if(!pre){
            consumer.apply(cwd, relPath);
        }

    }

    public static void main(String[] args) throws IOException {

        File f = new File("C:\\Users\\Peer\\Desktop\\cloudtest\\program\\bla.txt");

        FileInputStream in = new FileInputStream(f);

        byte[] buffer = new byte[8];

        while(-1 != (in.read(buffer))){

            System.out.println(Util.toHex(buffer));
        }

    }

    public static void copyClassToFile(String c, File root) throws IOException, ClassNotFoundException {
        String[] path = c.split("\\.");
        File base = root;
        for(int i = 0; i+1 < path.length; ++i){
            base = new File(base, path[i]);
        }
        base.mkdirs();

        base = new File(base, path[path.length-1]+".class");
        base.createNewFile();

        InputStream in = Class.forName(c).getResourceAsStream(path[path.length-1]+".class");
        FileOutputStream out = new FileOutputStream(base);
        byte[] buffer = new byte[1014];

        int len;
        while(-1 != (len = in.read(buffer))){
            out.write(buffer, 0, len);
        }

        in.close();
        out.flush();
        out.close();

    }

    public static void copyRessourceToFile(String c, File path) throws IOException, ClassNotFoundException {
        path.getParentFile().mkdirs();

        path.createNewFile();

        InputStream in = FileUtil.class.getResourceAsStream(c);
        FileOutputStream out = new FileOutputStream(path);
        byte[] buffer = new byte[1014];

        int len;
        while(-1 != (len = in.read(buffer))){
            out.write(buffer, 0, len);
        }

        in.close();
        out.flush();
        out.close();

    }
}
