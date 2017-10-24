package com.rccl.middleware.guest.impl.accounts;

public enum GuestEventStatus {
    
    CREATE("create"),
    UPDATE("update");
    
    String value;
    
    GuestEventStatus(String value) {
        this.value = value;
    }
    
    public String value() {
        return this.value;
    }
}
