package nz.sounie.bucketlist.repository

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ListBucketsRequest
import aws.sdk.kotlin.services.s3.paginators.buckets
import aws.sdk.kotlin.services.s3.paginators.listBucketsPaginated
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

class BucketRepository(private val s3Client : S3Client, private val maxBuckets: Int = 100) {

    fun listBucketNames() : Flow<String> {
        val request = ListBucketsRequest {
            bucketRegion = "us-east-1"
            maxBuckets = this@BucketRepository.maxBuckets
        }

        return s3Client.listBucketsPaginated(request)
            .buckets()
            .mapNotNull { bucket -> bucket.name }
    }
}
