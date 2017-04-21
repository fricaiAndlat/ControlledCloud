package de.diavololoop.gui;

import de.diavololoop.Program;
import javafx.application.Application;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Created by Peer on 21.04.2017.
 */
public class GUI {

    private boolean hasTray;

    private TrayIcon trayIcon;

    public GUI() {
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

        MenuItem item = new MenuItem( "Stop controlled clouding service" );
        item.addActionListener( e -> Program.instance().requestStop() );
        popup.add( item );

        trayIcon = new TrayIcon(ImageIO.read(GUI.class.getResourceAsStream("../../../icon/icon128.png")), "Java-Tray ", popup );
        trayIcon.setToolTip("Controlled Clouding v"+Program.VERSION);
        trayIcon.setImageAutoSize( true );

        SystemTray tray = SystemTray.getSystemTray();

        tray.add( trayIcon );
    }

    public void sendMessage(String desc, String message, TrayIcon.MessageType type) {

        trayIcon.displayMessage("Controlled Clouding", desc + "\r\n" + message, type);

    }
}
