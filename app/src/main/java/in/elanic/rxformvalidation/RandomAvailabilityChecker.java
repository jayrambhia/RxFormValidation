package in.elanic.rxformvalidation;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func0;

/**
 * Created by Jay Rambhia on 5/26/16.
 */
public class RandomAvailabilityChecker implements AvailabilityChecker {

    private static final String TAG = "RandAvailabilityChecker";
    private Random random;

    public RandomAvailabilityChecker() {
        random = new Random();
    }

    @Override
    public Observable<ValidationResult<String>> isEmailAvailable(@NonNull final String email) {
        return Observable.defer(new Func0<Observable<ValidationResult<String>>>() {
            @Override
            public Observable<ValidationResult<String>> call() {
                return Observable.just(isEmailAvailableSync(email));
            }
        }).delay(1200, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<ValidationResult<String>> isUsernameAvailable(@NonNull final String username) {
        return Observable.defer(new Func0<Observable<ValidationResult<String>>>() {
            @Override
            public Observable<ValidationResult<String>> call() {
                return Observable.just(isUsernameAvailableSync(username));
            }
        }).delay(3000, TimeUnit.MILLISECONDS);
    }

    @Override
    public ValidationResult<String> isEmailAvailableSync(@NonNull String email) {
        int rand = random.nextInt(12);
        if (rand > 4) {
            return ValidationResult.success(email);
        }

        return ValidationResult.failure("Email is already taken", email);
    }

    @Override
    public ValidationResult<String> isUsernameAvailableSync(@NonNull String username) {
        int rand = random.nextInt(12);
        if (rand > 4) {
            return ValidationResult.success(username);
        }

        return ValidationResult.failure("Username is already taken", username);
    }
}
