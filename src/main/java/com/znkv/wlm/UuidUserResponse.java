package com.znkv.wlm;

public class UuidUserResponse{
    public String id;
    public String name;

    public String getUuid() {
        return id;
    }

    public void setUuid(String uuid) {
        this.id = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
