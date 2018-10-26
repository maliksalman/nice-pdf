package com.smalik.nicepdf;

import org.springframework.util.DigestUtils;

import java.util.UUID;

public class EmbeddedImage {

    private long length;
    private String id;
    private String md5;

    public EmbeddedImage(long length, String md5) {
        this.id = UUID.randomUUID().toString();
        this.length = length;
        this.md5 = md5;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public static String calculateMd5(byte[] data) {
        return DigestUtils.md5DigestAsHex(data);
    }
}
