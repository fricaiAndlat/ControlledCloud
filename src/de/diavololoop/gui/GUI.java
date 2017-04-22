package de.diavololoop.gui;

import de.diavololoop.Program;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

/**
 * Created by Peer on 21.04.2017.
 */
public class GUI{

    private boolean hasTray;

    private TrayIcon trayIcon;
    private Stage stage;

    public GUI(Stage stage) {
        this.stage = stage;

        hasTray = SystemTray.isSupported();

        if(hasTray){

            try {
                createTray();
            } catch (IOException | AWTException e) {
                e.printStackTrace();
            }

        }
    }


    private void createTray() throws IOException, AWTException {

        PopupMenu popup = new PopupMenu();

        MenuItem item1 = new MenuItem( "Stop controlled clouding service" );
        item1.addActionListener( e -> Program.instance().requestStop() );
        popup.add( item1 );

        MenuItem item2 = new MenuItem( "synchronize local -> cloud" );
        item2.addActionListener( e -> Platform.runLater(() -> {

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Controlled Cloud - possible data conflict");
            alert.setHeaderText("synchronizing local changes to cloud will override all changes in cloud");
            alert.setContentText("Are you ok with this?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){

                item1.setEnabled(false);
                item2.setEnabled(false);
                Thread worker = new Thread(() -> {
                    Program.instance().synchronizeLocalToCloud();

                    SwingUtilities.invokeLater(() -> {
                        item2.setEnabled(true);
                        item1.setEnabled(true);
                    });

                    sendMessage("Synchronisation complete", "", TrayIcon.MessageType.NONE);

                });
                worker.start();



            }

        }));
        popup.add( item2 );

        MenuItem item3 = new MenuItem( "synchronize cloud -> local" );
        item3.addActionListener( e -> Platform.runLater(() -> {

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Controlled Cloud - possible data conflict");
            alert.setHeaderText("synchronizing cloud changes to local will override all local changes");
            alert.setContentText("Are you ok with this?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){

                item1.setEnabled(false);
                item2.setEnabled(false);
                Thread worker = new Thread(() -> {
                    Program.instance().synchronizeCloudToLocal();

                    SwingUtilities.invokeLater(() -> {
                        item2.setEnabled(true);
                        item1.setEnabled(true);
                    });

                    sendMessage("Synchronisation complete", "", TrayIcon.MessageType.NONE);

                });
                worker.start();



            }

        }));
        popup.add( item3 );

        trayIcon = new TrayIcon(ImageIO.read(GUI.class.getResourceAsStream("../../../icon/icon128.png")), "Java-Tray ", popup);
        trayIcon.setToolTip("Controlled Clouding v"+Program.VERSION);
        trayIcon.setImageAutoSize( true );

        SystemTray tray = SystemTray.getSystemTray();

        tray.add( trayIcon );
    }

    public void sendMessage(String desc, String message, TrayIcon.MessageType type) {
        trayIcon.displayMessage("Controlled Clouding", desc + "\r\n" + message, type);

    }

    public void release(){
        SystemTray.getSystemTray().remove(trayIcon);
    }
}
