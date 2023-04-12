package com.particlesdevs.photoncamera.gallery.ui.fragments

import android.content.Context
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File

class MultiUploaderS3 {
    fun s3ClientInitialization(context: Context): AmazonS3 {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val poolId: String? = pref.getString("aws_pool_id_config", "")
        val regionId: String? = pref.getString("aws_region_id_config", "")
        val cognitoCachingCredentialsProvider = CognitoCachingCredentialsProvider(
            context,
            poolId,
            Regions.fromName(regionId)
        )
        return AmazonS3Client(
            cognitoCachingCredentialsProvider,

            )
    }

    private fun transferUtility(context: Context): Single<TransferUtility?> {
        return Single.create { emitter ->
            emitter.onSuccess(
                TransferUtility(s3ClientInitialization(context), context)
            )
        }
    }

    fun uploadMultiple(fileToKeyUploads: MutableMap<String, File>, context: Context): Completable {
        return transferUtility(context)
            .flatMapCompletable { transferUtility ->
                Observable.fromIterable(fileToKeyUploads.entries)
                    .flatMapCompletable { entry ->
                        uploadSingle(
                            transferUtility,
                            entry.value,
                            entry.key,
                            context
                        )
                    }
            }
    }


    private fun uploadSingle(
        transferUtility: TransferUtility,
        aLocalFile: File?,
        toRemoteKey: String?,
        context: Context
    ): Completable {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
//            Toast.makeText(getContext(), "Amazon S3 is Clicked", Toast.LENGTH_LONG).show();
        val bucketName = pref.getString("aws_bucket_name", "")

        return Completable.create { emitter ->
            transferUtility.upload(bucketName, toRemoteKey, aLocalFile)
                .setTransferListener(object : TransferListener {
                    override fun onStateChanged(
                        id: Int,
                        state: TransferState
                    ) {

                        if (TransferState.FAILED == state) {
                            emitter.onError(Exception("Transfer state was FAILED."))
                        } else if (TransferState.COMPLETED == state) {

                            emitter.onComplete()
                        }
                    }

                    override fun onProgressChanged(
                        id: Int,
                        bytesCurrent: Long,
                        bytesTotal: Long
                    ) {
                    }

                    override fun onError(id: Int, exception: Exception) {
                        Toast.makeText(context, exception.message, Toast.LENGTH_SHORT).show()
                        emitter.onError(exception)
                    }
                })
        }
    }

}