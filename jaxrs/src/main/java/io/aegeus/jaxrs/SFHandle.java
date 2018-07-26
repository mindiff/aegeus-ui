package io.aegeus.jaxrs;

import io.nessus.ipfs.FHandle;

public class SFHandle {
    
    private String owner;
    private String path;
    private String cid;
    private boolean encrypted;
    
    public SFHandle() {
    }
    
    public SFHandle(String owner, String path, String cid, boolean encrypted) {
        this.owner = owner;
        this.path = path;
        this.cid = cid;
        this.encrypted = encrypted;
    }

    SFHandle(FHandle fhandle) {
        this.owner = fhandle.getOwner().getAddress();
        this.path = fhandle.getPath().toString();
        this.encrypted = fhandle.isEncrypted();
        this.cid = fhandle.getCid();
    }
    
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String toString() {
        return String.format("[cid=%s, owner=%s, path=%s]", cid, owner, path);
    }
}