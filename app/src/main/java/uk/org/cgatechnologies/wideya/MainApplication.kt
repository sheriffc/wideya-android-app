package uk.org.cgatechnologies.wideya

import android.app.Application
import android.util.Log
import com.nullwire.trace.ExceptionHandler
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.utils.Utils

/**
 * Created by Mohamad Abuzaid on 3/18/2023.
 */
private const val TAG = "MainApplication"

class MainApplication : Application() {

    override fun onCreate() {
        Log.d(TAG, "MainApplication Going onCreate")
        super.onCreate()

        initializeApplication()
    }

    private fun initializeApplication() {
        Utils.generateInstallId(applicationContext)

        val sp = Utils.getEncSharedPrefs(applicationContext)
        sp.edit().putBoolean(Constants.PREF_APP_UPDATE_NAG_BOOL, true).apply()

        //register remote stacktrace
        if (sp.getBoolean("stack_trace_enabled", true)) {
            ExceptionHandler.register(
                applicationContext,
                """
                    ${BuildConfig.SERVER_URL + getString(R.string.stacktrace_url_suffix)}
                    ?version_code=${BuildConfig.VERSION_CODE}
                    &version_name=${BuildConfig.VERSION_NAME}
                    &app_db_version=${Utils.getDbVersionCode()}
                    &user_id=${sp.getString(Constants.PREF_USER_ID, "")}
                    &username=${sp.getString(Constants.PREF_USERNAME, "")}
                    &install_id=${sp.getString(Constants.PREF_INSTALL_ID, "")}
                    &scope_hash=${sp.getString(Constants.PREF_SCOPE_HASH, "")}
                    &scope_cache_id=${sp.getString(Constants.PREF_SCOPE_CACHE_ID, "")}
                    &application_id=${BuildConfig.APPLICATION_ID}
                    &system_os_release=${Utils.getRelease()}
                    &system_os_sdk=${Utils.getSdk()}
                    &system_os_version=${Utils.getSystemOsVersion()}
                    &system_os_username=${Utils.getSystemUsername()}
                    &system_os_name=${Utils.getSystemOsName()}
                    &system_os_arch=${Utils.getSystemOsArch()}
                    &client_time=${Utils.getISODateTimeUTC()}
                """.trimIndent().replace("\\s".toRegex(), "")
            )
        }
    }
}