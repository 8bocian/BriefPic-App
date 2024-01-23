package pl.summernote.summernote

import android.app.Application
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger

class BriefPicApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
    }
}