package uk.org.cgatechnologies.wideya.common.data

object Constants {

    const val PKG = "uk.org.cgatechnologies.wideya"

    const val DB_FILENAME = "wideya.db"

    const val ATTENDANCE_PRESENT_ID = "present"

    const val ATTENDANCE_ABSENT_ID = "absent"

    const val ATTENDANCE_LATE_ID = "late"

    const val ABSENT_REASON_TEACHER_LIST_NAME ="absent_reason_teacher"

    const val NO_SCHOOL_REASON_LIST_NAME = "no_school_reason"

    const val NO_SCHOOL_OTHER_ITEM_ID = "other_no_school"

    const val EMPTY_FIELD_VALIDATION = "Please fill (required)"

    const val TEACHER_ENTITY_ID = "teacher"

    const val LEARNER_ENTITY_ID = "learner"

    //Biometric Methods
    const val BIOMETRIC_METHOD_PHOTO_ID = "photo"
    const val BIOMETRIC_METHOD_FINGERPRINT_ID = "fingerprint"

    //shared prefs
    const val SP_FILENAME_SETTINGS = "settings"
    const val PREF_SEND_CRASH_REPORTS_BOOL = "send_crash_reports"
    const val PREF_BACKGROUND_SYNC_BOOL = "background_sync_enabled"
    const val PREF_INSTALL_ID = "install_id"
    const val PREF_ACCESS_TOKEN = "access_token"
    const val PREF_USER_ID = "user_id"
    const val PREF_USERNAME = "username"
    const val PREF_LAST_USERNAME = "last_username"
    const val PREF_NAME = "name"
    const val PREF_SCOPE_SCHOOLS = "scope_schools"
    const val PREF_SCHOOL_ID = "school_uuid"
    const val PREF_SCOPE_HASH = "scope_hash"
    const val PREF_SCOPE_CACHE_ID = "scope_cache_id"
    const val PREF_INITIAL_LOAD_REQUIRED_BOOL = "initial_load_required"
    const val PREF_AC_YEAR_INT = "ac_year_int" // int
    const val PREF_AC_YEAR_NAME = "ac_year_name" // string
    const val PREF_APP_UPDATE_NAG_BOOL = "update_nag" //bool
    const val PREF_S3_KEY ="s3_key"
    const val PREF_S3_SECRET ="s3_secret"
    const val PREF_S3_REGION ="s3_region"
    const val PREF_S3_BUCKET ="s3_bucket"
    const val PREF_LOCATION_LAT = "location_lat"
    const val PREF_LOCATION_LNG = "location_lon"
    const val PREF_LOCATION_DATETIME_ACQUIRED = "location_datetime_acquired"
    const val PREF_PASSPHRASE = "passphrase"

    // shared prefs for state
    const val SP_FILENAME_STATE = "state"
    const val PREF_SYNC_TOTAL_RECORD_COUNT_INT = "sync_total_record_count"
    const val PREF_SYNC_TOTAL_FILE_COUNT_INT = "sync_total_file_count"

    //workmanager
    const val TOKEN_RELATED_WORKER_TAG = "token_related_worker"
    const val WORKER_SYNC_GROUP_TAG = "worker_sync_group_tag"

    const val WORKER_SYNC = "sync_worker"
    const val WORKER_DOWNLOAD_SYNC_TAG = "sync_download"
    const val WORKER_INITIAL_DOWNLOAD_SYNC_TAG = "sync_download_initial"
    const val WORKER_PERIODIC_SYNC_TAG = "periodic_sync"
    const val WORKER_CHECK_IN_ONETIME_TAG = "onetime_check_in"
    const val WORKER_CHECK_IN_TAG = "periodic_check_in"
    const val WORKER_UPLOAD_SYNC_TAG = "sync_upload"
    const val WORKER_UPLOAD_DB_ONETIME_TAG = "onetime_upload_db"
    const val WORKER_CHECK_APP_UPDATE_ONETIME_TAG = "onetime_check_app_update"

    const val WORKER_DOWNLOAD_SYNC = "download_worker"
    const val WORKER_INITIAL_DOWNLOAD_SYNC = "sync_download_initial_worker"
    const val WORKER_UPLOAD_SYNC = "upload_worker"
    const val WORKER_CHECK_IN = "check_in_worker"
    const val WORKER_CHECK_IN_ONETIME = "onetime_check_in_worker"
    const val WORKER_UPLOAD_DB_ONETIME = "onetime_upload_db_worker"
    const val WORKER_CHECK_APP_UPDATE_ONETIME = "onetime_check_app_update_worker"
    const val WORKER_UPLOAD_FP_SYNC = "upload_fp_worker"
    const val WORKER_UPLOAD_TEACHER_ATT_PHOTOS_SYNC = "upload_teacher_attendance_photos_worker"
    const val WORKER_UPLOAD_TEACHER_PROFILE_PHOTOS_SYNC = "upload_profile_photos_worker"
    const val WORKER_NO_SCHOOL_PREFIX = "no_school_"

    //app update
    const val APP_THIS_VERSION_CODE = "this_version_code"
    const val APP_NEW_VERSION_CODE = "update_new_version_code"
    const val APP_DESCRIPTION = "update_description"
    const val APP_FORCE_UPDATE = "force_update"
    const val APP_URI = "update_uri"
    const val APP_UPDATE_LAST_CHECKED = "update_last_checked"

    //notifications
    const val CHANNEL_ID = "wideya_sync"
    const val NOTIFY_SYNC_ID = 1

    //file paths
    const val PATH_TMP = "tmp"
    const val PATH_TEACHER_ATT_PHOTOS = "teacher_attendance_photos"
    const val PATH_TEACHER_PROFILE_PHOTOS = "teacher_photos"
    const val PATH_FINGERPRINTS = "fp"

    //Location Manager
    const val TIME_INTERVAL = (15 * 60 * 1000).toLong()
    const val DISTANCE_ACCURACY = 5.0f

}