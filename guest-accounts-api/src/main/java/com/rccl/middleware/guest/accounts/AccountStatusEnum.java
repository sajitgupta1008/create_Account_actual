package com.rccl.middleware.guest.accounts;

/**
 * Enumeration of available guest account statuses:
 *
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

    public String value() {
        return this.value;
    }
}
