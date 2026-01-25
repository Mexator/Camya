package com.mexator.camya.data

import android.util.Log
import com.mexator.camya.BuildConfig
import com.mexator.camya.session.CredentialsStorage
import com.yandex.disk.rest.Credentials
import com.yandex.disk.rest.ResourcesArgs
import com.yandex.disk.rest.RestClient
import com.yandex.disk.rest.json.Resource
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.File

class YandexDiskRepository(
    private val credentialsStorage: CredentialsStorage
) {

    var diskPath: String = ""

    private val compositeDisposable = CompositeDisposable()

    val diskAuthURL: String =
        "https://oauth.yandex.ru/authorize?response_type=token&client_id=${BuildConfig.ID}" +
                "&force_confirm=${if (BuildConfig.DEBUG) "no" else "yes"}"

    fun getFoldersList(path: String): Single<List<Resource>> =
        Single.fromCallable {
            requireDiskClient().getResources(
                ResourcesArgs.Builder()
                    .setPath(path)
                    .setFields("_embedded")
                    .build()
            )
        }
            .map { resource -> resource.resourceList.items }
            .map { list -> list.filter { item -> item.isDir } }

    fun createFolder(path: String): Completable {
        return Completable.fromRunnable { requireDiskClient().makeFolder(path) }
            .subscribeOn(Schedulers.io())
            .doOnError { error -> Log.e(TAG, "Error creating directory", error) }
    }

    fun uploadFile(path: String) {
        val name = path.split("/").last()
        val job = Single.fromCallable {
            Log.d(TAG, "Upload started")
            requireDiskClient().getUploadLink("$diskPath/$name", false)
        }.flatMapCompletable {
            Completable.fromRunnable {
                requireDiskClient().uploadFile(it, true, File(path), null)
            }
        }
            .subscribeOn(Schedulers.io())
            .subscribe {
                Log.d(TAG, "Upload completed: $diskPath/$name")
                File(path).delete()
            }

        compositeDisposable.add(job)
    }

    private fun requireDiskClient(): RestClient {
        return getDiskClient() ?: throw UnauthorizedException()
    }

    private fun getDiskClient(): RestClient? {
        val credentials = credentialsStorage.getCachedCredentials()
        return credentials?.let {
            RestClient(Credentials(credentials.username, credentials.token))
        }
    }

    class UnauthorizedException : Exception()

    private companion object {
        const val TAG = "YandexDiskRepository"
    }
}