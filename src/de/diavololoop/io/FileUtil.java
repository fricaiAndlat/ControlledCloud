package de.diavololoop.io;

import java.io.File;
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
}
