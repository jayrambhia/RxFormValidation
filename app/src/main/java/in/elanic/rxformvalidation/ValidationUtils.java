package in.elanic.rxformvalidation;

import android.support.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Jay Rambhia on 5/26/16.
 */
public class ValidationUtils {

    private static String MOBILE_NUMBER_REGEX = "[7-9][0-9]{9}$";
    private static String TEXT_WITH_MOBILE_NUMBER_REGEX = ".*[7-9][0-9]{9}.*";
    private static String TEXT_WITH_EMAIL_ADDRESS_REGEX = ".*[a-zA-Z0-9\\+\\" +
            ".\\_\\%\\-\\+]{1,256}\\@[a-zA-Z0-9]{1,64}\\.[a-zA-Z0-9]{1,25}.*";

    private static String USERNAME_REGEX = "^[a-zA-Z][a-zA-Z._0-9]{2,19}$";
    private static String TEXT_WITH_FOUR_CONSECUTIVE_NUMBERS_REGEX = ".*[0-9]{5,}.*";

    public static boolean isValidMobileNumber(String number) {
        Pattern mPattern = Pattern.compile(MOBILE_NUMBER_REGEX);
        Matcher matcher = mPattern.matcher(number);
        return matcher.find();
    }

    public static ValidationResult<String> isValidUsername(String username) {
        if (username.isEmpty()) {
            return ValidationResult.failure(null, username);
        }

        if (username.length() < 3) {
            return ValidationResult.failure("username should have 3 or more characters", username);
        }

        Pattern mPattern = Pattern.compile(USERNAME_REGEX);
        Matcher matcher = mPattern.matcher(username);
        boolean isValid = matcher.find();

        if (isValid) {
            return ValidationResult.success(username);
        }

        return ValidationResult.failure("username should contain only alphanumeric characters", username);
    }

    public static boolean containsFourConsecutiveNumbers(String text) {
        Pattern mPattern = Pattern.compile(TEXT_WITH_FOUR_CONSECUTIVE_NUMBERS_REGEX);
        Matcher matcher = mPattern.matcher(text);
        return matcher.find();
    }

    public static boolean containsMobileNumber(String text) {
        Pattern mPattern = Pattern.compile(TEXT_WITH_MOBILE_NUMBER_REGEX);
        Matcher matcher = mPattern.matcher(text);
        return matcher.find();
    }

    public static ValidationResult<String> isValidEmailAddress(@NonNull String text) {
        if (text.isEmpty()) {
            return ValidationResult.failure(null, text);
        }

        Pattern mPattern = Pattern.compile(TEXT_WITH_EMAIL_ADDRESS_REGEX);
        Matcher matcher = mPattern.matcher(text);
        boolean isValid = matcher.find();

        if (isValid) {
            return ValidationResult.success(text);
        }

        return ValidationResult.failure("Please enter correct email address", text);
    }

}
