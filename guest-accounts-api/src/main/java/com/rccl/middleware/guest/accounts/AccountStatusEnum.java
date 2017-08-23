package com.rccl.middleware.guest.accounts;

import org.apache.commons.lang3.StringUtils;

/**
 * Enumeration of available guest account statuses:
 * <p>
 * <li><b>Existing</b> - If account is already existing in IAM</li>
 * <li><b>DoesNotExist</b> - If account doesn't exists in both IAM and WebShopper</li>
 * <li><b>NeedsToBeMigrated</b> - If account doesn't exists in IAM but exists in WebShopper</li>
 */
public enum AccountStatusEnum {
    EXISTING("Exists"),
    DOESTNOTEXIST("DoesNotExist"),
    NEEDSTOBEMIGRATED("NeedsToBeMigrated");
    
    private String value;
    
    AccountStatusEnum(String value) {
        this.value = value;
    }
    
    public static AccountStatusEnum fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        
        try {
            for (AccountStatusEnum accountStatusEnum : AccountStatusEnum.values()) {
                if (accountStatusEnum.value.equalsIgnoreCase(value)) {
                    return accountStatusEnum;
                }
            }
        } catch (Exception e) {
            // No-op
        }
        
        return null;
    }
    
    public String value() {
        return this.value;
    }
}
