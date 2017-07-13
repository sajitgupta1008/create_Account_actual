package com.rccl.middleware.guest.accounts;

/**
 * Enumeration of all possible user authentication status based on the user record.
 * <li><b>NEW_ACCOUNT_AUTHENTICATED</b> - If the guest record is coming directly from VDS.</li>
 * <li><b>NEW_ACCOUNT_TEMPORARY_PASSWORD</b> - If the guest record is coming directly from VDS
 * but supplied with temporary password.</li>
 * <li><b>LEGACY_ACCOUNT_VERIFIED</b> - If the guest record is coming from WebShopper and needs
 * to be migrated to VDS.</li>
 * <p>
 * Note that the values might change depending on the finalized requirements.
 */
public enum LoginStatusEnum {
    NEW_ACCOUNT_AUTHENTICATED("new_account_authenticated"),
    LEGACY_ACCOUNT_VERIFIED("legacy_account_verified"),
    NEW_ACCOUNT_TEMPORARY_PASSWORD("new_account_temporary_password");
    
    private String value;
    
    LoginStatusEnum(String value) {
        this.value = value;
    }
    
    public String value() {
        return this.value;
    }
}
