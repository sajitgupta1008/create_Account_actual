package com.rccl.middleware.guest.accounts;

import org.apache.commons.lang3.StringUtils;

/**
 * An enumeration of available guest account statuses.
 * <p>
 * <li><b>Existing</b> - If account is already existing in IAM</li>
 * <li><b>DoesNotExist</b> - If account doesn't exists in both IAM and WebShopper</li>
 * <li><b>NeedsToBeMigrated</b> - If account doesn't exists in IAM but exists in WebShopper</li>
 * </p>
 */
public enum AccountStatusEnum {
    
    EXISTING("Exists"),
    DOES_NOT_EXIST("DoesNotExist"),
    NEEDS_TO_BE_MIGRATED("NeedsToBeMigrated");
    
    private String value;
    
    AccountStatusEnum(String value) {
        this.value = value;
    }
    
    public static AccountStatusEnum fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        
        for (AccountStatusEnum accountStatusEnum : AccountStatusEnum.values()) {
            if (accountStatusEnum.value.equalsIgnoreCase(value)) {
                return accountStatusEnum;
            }
        }
        
        return null;
    }
    
    public String value() {
        return this.value;
    }
}
