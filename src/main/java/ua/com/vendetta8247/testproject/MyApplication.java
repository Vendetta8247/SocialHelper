package ua.com.vendetta8247.testproject;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKAccessTokenTracker;
import com.vk.sdk.VKSdk;

/**
 * Created by Y500 on 12.10.2016.
 */

public class MyApplication extends Application {
    VKAccessTokenTracker vkAccessTokenTracker = new VKAccessTokenTracker() {
        @Override
        public void onVKAccessTokenChanged(VKAccessToken oldToken, VKAccessToken newToken) { //в случае, если изменился пароль и токен недействителен
            if (newToken == null) {
            VKSdk.logout();
            }
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        VKSdk.initialize(this);                             //инициализация ВК
        FacebookSdk.sdkInitialize(getApplicationContext()); //инициализация Favebook
        AppEventsLogger.activateApp(this);
    }
}
