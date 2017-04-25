package de.diavololoop.util;

import java.nio.charset.StandardCharsets;

/**
 * Created by Peer on 25.04.2017.
 */
public class Util {

    private final static byte[] hexChars = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70};
    private final static byte[] hexCharsBack = new byte[71];

    static {

        for(byte i = 0; i < hexChars.length; ++i){
            hexCharsBack[hexChars[i]] = i;
        }

    }

    public static String toHex(byte[] value){
        byte[] asciiHex = new byte[value.length * 2];

        for(int i = 0; i < value.length; ++i){
            asciiHex[2*i]   = hexChars[  (value[i] >> 4)&0xF  ];
            asciiHex[2*i+1] = hexChars[   value[i]      &0xF  ];
        }

        return new String(asciiHex, StandardCharsets.US_ASCII);
    }

    public static byte[] fromHex(String token) {
        byte[] tokenBytes = token.getBytes(StandardCharsets.US_ASCII);

        if(token.length()%2 != 0){
            throw new IllegalArgumentException("hex string length must be even");
        }

        byte[] value = new byte[token.length()/2];

        for(int i = 0; i < value.length; ++i){
            value[i] = (byte) (hexCharsBack[tokenBytes[2*i]]<<4 | hexCharsBack[tokenBytes[2*i + 1]]);
        }

        return value;


    }
}
