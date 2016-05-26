package in.elanic.rxformvalidation;

import android.support.annotation.NonNull;

import rx.Observable;

/**
 * Created by Jay Rambhia on 5/26/16.
 */
public interface AvailabilityChecker {
    Observable<ValidationResult<String>> isEmailAvailable(@NonNull String email);
    Observable<ValidationResult<String>> isUsernameAvailable(@NonNull String email);

    ValidationResult<String> isEmailAvailableSync(@NonNull String email);
    ValidationResult<String> isUsernameAvailableSync(@NonNull String email);
}
