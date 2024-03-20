package com.wacom.signature.sdk.example.utils;

import com.wacom.signature.sdk.Signature;

public class SignatureFormatInfo {

    public static enum Format {
        FSS, ISO_2007, ISO_2014, ISO_XML;
    }

    public static enum Encryption {
        NONE, ASYMMETRIC, SYMMETRIC;
    }

    private Signature signature;
    private Format format = Format.FSS;
    private Encryption encryption = Encryption.NONE;
    private String password;

    public SignatureFormatInfo() {

    }

    public SignatureFormatInfo(Signature signature, Format format, Encryption encryption, String password) {
        this.signature = signature;
        this.format = format;
        this.encryption = encryption;
        this.password = password;
    }

    public void setSignature(Signature signature) {
        this.signature = signature;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Signature getSignature() {
        return signature;
    }

    public Format getFormat() {
        return format;
    }

    public Encryption getEncryption() {
        return encryption;
    }

    public String getPassword() {
        return password;
    }
}
