package de.diavololoop.io;

import de.diavololoop.Program;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Created by Peer on 21.04.2017.
 */
public class SystemWatcher {

    private ArrayList<Path> observedDirectorys = new ArrayList<Path>();
    private ArrayList<Runnable> listener = new ArrayList<Runnable>();
    private boolean active;


    public SystemWatcher(File directory) throws IOException {
        Thread watcherThread = new Thread(() -> {

            try {
                initSystemWatcher(directory);
            } catch (InterruptedException | IOException e) {
                Program.instance().criticalError("error in system watch service", e);
            }

        });

        watcherThread.setDaemon(true);
        watcherThread.setName("DirectoryObserverThread");
        watcherThread.start();
    }

    public void initSystemWatcher(File directory) throws InterruptedException, IOException {
        Path root = Paths.get(directory.toURI());
        observedDirectorys.add(root);

        // We obtain the file system of the Path
        FileSystem fs = root.getFileSystem ();

        WatchService service = fs.newWatchService();

        registerAllSubFolder(directory);

        for(Path path: observedDirectorys){
            path.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        }

        // Start the infinite polling loop
        WatchKey key = null;
        while(true) {
            key = service.take();

            // Dequeueing events
            WatchEvent.Kind<?> kind = null;
            for (WatchEvent<?> watchEvent : key.pollEvents()) {
                // Get the type of the event
                kind = watchEvent.kind();
                if (OVERFLOW == kind) {
                    continue; //loop
                } else if (ENTRY_CREATE == kind) {
                    update();
                } else if (StandardWatchEventKinds.ENTRY_DELETE == kind) {
                    update();
                } else if (StandardWatchEventKinds.ENTRY_MODIFY == kind) {
                    update();
                }
            }

            if (!key.reset()) {
                break; //loop
            }
        }
    }

    public void setActive(boolean state){
        active = state;
    }

    public void addListener(Runnable runnable){
        listener.add(runnable);
    }

    private void update(){
        if(active){
            listener.forEach(runnable -> runnable.run());
        }
    }

    private void registerAllSubFolder(File directory){
        File[] files = directory.listFiles();

        for(File f: files){
            if(!f.isDirectory()){
                continue;
            }

            Path path = Paths.get(f.toURI());

            observedDirectorys.add(path);

            registerAllSubFolder(f);
        }
    }
}
