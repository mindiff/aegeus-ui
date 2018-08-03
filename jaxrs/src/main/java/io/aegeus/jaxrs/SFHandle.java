package io.aegeus.jaxrs;

import java.nio.file.Path;

import io.nessus.ipfs.FHandle;

public class SFHandle {
    
    private String cid;
    private String owner;
    private String path;
    private String txId;
    private boolean encrypted;
    private boolean available;
    private boolean expired;
    private int attempts;
    private Long elapsed;
    
    public SFHandle() {
    }
    
    public SFHandle(String cid, String owner, String path, boolean available, boolean encrypted) {
        this.cid = cid;
        this.owner = owner;
        this.path = path;
        this.available = available;
        this.encrypted = encrypted;
    }

    SFHandle(FHandle fhandle) {
        Path path = fhandle.getPath();
        this.cid = fhandle.getCid();
        this.owner = fhandle.getOwner().getAddress();
        this.path = path != null ? path.toString() : null;
        this.txId = fhandle.getTxId();
        this.encrypted = fhandle.isEncrypted();
        this.available = fhandle.isAvailable();
        this.expired = fhandle.isExpired();
        this.attempts = fhandle.getAttempt();
        this.elapsed = fhandle.getElapsed();
    }
    
    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
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

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Long getElapsed() {
        return elapsed;
    }

    public void setElapsed(Long elapsed) {
        this.elapsed = elapsed;
    }

    public String toString() {
        return String.format("[cid=%s, owner=%s, path=%s, avl=%b, exp=%b, try=%d, time=%s]", 
                cid, owner, path, available, expired, attempts, elapsed);
    }
}