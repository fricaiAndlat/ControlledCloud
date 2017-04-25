package de.diavololoop;

import de.diavololoop.gui.GUI;
import de.diavololoop.io.EncryptedFileTile;
import de.diavololoop.io.FileUtil;
import de.diavololoop.security.SecurityProvider;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Peer on 21.04.2017.
 */
public class Program extends Application{

    public final static String VERSION = "0.1";

    private static Program CURRENT_INSTANCE;
    private static String token;
    private static String programPath;


    public static void main(String... args){
        token = args[0];
        programPath = args[1];
        Program.launch(args);
    }

    public static Program instance(){
        return CURRENT_INSTANCE;
    }


    private SecurityProvider security;
    private GUI gui;
    private Setting setting;


    @Override
    public void start(Stage primaryStage) throws Exception {
        CURRENT_INSTANCE = this;
        Platform.setImplicitExit(false);
        gui = new GUI(primaryStage);
        setting = Setting.read(new File(programPath, "setting"));

        security = SecurityProvider.getDefault(token);



    }


    synchronized public void requestStop() {
        Platform.exit();
        System.exit(0);

    }

    public void criticalError(String desc, Exception e) {
        System.err.println("CRITICAL ERROR: " + desc);
        e.printStackTrace(System.err);

        gui.sendMessage(desc, e.getMessage(), TrayIcon.MessageType.ERROR);
    }

    public void warningError(String desc, Exception e){
        System.out.println("waning: "+desc);
    }

    public Setting getSetting() {
        return setting;
    }

    public void userWarnError(String s) {
        gui.sendMessage(s, "", TrayIcon.MessageType.WARNING);
    }
    public void userWarnError(String s, Exception e) {
        gui.sendMessage(s, e.getMessage(), TrayIcon.MessageType.WARNING);
    }



    // TODO MAKE THINS PRETTY AND REALTIME (also make this in an extra class)
    synchronized public void synchronizeCloudToLocal() {

        long startTime = System.currentTimeMillis();

        File[] fileArray = setting.cloudDirectory.listFiles();

        ArrayList<File> cloudFiles = new ArrayList<File>(fileArray.length);
        for(File f: fileArray)cloudFiles.add(f);


        List<EncryptedFileTile> fileTiles = cloudFiles.parallelStream()
                .map(f -> new EncryptedFileTile(f))
                .collect(Collectors.toList());

        fileTiles.stream().forEach(t -> t.decrypt(security));

        Map<String, List<EncryptedFileTile>> tileGroups = fileTiles.parallelStream()
                .sorted((a, b) -> {
                    if(a.isDirectory() || b.isDirectory()){
                        return a.getFilePath().length() - b.getFilePath().length();
                    }
                    if(a.isDirectory()){
                        return 1;
                    }
                    if(b.isDirectory()){
                        return -1;
                    }

                    return 0;
                })
                .collect(Collectors.groupingBy(t -> t.getFilePath()));

        tileGroups.values().stream()
                .map(g -> {
                    try {
                        return EncryptedFileTile.createDecrypted(g);
                    } catch (IOException e) {
                        userWarnError("cant process "+g.get(0).getFilePath(), e);
                    }
                    return null;
                })
                .filter(f -> f!=null)
                .collect(Collectors.toList())
                .forEach(f -> f.setLastModified(System.currentTimeMillis()));


        FileUtil.executeOnAllFiles(setting.localDirectory, "", (file, name) -> {
            if(name.startsWith(".temp") || name.equals("")){
                return false;
            }

            if(file.lastModified() < startTime){

                if(!file.delete()){
                    warningError("could not delete file "+file.getAbsolutePath(), new Exception());
                }

            }

            return true;
        }, false);

    }

    synchronized public void synchronizeLocalToCloud() {
        long startTime = System.currentTimeMillis();

        LinkedList<File> encrypted = new LinkedList<File>();

        FileUtil.executeOnAllFiles(setting.localDirectory, "", (file, name) -> {
            if(name.equals(".temp")){
                return false;
            }

            if(name.equals("")){
                return true;
            }

            try {
                encrypted.addAll(EncryptedFileTile.createEncrypted(name, security));
            } catch (IOException e) {
                userWarnError("cant encrypt File: "+name, e);
            }
            return true;
        }, true);

        encrypted.forEach(f -> f.setLastModified(System.currentTimeMillis()));

        for(File f: setting.cloudDirectory.listFiles()){
            if(f.lastModified() < startTime){

                if(!f.delete()){
                    warningError("could not delete file "+f.getAbsolutePath(), new Exception());
                }

            }
        }
    }
}
