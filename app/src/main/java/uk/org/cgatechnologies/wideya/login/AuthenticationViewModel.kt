package uk.org.cgatechnologies.wideya.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.haroldadmin.cnradapter.NetworkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import uk.org.cgatechnologies.wideya.BuildConfig
import uk.org.cgatechnologies.wideya.Secrets
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.network.RetrofitBuilder
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.login.interfaces.AuthenticationApi
import uk.org.cgatechnologies.wideya.login.models.common.AuthenticationErrorStatus
import uk.org.cgatechnologies.wideya.login.models.common.AuthenticationRequest
import uk.org.cgatechnologies.wideya.login.models.registration.RegistrationResponse
import uk.org.cgatechnologies.wideya.login.models.registration.RegistrationResponseData
import uk.org.cgatechnologies.wideya.login.models.resetpassword.ResetPasswordResponse
import uk.org.cgatechnologies.wideya.login.models.resetpassword.ResetPasswordResponseData

/**
 * Created by Mohamad Abuzaid on 02/19/2023.
 */

private const val TAG: String = "AuthenticationViewModel"

class AuthenticationViewModel(private val application: Application) :
    AndroidViewModel(application) {

    private val authenticationRepository: AuthenticationRepository

    private val _registrationState = MutableSharedFlow<LatestRegistrationUiState?>(0)
    val registrationState: SharedFlow<LatestRegistrationUiState?> = _registrationState

    private val _resetPasswordState = MutableSharedFlow<LatestResetPasswordUiState?>(0)
    val resetPasswordState: SharedFlow<LatestResetPasswordUiState?> = _resetPasswordState

    private val _resetRequestsStatusState = MutableStateFlow<LatestResetPasswordUiState?>(null)
    val resetRequestsStatusState: StateFlow<LatestResetPasswordUiState?> = _resetRequestsStatusState

    private val _cancelResetPasswordState = MutableStateFlow<LatestResetPasswordUiState?>(null)
    val cancelResetPasswordState: StateFlow<LatestResetPasswordUiState?> = _cancelResetPasswordState

    init {
        Log.d(TAG, "initialise vm")

        val retrofit = RetrofitBuilder.getRetrofit(BuildConfig.SERVER_URL)
        val authenticationApi = retrofit.create(AuthenticationApi::class.java)
        this.authenticationRepository = AuthenticationRepository.getInstance(authenticationApi)
    }

    fun postRegistration(username: String, password: String) {
        val jsonObject = JSONObject()
        jsonObject.put("username", username)
        jsonObject.put("password", password)
        jsonObject.put(
            Constants.PREF_INSTALL_ID,
            Utils.getEncSharedPrefs(application)
                .getString(Constants.PREF_INSTALL_ID, "")
        )
        jsonObject.put("client_time", Utils.getISODateTimeUTC())
        //encrypt
        val encrypted = Utils.xorEncrypt(
            jsonObject.toString(),
            Secrets().getXorKey(Constants.PKG)
        )
        val registrationRequest = AuthenticationRequest(encrypted)

        viewModelScope.launch(Dispatchers.IO) {
            when (val response = authenticationRepository.postRegistration(registrationRequest)) {
                is NetworkResponse.Success -> {
                    if (response.response.isSuccessful) {
                        val registrationResponse: RegistrationResponse = response.body
                        val responseStatus = registrationResponse.status

                        if (responseStatus) {
                            //attempt to decrypt data
                            val jsonDataString = Utils.xorDecrypt(
                                registrationResponse.data,
                                Secrets().getXorKey(Constants.PKG)
                            )

                            val gson = Gson()
                            val jsonData = gson.fromJson(
                                jsonDataString,
                                RegistrationResponseData::class.java
                            )
                            //check if data is valid
                            if (jsonData.access_token.take(3) == "wdy") {
                                Utils.getEncSharedPrefs(application).edit()
                                    .putString(
                                        Constants.PREF_ACCESS_TOKEN,
                                        jsonData.access_token
                                    )
                                    .putString(Constants.PREF_USER_ID, jsonData.user_id)
                                    .putString(Constants.PREF_USERNAME, jsonData.username)
                                    .putString(Constants.PREF_LAST_USERNAME, jsonData.username)
                                    .putString(Constants.PREF_NAME, jsonData.name)
                                    .putString(
                                        Constants.PREF_SCOPE_SCHOOLS,
                                        jsonData.scope_schools
                                    )
                                    .putString(Constants.PREF_SCOPE_HASH, jsonData.scope_hash)
                                    .putString(
                                        Constants.PREF_SCOPE_CACHE_ID,
                                        jsonData.scope_cache_id
                                    )
                                    .putString(Constants.PREF_PASSPHRASE, jsonData.passphrase)
                                    .putBoolean(Constants.PREF_INITIAL_LOAD_REQUIRED_BOOL, true)
                                    .apply()

                                // copy some items to SharedPrefs too since its faster than EncSharedPrefs
                                Utils.getSharedPrefsState(application).edit()
                                    .putString(Constants.PREF_USER_ID, jsonData.user_id)
                                    .apply()

                                _registrationState.emit(
                                    LatestRegistrationUiState.Success(
                                        registrationResponse.message
                                    )
                                )

                            } else {
                                Log.d(TAG, "show error")
                                _registrationState.emit(
                                    LatestRegistrationUiState.Fail(
                                        AuthenticationErrorStatus.UNKNOWN_ERROR,
                                        registrationResponse.message
                                    )
                                )
                            }

                        } else {
                            _registrationState.emit(
                                LatestRegistrationUiState.Fail(
                                    AuthenticationErrorStatus.ACCESS_DENIED,
                                    registrationResponse.message
                                )
                            )
                        }

                    } else {
                        Log.e(TAG, response.response.code().toString())
                        _registrationState.emit(
                            LatestRegistrationUiState.Fail(
                                AuthenticationErrorStatus.NETWORK_ERROR,
                                response.response.code().toString()
                            )
                        )
                    }
                }
                is NetworkResponse.Error -> {
                    Log.e(TAG, response.error?.message.toString())
                    _registrationState.emit(LatestRegistrationUiState.Error(response.error))
                }
            }

        }
    }

    fun postResetRequestsStatus() {
        val jsonObject = JSONObject()
        jsonObject.put(
            Constants.PREF_INSTALL_ID,
            Utils.getEncSharedPrefs(application)
                .getString(Constants.PREF_INSTALL_ID, "")
        )
        //encrypt
        val encrypted = Utils.xorEncrypt(
            jsonObject.toString(),
            Secrets().getXorKey(Constants.PKG)
        )
        val resetStatusRequest = AuthenticationRequest(encrypted)

        viewModelScope.launch(Dispatchers.IO) {
            when (val response =
                authenticationRepository.postResetRequestsStatus(resetStatusRequest)) {
                is NetworkResponse.Success -> {
                    if (response.response.isSuccessful) {
                        val resetStatusResponse: ResetPasswordResponse = response.body
                        val responseStatus = resetStatusResponse.status

                        if (responseStatus) {
                            _resetRequestsStatusState.value =
                                LatestResetPasswordUiState.Success(resetStatusResponse.data)

                        } else {
                            _resetRequestsStatusState.value =
                                LatestResetPasswordUiState.Fail(
                                    AuthenticationErrorStatus.ACCESS_DENIED,
                                    resetStatusResponse.message
                                )
                        }

                    } else {
                        Log.e(TAG, response.response.code().toString())
                        _resetRequestsStatusState.value =
                            LatestResetPasswordUiState.Fail(
                                AuthenticationErrorStatus.NETWORK_ERROR,
                                response.response.code().toString()
                            )
                    }
                }
                is NetworkResponse.Error -> {
                    Log.e(TAG, response.error?.message.toString())
                    _resetRequestsStatusState.value =
                        LatestResetPasswordUiState.Error(response.error)
                }
            }

        }
    }

    fun postCancelResetRequest(username: String, requestId: String) {
        val jsonObject = JSONObject()
        jsonObject.put("username", username)
        jsonObject.put("id", requestId)
        jsonObject.put(
            Constants.PREF_INSTALL_ID,
            Utils.getEncSharedPrefs(application)
                .getString(Constants.PREF_INSTALL_ID, "")
        )
        //encrypt
        val encrypted = Utils.xorEncrypt(
            jsonObject.toString(),
            Secrets().getXorKey(Constants.PKG)
        )
        val cancelResetRequest = AuthenticationRequest(encrypted)

        viewModelScope.launch(Dispatchers.IO) {
            when (val response =
                authenticationRepository.postCancelResetRequest(cancelResetRequest)) {
                is NetworkResponse.Success -> {
                    if (response.response.isSuccessful) {
                        val cancelResetResponse: ResetPasswordResponse = response.body
                        val responseStatus = cancelResetResponse.status

                        if (responseStatus) {
                            _cancelResetPasswordState.value =
                                LatestResetPasswordUiState.Success(cancelResetResponse.data)

                        } else {
                            _cancelResetPasswordState.value =
                                LatestResetPasswordUiState.Fail(
                                    AuthenticationErrorStatus.ACCESS_DENIED,
                                    cancelResetResponse.message
                                )
                        }

                    } else {
                        Log.e(TAG, response.response.code().toString())
                        _cancelResetPasswordState.value =
                            LatestResetPasswordUiState.Fail(
                                AuthenticationErrorStatus.NETWORK_ERROR,
                                response.response.code().toString()
                            )
                    }
                }
                is NetworkResponse.Error -> {
                    Log.e(TAG, response.error?.message.toString())
                    _cancelResetPasswordState.value =
                        LatestResetPasswordUiState.Error(response.error)
                }
            }
        }
    }

    fun postResetPassword(username: String, password: String) {
        val jsonObject = JSONObject()
        jsonObject.put("username", username)
        jsonObject.put("password", password)
        jsonObject.put(
            Constants.PREF_INSTALL_ID,
            Utils.getEncSharedPrefs(application)
                .getString(Constants.PREF_INSTALL_ID, "")
        )
        //encrypt
        val encrypted = Utils.xorEncrypt(
            jsonObject.toString(),
            Secrets().getXorKey(Constants.PKG)
        )
        val resetPasswordRequest = AuthenticationRequest(encrypted)

        viewModelScope.launch(Dispatchers.IO) {
            when (val response = authenticationRepository.postResetPassword(resetPasswordRequest)) {
                is NetworkResponse.Success -> {
                    if (response.response.isSuccessful) {
                        val resetPasswordResponse: ResetPasswordResponse = response.body
                        val responseStatus = resetPasswordResponse.status

                        if (responseStatus) {
                            _resetPasswordState.emit(
                                LatestResetPasswordUiState.Success(resetPasswordResponse.data)
                            )

                        } else {
                            _resetPasswordState.emit(
                                LatestResetPasswordUiState.Fail(
                                    AuthenticationErrorStatus.ACCESS_DENIED,
                                    resetPasswordResponse.message
                                )
                            )
                        }

                    } else {
                        Log.e(TAG, response.response.code().toString())
                        _resetPasswordState.emit(
                            LatestResetPasswordUiState.Fail(
                                AuthenticationErrorStatus.NETWORK_ERROR,
                                response.response.code().toString()
                            )
                        )
                    }
                }
                is NetworkResponse.Error -> {
                    Log.e(TAG, response.error?.message.toString())
                    _resetPasswordState.emit(LatestResetPasswordUiState.Error(response.error))
                }
            }

        }
    }
}

sealed class LatestRegistrationUiState {
    data class Success(val message: String?) : LatestRegistrationUiState()

    data class Fail(val authenticationStatus: AuthenticationErrorStatus, val message: String) :
        LatestRegistrationUiState()

    data class Error(val exception: Throwable?) : LatestRegistrationUiState()
}

sealed class LatestResetPasswordUiState {
    data class Success(val requests: List<ResetPasswordResponseData>?) :
        LatestResetPasswordUiState()

    data class Fail(val authenticationStatus: AuthenticationErrorStatus, val message: String) :
        LatestResetPasswordUiState()

    data class Error(val exception: Throwable?) : LatestResetPasswordUiState()
}