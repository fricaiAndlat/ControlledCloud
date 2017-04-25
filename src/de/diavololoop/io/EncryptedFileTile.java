package de.diavololoop.io;

import de.diavololoop.Program;
import de.diavololoop.security.SecurityProvider;
import de.diavololoop.util.Util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Peer on 21.04.2017.
 */
public class EncryptedFileTile {

    private File underlyingFile;

    //may be null:
    private File decryptedFile;
    private long tileLength;
    private long tileOffset;
    private String filePath;

    public EncryptedFileTile(File fileTile){
        underlyingFile = fileTile;
    }

    public void decrypt(SecurityProvider security){

        try{
            File fileWithMeta = new File(Program.instance().getSetting().tempDirectory, underlyingFile.getName());

            security.decryptFile(underlyingFile, fileWithMeta);

            fillMetaAndCopyWithout(fileWithMeta);

            if(!fileWithMeta.delete()){
                Program.instance().warningError("cannot delete temporary file "+fileWithMeta.getAbsolutePath(), new Exception());
            }


        }catch(IOException|GeneralSecurityException e){
            Program.instance().warningError("cant decrypt FileTile", e);
        }


    }



    private void fillMetaAndCopyWithout(File fileWithMeta) throws IOException {

        DataInputStream input = new DataInputStream(new FileInputStream(fileWithMeta));

        try {
            tileLength = input.readLong();
            tileOffset = input.readLong();

            int filePathLength = input.readShort()&0xFFFF;
            byte[] filePathBytes = new byte[filePathLength];
            input.readFully(filePathBytes);
            filePath = new String(filePathBytes, StandardCharsets.UTF_8);


            String encodedPath = filePath.replaceAll("_", "__").replaceAll("/", "_");
            decryptedFile = new File(Program.instance().getSetting().tempDirectory, encodedPath + ".part" + tileOffset);

            FileOutputStream output = new FileOutputStream(decryptedFile);

            byte[] buffer = new byte[1024];
            int len;

            while(-1 != (len = input.read(buffer))){
                output.write(buffer, 0, len);
            }

            output.flush();
            output.close();
            input.close();

        } catch (IOException e) {
            input.close();
            throw e;
        }

    }

    public static File createDecrypted(List<EncryptedFileTile> fileTiles) throws IOException {

        String path = null;

        for(EncryptedFileTile tile: fileTiles){
            if(path == null){
                path = tile.filePath;
            }else if(tile.filePath == null){
                throw new RuntimeException("file tiles must be decrypted to be combined");
            }else if(!path.equals(tile.filePath)){
                throw new RuntimeException("file tiles must have the same path to be combined");
            }else if(fileTiles.size()!=1 && (tile.tileLength == -1 || tile.tileOffset == -1)){
                throw new RuntimeException("directories must only have one tile file");
            }
        }

        //directory tile file;
        if(fileTiles.size()==1 && (fileTiles.get(0).tileLength == -1 || fileTiles.get(0).tileOffset == -1)){
            File resultFile = new File(Program.instance().getSetting().localDirectory, path);
            resultFile.mkdirs();

            if(!fileTiles.get(0).decryptedFile.delete()){
                Program.instance().warningError("could not delete temporary file: "+fileTiles.get(0).decryptedFile.getAbsolutePath(), new Exception());
            }

            return resultFile;
        }

        fileTiles.sort((a, b) -> -Long.compare(a.tileOffset, b.tileOffset) );

        long currentOffset = 0;
        File resultFile = new File(Program.instance().getSetting().localDirectory, path);

        if(!resultFile.exists()){
            resultFile.createNewFile();
        }

        RandomAccessFile outputFile  = new RandomAccessFile(resultFile, "rw");

        EncryptedFileTile tile0 = null;
        for(EncryptedFileTile tile: fileTiles){
            if(tile0 != null){
                if(tile.tileOffset+tile.tileLength != tile0.tileOffset){

                    System.out.println("offset(n)  : "+tile.tileOffset);
                    System.out.println("offset(n-1): "+tile0.tileOffset);
                    System.out.println("length(n-1): "+tile0.tileLength);
                    Program.instance().userWarnError("file "+path+" is not complete or may be corrupted (near offset "+tile.tileOffset+")");
                }
            }

            tile0 = tile;

            FileInputStream input = new FileInputStream(tile.decryptedFile);

            if(tile.tileOffset + tile.tileLength > outputFile.length()){
                outputFile.setLength(tile.tileOffset + tile.tileLength);
            }

            outputFile.seek(tile.tileOffset);

            byte[] buffer = new byte[1024];
            long bytesWritten = 0;
            while(bytesWritten < tile.tileLength){
                int BytesToBeWritten = tile.tileLength-bytesWritten>buffer.length ? buffer.length : (int)(tile.tileLength - bytesWritten);

                int len = input.read(buffer, 0, BytesToBeWritten);

                outputFile.write(buffer, 0, len);
                bytesWritten += len;
            }
            input.close();

            if(!tile.decryptedFile.delete()){
                Program.instance().warningError("could not delete temporary file: "+tile.decryptedFile.getAbsolutePath(), new Exception());
            }

        }

        outputFile.close();

        return resultFile;

    }

    public static List<File> createEncrypted(String relPath, SecurityProvider security) throws IOException {



        Random random = new Random();

        ArrayList<File> result = new ArrayList<File>();

        File source = new File(Program.instance().getSetting().localDirectory, relPath);

        if(source.isDirectory()){
            createEncryptedDirectory(security, relPath, source, result);
            return result;
        }


        byte[] path = relPath.getBytes(StandardCharsets.UTF_8);

        long dataLength = source.length();
        long tileDataLength = Program.instance().getSetting().preferedEncryptedTileLength - 32 - path.length;


        FileInputStream input = new FileInputStream(source);

        for(long writtenDataLength = 0; dataLength > writtenDataLength; writtenDataLength += tileDataLength){

            long partOffset = writtenDataLength;


            long partLength = source.length() - writtenDataLength;
            if(partLength > tileDataLength){
                partLength = tileDataLength;
            }
            String partHexName = createHashHex(relPath + partOffset);

            File partFile = new File(Program.instance().getSetting().tempDirectory, partHexName+".part"+partOffset);

            DataOutputStream output = new DataOutputStream(new FileOutputStream(partFile));

            try {
                output.writeLong(partLength);
                output.writeLong(partOffset);

                output.writeShort(path.length & 0xFFFF);
                output.write(path);

                byte[] buffer = new byte[1024];
                int len;
                long partWrittenLength = 0;
                while (partWrittenLength < tileDataLength) {

                    int bytesToRead = tileDataLength - partWrittenLength >= 1024 ? 1024 : (int) (tileDataLength - partWrittenLength);

                    len = input.read(buffer, 0, bytesToRead);

                    if (len == -1) {
                        //if no bytes can be read the rest of file will be filled with random bytes

                        len = bytesToRead;
                        random.nextBytes(buffer);
                    }

                    partWrittenLength += len;
                    output.write(buffer, 0, len);

                }

                output.flush();
            }catch(IOException e){
                output.close();
                input.close();
                throw e;
            }
            output.close();

            File encryptedPartFile = new File(Program.instance().getSetting().cloudDirectory, partFile.getName().replaceAll("\\..*", ".crypt"));

            try {
                security.encryptFile(partFile, encryptedPartFile);
            } catch (GeneralSecurityException e) {
                partFile.delete();
                Program.instance().criticalError("could not encrypt file "+relPath, e);
            } catch (IOException e) {
                output.close();
                input.close();
                throw e;
            }

            if(!partFile.delete()){
                Program.instance().warningError("could not delete temporary file: "+partFile.getAbsolutePath(), new Exception());
            }

            result.add(encryptedPartFile);

        }

        input.close();



        return result;


    }

    private static void createEncryptedDirectory(SecurityProvider security,String relPath, File source, ArrayList<File> result) throws IOException {

        Random random = new Random();

        byte[] path = relPath.getBytes(StandardCharsets.UTF_8);

        long tileDataLength = Program.instance().getSetting().preferedEncryptedTileLength - 32 - path.length;

        String partHexName = createHashHex(relPath + "d");

        File partFile = new File(Program.instance().getSetting().tempDirectory, partHexName+".d");

        DataOutputStream output = new DataOutputStream(new FileOutputStream(partFile));

        try {
            output.writeLong(-1);
            output.writeLong(-1);
            output.writeShort(path.length & 0xFFFF);
            output.write(path);

            byte[] buffer = new byte[1024];
            long partWrittenLength = 0;
            while (partWrittenLength < tileDataLength) {

                random.nextBytes(buffer);

                int len = tileDataLength-partWrittenLength>=buffer.length ? buffer.length : (int)(tileDataLength - partWrittenLength);

                partWrittenLength += buffer.length;
                output.write(buffer, 0, len);

            }

            output.flush();
        }catch(IOException e){
            output.close();
            throw e;
        }
        output.close();

        File encryptedPartFile = new File(Program.instance().getSetting().cloudDirectory, partFile.getName().replaceAll("\\..*", ".crypt"));

        try {
            security.encryptFile(partFile, encryptedPartFile);
        } catch (GeneralSecurityException e) {
            partFile.delete();
            Program.instance().criticalError("could not encrypt file "+relPath, e);
        } catch (IOException e) {
            output.close();
            throw e;
        }

        if(!partFile.delete()){
            Program.instance().warningError("could not delete temporary file: "+partFile.getAbsolutePath(), new Exception());
        }

        result.add(encryptedPartFile);
    }

    public static String fileNameHash(String name, boolean directory){
        if(directory){
            return createHashHex(name+"d");
        }else{
            return createHashHex(name+"0");
        }
    }

    private static String createHashHex(String string){
        byte[] hashBytes = SecurityProvider.sha256(string.getBytes(StandardCharsets.UTF_8));
        return Util.toHex(hashBytes);
     }

    public String getFilePath() {
        return filePath;
    }

    public boolean isDirectory() {
        return tileLength == -1 || tileOffset == -1;
    }
}
