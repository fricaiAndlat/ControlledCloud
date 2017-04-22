package de.diavololoop.security;

import de.diavololoop.Program;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.Map;

/**
 * Created by Peer on 21.04.2017.
 */
public class SunSecurityProvider extends SecurityProvider {

    private final SecretKeySpec key;
    private final SecureRandom random;


    public SunSecurityProvider(String token){
        removeCryptographyRestrictions();

        random = new SecureRandom();
        key = new SecretKeySpec(sha256( token.getBytes(StandardCharsets.UTF_8 )), "AES");
    }

    @Override
    public void decryptFile(File source, File target) throws IOException, GeneralSecurityException {
        FileInputStream input = new FileInputStream(source);
        FileOutputStream output = new FileOutputStream(target);

        byte[] ivBytes = new byte[16];

        if(input.read(ivBytes, 0, ivBytes.length) != ivBytes.length){
            System.err.println("no IV given: "+source.getAbsolutePath());
            input.close();
            output.close();
            return;
        }

        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            cipher.init(Cipher.DECRYPT_MODE, key, iv);

            byte[] buffer = new byte[1024];
            int len;

            while((len = input.read(buffer)) != -1){

                byte[] raw = cipher.update(buffer, 0, len);
                output.write(raw, 0, raw.length);

            }

            byte[] raw = cipher.doFinal();
            output.write(raw, 0, raw.length);

            output.flush();
            output.close();
            input.close();

        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Program.instance().criticalError("could not use AES/CBC/PKCS5Padding", e);
        } catch (InvalidAlgorithmParameterException e) {
            Program.instance().criticalError("invalid use of IV", e);
        } catch (InvalidKeyException e) {
            Program.instance().criticalError("use of a invalid generated key", e);
        } catch (GeneralSecurityException e) {
            input.close();
            output.close();
            target.delete();
            throw e;
        }
    }

    @Override
    public void encryptFile(File source, File target) throws IOException, GeneralSecurityException {

        FileInputStream input = new FileInputStream(source);
        FileOutputStream output = new FileOutputStream(target);

        byte[] ivBytes = new byte[16];
        random.nextBytes(ivBytes);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        output.write(ivBytes, 0, ivBytes.length);

        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            byte[] buffer = new byte[1024];
            int len;

            while((len = input.read(buffer)) != -1){

                byte[] raw = cipher.update(buffer, 0, len);
                output.write(raw, 0, raw.length);

            }

            byte[] raw = cipher.doFinal();
            output.write(raw, 0, raw.length);

            output.flush();
            output.close();
            input.close();

        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Program.instance().criticalError("could not use AES/CBC/PKCS5Padding", e);
        } catch (InvalidAlgorithmParameterException e) {
            Program.instance().criticalError("invalid use of IV", e);
        } catch (InvalidKeyException e) {
            Program.instance().criticalError("use of a invalid generated key", e);
        } catch (GeneralSecurityException e) {
            output.close();
            input.close();
            target.delete();
            throw e;
        }

    }

    public static byte[] sha256(byte[] data){
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Program.instance().criticalError("sha256 is not supported", e);
        }
        return digest.digest(data);
    }




    private static void removeCryptographyRestrictions() {
        if (!isRestrictedCryptography()) {
            return;
        }
        try {
        /*
         * Do the following, but with reflection to bypass access checks:
         *
         * JceSecurity.isRestricted = false;
         * JceSecurity.defaultPolicy.perms.clear();
         * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
         */
            final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
            final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
            final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");

            final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
            isRestrictedField.setAccessible(true);
            final Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(isRestrictedField, isRestrictedField.getModifiers() & ~Modifier.FINAL);
            isRestrictedField.set(null, false);

            final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
            defaultPolicyField.setAccessible(true);
            final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);

            final Field perms = cryptoPermissions.getDeclaredField("perms");
            perms.setAccessible(true);
            ((Map<?, ?>) perms.get(defaultPolicy)).clear();

            final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            defaultPolicy.add((Permission) instance.get(null));

        } catch (final Exception e) {
            Program.instance().criticalError("could not unlock AES-256", e);
        }
    }

    private static boolean isRestrictedCryptography() {
        // This simply matches the Oracle JRE, but not OpenJDK.
        return "Java(TM) SE Runtime Environment".equals(System.getProperty("java.runtime.name"));
    }

}
