
import javax.swing.*;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;

public class MainWatch {

    public static void watchDirectoryPath(Path path) {
        // Sanity check - Check if path is a folder
        try {
            Boolean isFolder = (Boolean) Files.getAttribute(path,
                    "basic:isDirectory", NOFOLLOW_LINKS);
            if (!isFolder) {
                throw new IllegalArgumentException("Path: " + path + " is not a folder");
            }
        } catch (IOException ioe) {
            // Folder does not exists
            ioe.printStackTrace();
        }

        System.out.println("Watching path: " + path);

        // We obtain the file system of the Path
        FileSystem fs = path.getFileSystem ();

        // We create the new WatchService using the new try() block
        try(WatchService service = fs.newWatchService()) {

            // We register the path to the service
            // We watch for creation events
            path.register(service, ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

            // Start the infinite polling loop
            WatchKey key = null;
            while(true) {
                key = service.take();

                // Dequeueing events
                Kind<?> kind = null;
                for(WatchEvent<?> watchEvent : key.pollEvents()) {
                    // Get the type of the event
                    kind = watchEvent.kind();
                    if (OVERFLOW == kind) {
                        continue; //loop
                    } else if (ENTRY_CREATE == kind) {
                        // A new Path was created
                        Path newPath = ((WatchEvent<Path>) watchEvent).context();
                        // Output
                        System.out.println("New path created: " + newPath);
                    } else if ( StandardWatchEventKinds.ENTRY_DELETE == kind) {
                        // A new Path was created
                        Path newPath = ((WatchEvent<Path>) watchEvent).context();
                        // Output
                        System.out.println("New path deleted: " + newPath);
                    } else if ( StandardWatchEventKinds.ENTRY_MODIFY == kind) {
                        // A new Path was created
                        Path newPath = ((WatchEvent<Path>) watchEvent).context();
                        // Output
                        System.out.println("New path mod: " + newPath);
                    }
                }

                if(!key.reset()) {
                    break; //loop
                }
            }

        } catch(IOException ioe) {
            ioe.printStackTrace();
        } catch(InterruptedException ie) {
            ie.printStackTrace();
        }

    }

    public static void main(String[] args) throws IOException,
            InterruptedException {

        PopupMenu popup = new PopupMenu();
        MenuItem item = new MenuItem( "Ende" );
        item.addActionListener( new ActionListener() {
            @Override public void actionPerformed( ActionEvent e ) {
                System.exit( 0 );
            }
        } );
        popup.add( item );

        TrayIcon trayIcon = new TrayIcon( new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB), "Java-Tray ", popup );
        trayIcon.setImageAutoSize( true );

        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add( trayIcon );
        } catch (AWTException e) {
            e.printStackTrace();
        }

        trayIcon.displayMessage( "Formatierung beendet.",
                "Sie können den Rechner jetzt neu installieren.",
                TrayIcon.MessageType.INFO );


        // Folder we are going to watch
        Path folder = Paths.get(System.getProperty("user.home"));
        //watchDirectoryPath(folder);
    }



}