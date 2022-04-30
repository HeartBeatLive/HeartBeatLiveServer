package com.munoon.heartbeatlive.server.user.firebase

import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object FirebaseUtils {
    suspend fun <T> ApiFuture<T>.await(): T? = suspendCoroutine { cont ->
        ApiFutures.addCallback(this, object : ApiFutureCallback<T> {
            override fun onFailure(t: Throwable) = cont.resumeWithException(t)
            override fun onSuccess(result: T?) = cont.resume(result)
        }, Dispatchers.IO.asExecutor())
    }
}