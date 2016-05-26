package in.elanic.rxformvalidation;

import android.support.annotation.Nullable;

/**
 * Created by Jay Rambhia on 5/26/16.
 */
public class ValidationResult<T> {

    private boolean valid;
    private String reason;
    private T data;

    public static <T> ValidationResult<T> success(T t) {
        return new ValidationResult<>(true, null, t);
    }

    public static <T> ValidationResult<T> failure(@Nullable String reason, T t) {
        return new ValidationResult<>(false, reason, t);
    }

    private ValidationResult(boolean valid, @Nullable String reason, T t) {
        this.valid = valid;
        this.reason = reason;
        this.data = t;
    }

    public boolean isValid() {
        return valid;
    }

    @Nullable
    public String getReason() {
        return reason;
    }

    public T getData() {
        return data;
    }
}
