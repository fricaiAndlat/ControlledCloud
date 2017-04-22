package de.diavololoop.installer;

import de.diavololoop.Setting;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by Peer on 22.04.2017.
 */
public class InstallController {

    private int currentstep = 0;
    Stage stage;

    @FXML TabPane viewTab;
    @FXML Button btnNextLicense;
    @FXML Button btnNextTarget;
    @FXML Button btnNextFolders;
    @FXML RadioButton chkLicense;
    @FXML TextField fldDestination;
    @FXML TextField fldCloudPath;
    @FXML TextField fldLocalPath;

    public void setStage(Stage stage){
        int osarch = 32;

        try{
            osarch = Integer.parseInt(System.getProperty("sun.arch.data.model"));
        }catch (IllegalArgumentException e){

        }
        this.stage = stage;

        if(System.getProperty("os.name").equals("Windows 10") && osarch == 64){
            fldDestination.setText("C:/Program Files/diavololoop/CloudControl/");
        }else if(System.getProperty("os.name").equals("Windows 10") && osarch == 32){
            fldDestination.setText("C:/Program Files (x86)/diavololoop/CloudControl/");
        }
        onDestinationChange();

    }

    @FXML
    public void initialize(){

    }

    @FXML
    public void onChangeLocalFile(){
        File local = new File(fldLocalPath.getText());
        File cloud = new File(fldCloudPath.getText());

        btnNextFolders.setDisable(!(local.isDirectory() && cloud.isDirectory()));
    }

    @FXML
    public void onChangeCloudFile(){
        File local = new File(fldLocalPath.getText());
        File cloud = new File(fldCloudPath.getText());

        btnNextFolders.setDisable(!(local.isDirectory() && cloud.isDirectory()));
    }

    @FXML
    public void onBrowseLocalFile(){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if(selectedDirectory != null){
            fldLocalPath.setText(selectedDirectory.getAbsolutePath());
        }
        onChangeLocalFile();
    }

    @FXML
    public void onBrowseCloudFile(){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if(selectedDirectory != null){
            fldCloudPath.setText(selectedDirectory.getAbsolutePath());
        }
        onChangeCloudFile();
    }

    @FXML
    public void onBrowseDestination(){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if(selectedDirectory != null){
            fldDestination.setText(selectedDirectory.getAbsolutePath());
        }
        onDestinationChange();
    }

    @FXML
    public void onDestinationChange(){
        File f = new File(fldDestination.getText());
        System.out.println(f.exists() && f.isFile());
        btnNextTarget.setDisable(f.exists() && f.isFile());
    }

    @FXML
    public void onLicenseChange(){
        btnNextLicense.setDisable(!chkLicense.isSelected());
    }

    @FXML
    public void previousCard(){
        viewTab.getSelectionModel().select(--currentstep);
    }

    @FXML
    public void nextCard(){
        viewTab.getSelectionModel().select(++currentstep);
    }

    @FXML
    public void onCancel(){

    }

    @FXML
    public void onInstall(){

        try {

            URL url = InstallController.class.getProtectionDomain().getCodeSource().getLocation();
            File codeBase = new File(url.toURI());
            System.out.println(codeBase);

            File programRoot = new File(fldDestination.getText());
            programRoot.mkdirs();

            Setting setting = new Setting();
            setting.localDirectory = new File( fldLocalPath.getText() );
            setting.cloudDirectory = new File( fldCloudPath.getText() );
            setting.tempDirectory = new File(setting.localDirectory, ".temp");

            File settingFile = new File(programRoot, "setting");

            setting.save(settingFile);

            //copy binary
            InputStream input = InstallController.class.getResourceAsStream("../../file/ControlledCloud.jar");
            FileOutputStream output = new FileOutputStream(new File(programRoot, "ControlledCloud.jar"));

            int len;
            byte[] buffer = new byte[1024];
            while(-1 != (len = input.read(buffer))){
                output.write(buffer, 0, len);
            }
            input.close();
            output.flush();
            output.close();




        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onFinish(){

    }

}
