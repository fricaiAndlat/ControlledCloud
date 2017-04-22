package de.diavololoop;

import java.io.*;
import java.nio.file.Files;

/**
 * Created by Peer on 21.04.2017.
 */
public class Setting {

    public File localDirectory = new File("C:\\Users\\Peer\\Desktop\\cloudtest\\local");
    public File cloudDirectory = new File("C:\\Users\\Peer\\Desktop\\cloudtest\\cloud");
    public File tempDirectory;


    //public File jarFile;

    public int updateMethod = 0; //0 = never, 1 = prompt user, 2 = autoinstall
    //public File cloudImageFile;
    //public File localImageFile;

    public long preferedEncryptedTileLength = 512 * 1024 - 2 * 1024;

    //public long secondsToBufferDirectory = 1 * 1000;

    public static Setting read(File file){
        Setting setting = new Setting();
        try {
            DataInputStream input = new DataInputStream(new FileInputStream(file));

            setting.cloudDirectory = new File(input.readUTF());
            setting.tempDirectory = new File(input.readUTF());
            setting.localDirectory = new File(input.readUTF());

            setting.preferedEncryptedTileLength = input.readLong();
            setting.updateMethod = input.readInt();

            input.close();
        } catch (IOException e) {
            Program.instance().criticalError("could not read Setting", e);
        }
        return setting;
    }

    public Setting(){
        tempDirectory = new File(localDirectory, ".temp");

        if(!tempDirectory.exists()){
            tempDirectory.mkdirs();

            try {
                Files.setAttribute(tempDirectory.toPath(), "dos:hidden", true);
            } catch (IOException e) {
                System.out.println("cant make temp directory hidden");
            }
        }


        /*cloudImageFile = new File(tempDirectory, ".cif");
        localImageFile = new File(tempDirectory, ".lif");*/

    }

    public void save(File file){
        try {
            DataOutputStream output = new DataOutputStream(new FileOutputStream(file));

            output.writeUTF(cloudDirectory.getAbsolutePath());
            output.writeUTF(tempDirectory.getAbsolutePath());
            output.writeUTF(localDirectory.getAbsolutePath());

            output.writeLong(preferedEncryptedTileLength);
            output.writeInt(updateMethod);

            output.flush();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
