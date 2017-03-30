package com.fsck.k9.pEp;

import java.util.regex.Pattern;

public class EmailValidator {

    private static final String EMAIL_PATTERN = "[a-zA-Z0-9\\+\\._%\\-\\+]{1,256}" +
            "@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{1,25}" +
            ")+";

    //region Email
    public static Boolean isEmailValid(String email) {
        return validateEmailIsNotNull(email) && validateEmailFormat(email);
    }

    private static boolean validateEmailIsNotNull(String email) {
        return email != null;
    }

    private static boolean validateEmailFormat(String email) {
        Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);
        return emailPattern.matcher(email).matches();
    }
    //endregion
}
