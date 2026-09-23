package com.omnitune.app.models

import android.content.Context
import com.omnitune.app.constants.AccountChannelHandleKey
import com.omnitune.app.constants.AccountEmailKey
import com.omnitune.app.constants.AccountNameKey
import com.omnitune.app.constants.InnerTubeCookieKey
import com.omnitune.app.utils.SecurePreferenceCipher
import com.omnitune.app.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountSessionRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    val accountState: Flow<AccountSessionState> = context.dataStore.data
        .map { preferences ->
            AccountSessionStateMapper.fromStoredAccount(
                plainCookie = SecurePreferenceCipher.decryptOrPlain(preferences[InnerTubeCookieKey]),
                accountName = preferences[AccountNameKey],
                accountEmail = preferences[AccountEmailKey],
                channelHandle = preferences[AccountChannelHandleKey],
            )
        }
        .distinctUntilChanged()
}
