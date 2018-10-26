package com.smalik.nicepdf;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class EmbeddedImageRepository {

    private HashMap<String, String> md5ToIdMap = new HashMap<>();
    private HashMap<String, EmbeddedImage> idToEmbeddedImageMap = new HashMap<>();
    private File rootDir;

    public EmbeddedImageRepository(File rootDir) {
        this.rootDir = rootDir;
    }

    public EmbeddedImage findEmbeddedImageById(String id) {
        return idToEmbeddedImageMap.get(id);
    }

    public EmbeddedImage findEmbeddedImageByMd5(String md5) {
        if (md5ToIdMap.containsKey(md5)) {
            return findEmbeddedImageById(md5ToIdMap.get(md5));
        }
        return null;
    }

    public void addEmbeddedImage(EmbeddedImage image, byte[] data) throws IOException {
        md5ToIdMap.put(image.getMd5(), image.getId());
        idToEmbeddedImageMap.put(image.getId(), image);

        File file = new File(rootDir, image.getId() + ".data");
        FileUtils.writeByteArrayToFile(file, data);
    }

    public byte[] readEmbeddedImageData(EmbeddedImage image) throws IOException {
        File file = new File(rootDir, image.getId() + ".data");
        return FileUtils.readFileToByteArray(file);
    }
}
