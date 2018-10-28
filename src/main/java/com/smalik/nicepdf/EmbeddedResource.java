package com.smalik.nicepdf;

import java.util.UUID;

public class EmbeddedResource {

    private long length;
    private String id;
    private String md5;
    private boolean dctDecode;
    private String type;

    public EmbeddedResource() {
    }

    public EmbeddedResource(long length, String md5, String type, boolean dctDecode) {
        this.id = UUID.randomUUID().toString();
        this.length = length;
        this.md5 = md5;
        this.dctDecode = dctDecode;
        this.type = type;
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

    public boolean isDctDecode() {
        return dctDecode;
    }

    public void setDctDecode(boolean dctDecode) {
        this.dctDecode = dctDecode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
