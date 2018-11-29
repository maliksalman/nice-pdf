package com.smalik.nicepdf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class EmbeddedResourceRepository {

    private HashMap<String, String> md5ToIdMap = new HashMap<>();
    private HashMap<String, EmbeddedResource> idToResourcesMap = new HashMap<>();

    private File rootDir;
    private File repositoryFile;
    private ObjectMapper jsonMapper;

    public EmbeddedResourceRepository(File rootDir) {
        this.rootDir = rootDir;
        this.repositoryFile = new File(rootDir, "repository.json");
        this.jsonMapper = new ObjectMapper();
    }

    public EmbeddedResource findEmbeddedResourceById(String id) {
        return idToResourcesMap.get(id);
    }

    public EmbeddedResource findEmbeddedResourceByMd5(String md5) {
        if (md5ToIdMap.containsKey(md5)) {
            return findEmbeddedResourceById(md5ToIdMap.get(md5));
        }
        return null;
    }

    public void addEmbeddedResource(EmbeddedResource resource, byte[] data) throws IOException {
        md5ToIdMap.put(resource.getMd5(), resource.getId());
        idToResourcesMap.put(resource.getId(), resource);

        File file = new File(rootDir, resource.getId() + ".data");
        FileUtils.writeByteArrayToFile(file, data);
    }

    public byte[] readEmbeddedResourceData(EmbeddedResource resource) throws IOException {
        File file = new File(rootDir, resource.getId() + ".data");
        return FileUtils.readFileToByteArray(file);
    }

    public String calculateMd5(byte[] data) {
        return DigestUtils.md5Hex(data);
    }

    public void persist() throws IOException {
        jsonMapper.writeValue(repositoryFile, idToResourcesMap);
    }

    public void load() throws IOException {
        if (repositoryFile.exists()) {
            idToResourcesMap = jsonMapper.readValue(repositoryFile, new TypeReference<HashMap<String, EmbeddedResource>>() {
            });

            md5ToIdMap.clear();
            for (EmbeddedResource resource : idToResourcesMap.values()) {
                md5ToIdMap.put(resource.getMd5(), resource.getId());
            }
        }
    }
}
