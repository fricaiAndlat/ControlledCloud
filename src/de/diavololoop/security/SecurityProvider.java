package de.diavololoop.security;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Created by Peer on 21.04.2017.
 */
public abstract class SecurityProvider {

    public abstract void decryptFile(File source, File target) throws IOException, GeneralSecurityException;
    public abstract void encryptFile(File source, File target) throws IOException, GeneralSecurityException;


    public static SecurityProvider getDefault(String token){
        return new SunSecurityProvider(token);
    }

    public static byte[] sha256(byte[] data){
        return SunSecurityProvider.sha256(data);
    }

}
