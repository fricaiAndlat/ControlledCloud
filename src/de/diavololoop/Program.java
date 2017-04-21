package de.diavololoop;

import de.diavololoop.gui.GUI;
import de.diavololoop.io.SystemWatcher;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.io.File;

import java.awt.*;

/**
 * Created by Peer on 21.04.2017.
 */
public class Program extends Application{

    public final static String VERSION = "0.1";

    private static Program CURRENT_INSTANCE;

    public static void main(String[] args){
        Program.launch(args);
    }

    public static Program instance(){
        return CURRENT_INSTANCE;
    }



    private GUI gui;
    private SystemWatcher watcher;

    @Override
    public void start(Stage primaryStage) throws Exception {
        CURRENT_INSTANCE = this;
        gui = new GUI();
        watcher = new SystemWatcher(new File("C:\\Users\\Peer\\Documents\\temp"));
    }


    public void requestStop() {

        Platform.exit();
        System.exit(0);

    }

    public void criticalError(String desc, Exception e) {
        System.err.println("CRITICAL ERROR: " + desc);
        e.printStackTrace(System.err);

        gui.sendMessage(desc, e.getMessage(), TrayIcon.MessageType.ERROR);
    }
}
