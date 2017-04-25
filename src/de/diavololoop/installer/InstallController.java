package de.diavololoop.installer;

import de.diavololoop.Setting;
import de.diavololoop.io.FileUtil;
import de.diavololoop.util.Util;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;

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
    @FXML CheckBox chkAutostart;
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

    public void onError(String error, Exception e){

    }

    @FXML
    public void onInstall(){

        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Util.toHex(tokenBytes);

        File programRoot = new File(fldDestination.getText());
        programRoot.mkdirs();

        Setting setting = new Setting();
        setting.localDirectory = new File( fldLocalPath.getText() );
        setting.cloudDirectory = new File( fldCloudPath.getText() );
        setting.tempDirectory = new File(setting.localDirectory, ".temp");

        File settingFile = new File(programRoot, "setting");

        setting.save(settingFile);

        //writing bash start script
        try {
            FileWriter output = new FileWriter(new File(programRoot, "start.sh"));
            output.write("\"");
            output.write(System.getProperty("java.home"));
            output.write("/bin/javaw\" de.diavololoop.Program ");
            output.write(token);
            output.write(" \"");
            output.write(programRoot.getAbsolutePath());
            output.write("\"\n");
            output.flush();
            output.close();

        } catch (IOException e) {
            onError("Error while writing startscript", e);
        }

        try {
            FileWriter output = new FileWriter(new File(programRoot, "start.bat"));
            output.write("\"");
            output.write(System.getProperty("java.home"));
            output.write("\\bin\\javaw.exe\" de.diavololoop.Program ");
            output.write(token);
            output.write(" \"");
            output.write(programRoot.getAbsolutePath());
            output.write("\"\n");
            output.flush();
            output.close();

        } catch (IOException e) {
            onError("Error while writing startscript", e);
        }

        if(chkAutostart.isSelected() && System.getProperty("os.name").matches(".*[wW]indows.*")){
            try {
                FileWriter output = new FileWriter(new File(programRoot, "start.bat"));
                output.write("\"");
                output.write(System.getProperty("java.home"));
                output.write("\\bin\\javaw.exe\" de.diavololoop.Program ");
                output.write(token);
                output.write(" \"");
                output.write(programRoot.getAbsolutePath());
                output.write("\"\n");
                output.flush();
                output.close();

            } catch (IOException e) {
                onError("Error while writing startscript", e);
            }
        }

        try {
            FileUtil.copyClassToFile("de.diavololoop.gui.GUI", programRoot);
            FileUtil.copyClassToFile("de.diavololoop.io.EncryptedFileTile", programRoot);
            FileUtil.copyClassToFile("de.diavololoop.io.FileUtil", programRoot);
            FileUtil.copyClassToFile("de.diavololoop.security.SecurityProvider", programRoot);
            FileUtil.copyClassToFile("de.diavololoop.security.SunSecurityProvider", programRoot);
            FileUtil.copyClassToFile("de.diavololoop.util.Util", programRoot);
            FileUtil.copyClassToFile("de.diavololoop.Program", programRoot);
            FileUtil.copyClassToFile("de.diavololoop.Setting", programRoot);

            File iconDir = new File(programRoot, "icon");
            FileUtil.copyRessourceToFile("../../../icon/icon64.png", new File(iconDir, "icon64.png"));
            FileUtil.copyRessourceToFile("../../../icon/icon128.png", new File(iconDir, "icon128.png"));
            FileUtil.copyRessourceToFile("../../../icon/icon256.png", new File(iconDir, "icon256.png"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    @FXML
    public void onFinish(){

    }

}
