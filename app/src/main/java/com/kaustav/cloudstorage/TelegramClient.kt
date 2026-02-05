package com.kaustav.cloudstorage

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File

object TelegramClient {
    private var client: Client? = null
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    // API Credentials
    private const val API_ID = 20110837
    private const val API_HASH = "b9658b136c2b71af2bdb7497649ace5c"

    fun initialize(context: Context) {
        if (client != null) return

        // Set log verbosity to avoid spam
        Client.execute(TdApi.SetLogVerbosityLevel(1))

        // Create client
        client = Client.create { `object` ->
            handleUpdate(`object`, context)
        }
    }

    private fun handleUpdate(`object`: TdApi.Object, context: Context) {
        if (`object` is TdApi.UpdateAuthorizationState) {
            onAuthorizationStateUpdated(`object`.authorizationState, context)
        }
    }

    private fun onAuthorizationStateUpdated(authorizationState: TdApi.AuthorizationState, context: Context) {
        when (authorizationState) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val parameters = TdApi.TdlibParameters()
                parameters.databaseDirectory = File(context.filesDir, "tdlib").absolutePath
                parameters.useMessageDatabase = true
                parameters.useSecretChats = true
                parameters.apiId = API_ID
                parameters.apiHash = API_HASH
                parameters.systemLanguageCode = "en"
                parameters.deviceModel = "Android"
                parameters.applicationVersion = "1.0"
                parameters.enableStorageOptimizer = true

                client?.send(TdApi.SetTdlibParameters(parameters), null)
            }
            is TdApi.AuthorizationStateWaitEncryptionKey -> {
                client?.send(TdApi.CheckDatabaseEncryptionKey(), null)
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                _authState.value = AuthState.WaitPhoneNumber
            }
            is TdApi.AuthorizationStateWaitCode -> {
                _authState.value = AuthState.WaitCode
            }
            is TdApi.AuthorizationStateWaitPassword -> {
                _authState.value = AuthState.WaitPassword
            }
            is TdApi.AuthorizationStateReady -> {
                _authState.value = AuthState.LoggedIn
            }
            is TdApi.AuthorizationStateLoggingOut -> {
                _authState.value = AuthState.LoggingOut
            }
            is TdApi.AuthorizationStateClosed -> {
                _authState.value = AuthState.Closed
            }
        }
    }

    fun sendPhoneNumber(phoneNumber: String) {
        client?.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), null)
    }

    fun checkCode(code: String) {
        client?.send(TdApi.CheckAuthenticationCode(code), null)
    }

    fun checkPassword(password: String) {
        client?.send(TdApi.CheckAuthenticationPassword(password), null)
    }

    fun uploadFile(filePath: String, completion: (Boolean) -> Unit) {
        // First get 'me' to find self chat (Saved Messages)
        client?.send(TdApi.GetMe()) { obj ->
            if (obj is TdApi.User) {
                // Create private chat with self
                client?.send(TdApi.CreatePrivateChat(obj.id, false)) { chatObj ->
                    if (chatObj is TdApi.Chat) {
                        val inputFile = TdApi.InputFileLocal(filePath)
                        // InputMessageDocument constructor: document, thumbnail, disableContentTypeDetection, caption
                        val content = TdApi.InputMessageDocument(
                            inputFile,
                            null,
                            false,
                            null
                        )

                        client?.send(TdApi.SendMessage(
                            chatObj.id,
                            0,
                            0,
                            null,
                            null,
                            content
                        )) { msg ->
                            completion(msg is TdApi.Message)
                        }
                    } else {
                        completion(false)
                    }
                }
            } else {
                completion(false)
            }
        }
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object WaitPhoneNumber : AuthState()
    object WaitCode : AuthState()
    object WaitPassword : AuthState()
    object LoggedIn : AuthState()
    object LoggingOut : AuthState()
    object Closed : AuthState()
}
