package de.diavololoop.installer;

import de.diavololoop.Program;
import de.diavololoop.io.FileUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

/**
 * Created by Peer on 22.04.2017.
 */
public class Installer extends Application{



    public static void main(String[] args){
        try {
            FileUtil.copyClassToFile("de.diavololoop.security.SunSecurityProvider", new File("C:\\Users\\Peer\\Desktop\\cloudtest\\program"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println(System.getProperty("java.home"));

        boolean startAsInstaller = true;
        String pathToExecutable = null;
        String token = null;

        for(int i = 0; i < args.length; ++i){
            if(args[i].equalsIgnoreCase("-u")){
                startAsInstaller = false;
            }else if(args[i].equalsIgnoreCase("-e")){
                if(args.length == i+1){
                    printHelp();
                }

                pathToExecutable = args[i+1];
            }else if(args[i].equalsIgnoreCase("-t")){
                if(args.length == i+1){
                    printHelp();
                }

                token = args[i+1];
            }
        }

        if(startAsInstaller){
            launch();
        }else{
            Program.main(token, pathToExecutable);
        }


    }

    public static void printHelp(){
        System.exit(0);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("../../../layout/install.fxml"));
        Parent fxml = fxmlLoader.load();
        Scene scene = new Scene(fxml);
        stage.setScene(scene);
        stage.show();

        InstallController controller = fxmlLoader.<InstallController>getController();
        controller.setStage(stage);
    }

    public void installTo(File f){
        ClassLoader loader = Installer.class.getClassLoader();
    }
}
