package com.mexator.camya.data

import android.util.Log
import com.mexator.camya.BuildConfig
import com.mexator.camya.data.model.User
import com.yandex.disk.rest.Credentials
import com.yandex.disk.rest.ResourcesArgs
import com.yandex.disk.rest.RestClient
import com.yandex.disk.rest.json.Resource
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.File

object ActualRepository {
    private const val TAG = "ActualRepository"

    // Dependencies
    private var diskClient: RestClient? = null

    var diskPath: String = ""

    private val compositeDisposable = CompositeDisposable()

    val diskAuthURL: String =
        "https://oauth.yandex.ru/authorize?response_type=token&client_id=${BuildConfig.ID}" +
                "&force_confirm=${if (BuildConfig.DEBUG) "no" else "yes"}"

    fun initDiskSdk(user: User, token: String) {
        diskClient = RestClient(Credentials(user.username, token))
    }

    fun getFoldersList(path: String): Single<List<Resource>> =
        Single.defer {
            Single.just(
                diskClient!!.getResources(
                    ResourcesArgs.Builder()
                        .setPath(path)
                        .setFields("_embedded")
                        .build()
                )
            )
        }
            .map { resource -> resource.resourceList.items }
            .map { list -> list.filter { item -> item.isDir } }

    fun createFolder(path: String) {
        val job = Completable.fromRunnable { diskClient!!.makeFolder(path) }
            .subscribeOn(Schedulers.io())
            .subscribe({}) { error -> Log.e(TAG, "Error creating directory", error) }
    }

    fun uploadFile(path: String) {
        val name = path.split("/").last()
        val job = Single.defer {
            Log.d(TAG, "Upload started")
            Single.just(
                diskClient!!.getUploadLink("$diskPath/$name", false)
            )
        }.flatMapCompletable {
            Completable.fromRunnable {
                diskClient!!.uploadFile(it, true, File(path), null)
            }
        }
            .subscribeOn(Schedulers.io())
            .subscribe {
                Log.d(TAG, "Upload completed: $diskPath/$name")
                File(path).delete()
            }

        compositeDisposable.add(job)
    }
}