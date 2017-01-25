package in.elanic.rxformvalidation;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func3;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    @Bind(R.id.email_view) EditText emailView;
    @Bind(R.id.username_view) EditText usernameView;
    @Bind(R.id.phone_view) EditText phoneView;
    @Bind(R.id.button) Button submitButton;

    private Subscription _subscription;
    private AvailabilityChecker availabilityChecker;

    // API subscriptions
    private Subscription emailApiSubscription;
    private Subscription usernameApiSubscription;

    // API subjects
    private PublishSubject<Boolean> emailSubject;
    private PublishSubject<Boolean> usernameSubject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        availabilityChecker = new RandomAvailabilityChecker();
        setupObservables5();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (_subscription != null) {
            _subscription.unsubscribe();
        }

        if (emailSubject != null) {
            emailSubject.onCompleted();
        }

        if (usernameSubject != null) {
            usernameSubject.onCompleted();
        }

        cancelEmailApiCall();
        cancelUsernameApiCall();
    }

    // No validations. Just testing, if we are getting the data or not
    private void setupObservables() {
        Observable<String> emailObservable = RxHelper.getTextWatcherObservable(emailView);
        Observable<String> usernameObservable = RxHelper.getTextWatcherObservable(usernameView);
        Observable<String> phoneObservable = RxHelper.getTextWatcherObservable(phoneView);

        // combineLatest -> It will start emitting once all the observables start emitting
        // If you add email (no event emitted). After that if you add username, still there won't
        // be any event emitted. Once you start adding phone, it will start emitting the events
        // Now, even if you remove the phone number and edit email, it will keep emitting events.
        // So, until all the observables start emitting events, combineLatest will not emit any events.
        _subscription = Observable.combineLatest(emailObservable, usernameObservable,
                phoneObservable, new Func3<String, String, String, Boolean>() {
            @Override
            public Boolean call(String email, String username, String phone) {
                Log.i(TAG, "email: " + email + ", username: " + username + ", phone: " + phone);
                return false;
            }
        }).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                Log.i(TAG, "submit button enabled: " + aBoolean);
            }
        });
    }

    // Validate input data with debounce
    private void setupObservables1() {

        // Debounce is coming in very handy here.
        // What I had understood before is that if I use debounce, it will emit event after the give
        // time period regardless of other events.
        // But now I am realizing that this is not the case.
        // Let's say debounce interval is 200 milliseconds. Once an event is emitted, RxJava clock starts
        // ticking. Once 200 ms is up, debounce operator will emit that event.
        // One more event comes to debounce and it will start the clock for 200 ms. If another event comes
        // in 100 ms, debounce operator will reset the clock and start to count 200 ms again.
        // So let's say if you continue emitting events at 199 ms intervals, this debounce operator
        // will never emit any event.

        // Also, debounce by default goes on Scheduler thread, so it is important to add observeOn
        // and observe it on main thread.

        Observable<Boolean> emailObservable = RxHelper.getTextWatcherObservable(emailView)
                .debounce(800, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        Log.i(TAG, "validate email: " + s);
                        ValidationResult result = validateEmail(s);
                        emailView.setError(result.getReason());
                        return result.isValid();
                    }
                });

        Observable<Boolean> usernameObservable = RxHelper.getTextWatcherObservable(usernameView)
                .debounce(800, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        ValidationResult result = validateUsername(s);
                        usernameView.setError(result.getReason());
                        return result.isValid();
                    }
                });

        Observable<Boolean> phoneObservable = RxHelper.getTextWatcherObservable(phoneView)
                .debounce(800, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        ValidationResult result = validatePhone(s);
                        phoneView.setError(result.getReason());
                        return result.isValid();
                    }
                });

        _subscription = Observable.combineLatest(usernameObservable, emailObservable, phoneObservable, new Func3<Boolean, Boolean, Boolean, Boolean>() {
                @Override
                public Boolean call(Boolean validUsername, Boolean validEmail, Boolean validPhone) {
                    Log.i(TAG, "email: " + validEmail + ", username: " + validUsername + ", phone: " + validPhone);
                    return validUsername && validEmail && validPhone;
                }
            }).subscribe(new Action1<Boolean>() {
                @Override
                public void call(Boolean aBoolean) {
                    submitButton.setEnabled(aBoolean);
                }
            });
    }

    // Validate input data (pattern and from server) with debounce
    private void setupObservables2() {
        Observable<Boolean> emailObservable = RxHelper.getTextWatcherObservable(emailView)
                .debounce(800, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .map(new Func1<String, ValidationResult<String>>() {
                    @Override
                    public ValidationResult<String> call(String s) {
                        Log.i(TAG, "validate email: " + s);
                        return validateEmail(s);

                    }
                }).map(new Func1<ValidationResult<String>, ValidationResult<String>>() {
                    @Override
                    public ValidationResult<String> call(ValidationResult<String> result) {
                        if (!result.isValid()) {
                            return result;
                        }

                        return availabilityChecker.isEmailAvailableSync(result.getData());
                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<ValidationResult<String>, Boolean>() {
                    @Override
                    public Boolean call(ValidationResult<String> result) {
                        Log.i(TAG, "email validation result: " + result.isValid() + " email: " + result.getData());
                        emailView.setError(result.getReason());
                        return result.isValid();
                    }
                });

        Observable<Boolean> usernameObservable = RxHelper.getTextWatcherObservable(usernameView)
                .debounce(800, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .map(new Func1<String, ValidationResult<String>>() {
                    @Override
                    public ValidationResult<String> call(String s) {
                        Log.i(TAG, "validate username: " + s);
                        return validateUsername(s);

                    }
                }).flatMap(new Func1<ValidationResult<String>, Observable<ValidationResult<String>>>() {
                    @Override
                    public Observable<ValidationResult<String>> call(ValidationResult<String> result) {
                        if (!result.isValid()) {
                            return Observable.just(result);
                        }

                        return availabilityChecker.isUsernameAvailable(result.getData());
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<ValidationResult<String>, Boolean>() {
                    @Override
                    public Boolean call(ValidationResult<String> result) {
                        Log.i(TAG, "username validation result: " + result.isValid() + " usrname: " + result.getData());
                        usernameView.setError(result.getReason());
                        return result.isValid();
                    }
                });

        Observable<Boolean> phoneObservable = RxHelper.getTextWatcherObservable(phoneView)
                .debounce(800, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        ValidationResult result = validatePhone(s);
                        phoneView.setError(result.getReason());
                        return result.isValid();
                    }
                });

        _subscription = Observable.combineLatest(usernameObservable, emailObservable, phoneObservable, new Func3<Boolean, Boolean, Boolean, Boolean>() {
            @Override
            public Boolean call(Boolean validUsername, Boolean validEmail, Boolean validPhone) {
                Log.i(TAG, "email: " + validEmail + ", username: " + validUsername + ", phone: " + validPhone);
                return validUsername && validEmail && validPhone;
            }
        }).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                submitButton.setEnabled(aBoolean);
            }
        });
    }

    // This works fine and cancels api call if text is changed but the problem is cancellation
    // occurs only after we get the event from debounce. This will take min of 800 ms and can
    // go on for long. So this is also not an ideal scenario.

    private void setupObservables3() {

        emailSubject = PublishSubject.create();
        usernameSubject = PublishSubject.create();

        RxHelper.getTextWatcherObservable(emailView)
                .debounce(800, TimeUnit.MILLISECONDS)
                .map(new Func1<String, ValidationResult<String>>() {
                    @Override
                    public ValidationResult<String> call(String s) {
                        cancelEmailApiCall();
                        Log.i(TAG, "validate email: " + s);
                        return validateEmail(s);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ValidationResult<String>>() {
                    @Override
                    public void call(ValidationResult<String> result) {

                        Log.i(TAG, "Email validation result: " + result.isValid() + ", " + result.getData());

                        if (!result.isValid()) {
                            emailView.setError(result.getReason());
                            emailSubject.onNext(false);
                            return;
                        }

                        callApiToValidateEmail(result.getData());
                    }
                });

        RxHelper.getTextWatcherObservable(usernameView)
                .debounce(800, TimeUnit.MILLISECONDS)
                .map(new Func1<String, ValidationResult<String>>() {
                    @Override
                    public ValidationResult<String> call(String s) {
                        cancelUsernameApiCall();
                        Log.i(TAG, "validate username: " + s);
                        return validateUsername(s);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ValidationResult<String>>() {
                    @Override
                    public void call(ValidationResult<String> result) {
                        Log.i(TAG, "Username validation result: " + result.isValid() + ", " + result.getData());

                        if (!result.isValid()) {
                            usernameView.setError(result.getReason());
                            usernameSubject.onNext(false);
                            return;
                        }

                        callApiToValidateUsername(result.getData());
                    }
                });

        Observable<Boolean> phoneObservable = RxHelper.getTextWatcherObservable(phoneView)
                .debounce(800, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        ValidationResult result = validatePhone(s);
                        phoneView.setError(result.getReason());
                        return result.isValid();
                    }
                });

        _subscription = Observable.combineLatest(usernameSubject, emailSubject, phoneObservable, new Func3<Boolean, Boolean, Boolean, Boolean>() {
            @Override
            public Boolean call(Boolean validUsername, Boolean validEmail, Boolean validPhone) {
                Log.i(TAG, "email: " + validEmail + ", username: " + validUsername + ", phone: " + validPhone);
                return validUsername && validEmail && validPhone;
            }
        }).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                submitButton.setEnabled(aBoolean);
            }
        });
    }

    // Now, our problem was our api call wouldn't be cancelled until debounce emits an event.
    // So, we just catch the event before debounce and cancel the api call ourselves.
    private void setupObservables4() {

        emailSubject = PublishSubject.create();
        usernameSubject = PublishSubject.create();

        RxHelper.getTextWatcherObservable(emailView)
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        cancelEmailApiCall();
                        return s;
                    }
                })
                .debounce(800, TimeUnit.MILLISECONDS)
                .map(new Func1<String, ValidationResult<String>>() {
                    @Override
                    public ValidationResult<String> call(String s) {
                        Log.i(TAG, "validate email: " + s);
                        return validateEmail(s);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ValidationResult<String>>() {
                    @Override
                    public void call(ValidationResult<String> result) {

                        Log.i(TAG, "Email validation result: " + result.isValid() + ", " + result.getData());

                        if (!result.isValid()) {
                            emailView.setError(result.getReason());
                            emailSubject.onNext(false);
                            return;
                        }

                        callApiToValidateEmail(result.getData());
                    }
                });

        RxHelper.getTextWatcherObservable(usernameView)
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        cancelUsernameApiCall();
                        return s;
                    }
                })
                .debounce(800, TimeUnit.MILLISECONDS)
                .map(new Func1<String, ValidationResult<String>>() {
                    @Override
                    public ValidationResult<String> call(String s) {
                        Log.i(TAG, "validate username: " + s);
                        return validateUsername(s);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ValidationResult<String>>() {
                    @Override
                    public void call(ValidationResult<String> result) {
                        Log.i(TAG, "Username validation result: " + result.isValid() + ", " + result.getData());

                        if (!result.isValid()) {
                            usernameView.setError(result.getReason());
                            usernameSubject.onNext(false);
                            return;
                        }

                        callApiToValidateUsername(result.getData());
                    }
                });

        Observable<Boolean> phoneObservable = RxHelper.getTextWatcherObservable(phoneView)
                .debounce(800, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        ValidationResult result = validatePhone(s);
                        phoneView.setError(result.getReason());
                        return result.isValid();
                    }
                });

        _subscription = Observable.combineLatest(usernameSubject, emailSubject, phoneObservable, new Func3<Boolean, Boolean, Boolean, Boolean>() {
            @Override
            public Boolean call(Boolean validUsername, Boolean validEmail, Boolean validPhone) {
                Log.i(TAG, "email: " + validEmail + ", username: " + validUsername + ", phone: " + validPhone);
                return validUsername && validEmail && validPhone;
            }
        }).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                submitButton.setEnabled(aBoolean);
            }
        });
    }

    private void setupObservables5() {

        final PublishSubject<Observable<ValidationResult<String>>> emailSubject = PublishSubject.create();

        emailSubject.subscribe(new Action1<Observable<ValidationResult<String>>>() {
            @Override
            public void call(Observable<ValidationResult<String>> validationResultObservable) {
                Log.d(TAG, "emailsubject subscriber. New call");
            }
        });

        final Observable<ValidationResult<String>> emailApiObservable = Observable.switchOnNext(emailSubject);

        RxHelper.getTextWatcherObservable(emailView)
                .debounce(800, TimeUnit.MILLISECONDS)
                .doOnNext(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        Log.d(TAG, "start email validation: " + s);
                    }
                })
                .map(new Func1<String, ValidationResult<String>>() {
                    @Override
                    public ValidationResult<String> call(String s) {
                        return validateEmail(s);
                    }
                })
                .doOnNext(new Action1<ValidationResult<String>>() {
                    @Override
                    public void call(ValidationResult<String> result) {
                        Log.d(TAG, "regex validation done: " + result.getData() + ", " + result.isValid() + ", " + result.getReason());
                    }
                })
                .flatMap(new Func1<ValidationResult<String>, Observable<ValidationResult<String>>>() {
                    @Override
                    public Observable<ValidationResult<String>> call(ValidationResult<String> result) {
                        if (!result.isValid()) {
                            Log.e(TAG, "email pattern validation failed: " + result.getData() + ", " + result.getReason());
                            return Observable.just(result);
                        }

                        Log.d(TAG, "calling API call via onNext on emailSubject: " + result.getData());
                        emailSubject.onNext(availabilityChecker.isEmailAvailable(result.getData()));
//                        return Observable.switchOnNext(emailSubject);
                        return emailApiObservable;
                    }
                })
                .doOnNext(new Action1<ValidationResult<String>>() {
                    @Override
                    public void call(ValidationResult<String> result) {
                        Log.d(TAG, "Got API validation result for: " + result.getData());
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ValidationResult<String>>() {
                    @Override
                    public void call(ValidationResult<String> result) {
                        Log.d(TAG, "email validation result: " + result.getData() + ", " + result.isValid() + ", " + result.getReason());
                        emailView.setError(result.getReason());
                    }
                });


        /*RxHelper.getTextWatcherObservable(emailView)
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        cancelEmailApiCall();
                        return s;
                    }
                })
                .debounce(800, TimeUnit.MILLISECONDS)
                .flatMap(new Func1<String, Observable<ValidationResult<String>>>() {
                    @Override
                    public Observable<ValidationResult<String>> call(String s) {
                        ValidationResult<String> result = validateEmail(s);
                        if (!result.isValid()) {
                            return Observable.just(result);
                        }

                        return callApiToValidateEmail2(result.getData());
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ValidationResult<String>>() {
                    @Override
                    public void call(ValidationResult<String> result) {

                    }
                });
*/
    }

    private ValidationResult<String> validateEmail(@NonNull String email) {
        return ValidationUtils.isValidEmailAddress(email);
    }

    private ValidationResult<String> validateUsername(@NonNull String username) {
        return ValidationUtils.isValidUsername(username);
    }

    private ValidationResult validatePhone(@NonNull String phone) {
        if (phone.isEmpty()) {
            return ValidationResult.failure(null, phone);
        }

        boolean isValid = ValidationUtils.isValidMobileNumber(phone);
        if (isValid) {
            return ValidationResult.success(phone);
        }

        return ValidationResult.failure("Phone should be exactly 10 numbers", phone);
    }

    private void cancelEmailApiCall() {
        if (emailApiSubscription != null && !emailApiSubscription.isUnsubscribed()) {
            Log.i(TAG, "unsubscribe email api subscription");
            emailApiSubscription.unsubscribe();
            emailApiSubscription = null;
        }
    }

    private Observable<ValidationResult<String>> callApiToValidateEmail2(@NonNull String email) {
        cancelEmailApiCall();

        Observable<ValidationResult<String>> observable = availabilityChecker.isEmailAvailable(email)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        emailApiSubscription = observable.subscribe(new Action1<ValidationResult<String>>() {
            @Override
            public void call(ValidationResult<String> result) {
                emailView.setError(result.getReason());
                emailSubject.onNext(result.isValid());
            }
        });

        return observable;
    }

    /*private Observable<Observable<ValidationResult<String>>> getEmailApiObservable() {
        return Observable.create(new Observable.OnSubscribe<Observable<ValidationResult<String>>>() {
            @Override
            public void call(Subscriber<? super Observable<ValidationResult<String>>> subscriber) {
                subscriber.onNext(availabilityChecker.isEmailAvailable(""));
            }
        });
    }*/

    private void callApiToValidateEmail(@NonNull String email) {
        cancelEmailApiCall();

        emailApiSubscription = availabilityChecker.isEmailAvailable(email)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ValidationResult<String>>() {
                    @Override
                    public void call(ValidationResult<String> result) {
                        emailView.setError(result.getReason());
                        emailSubject.onNext(result.isValid());
                    }
                });
    }

    private void cancelUsernameApiCall() {
        if (usernameApiSubscription != null && !usernameApiSubscription.isUnsubscribed()) {
            Log.i(TAG, "unsubscribe username api subscription");
            usernameApiSubscription.unsubscribe();
            usernameApiSubscription = null;
        }
    }

    private void callApiToValidateUsername(@NonNull String username) {
        cancelUsernameApiCall();

        usernameApiSubscription = availabilityChecker.isUsernameAvailable(username)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ValidationResult<String>>() {
                    @Override
                    public void call(ValidationResult<String> result) {
                        Log.i(TAG, "username api validation: " + result.getData() + ", " + result.isValid());
                        usernameView.setError(result.getReason());
                        usernameSubject.onNext(result.isValid());
                    }
                });
    }
}
