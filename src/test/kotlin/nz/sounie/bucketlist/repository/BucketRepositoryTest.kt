package nz.sounie.bucketlist.repository

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.Bucket
import aws.sdk.kotlin.services.s3.model.ListBucketsRequest
import aws.sdk.kotlin.services.s3.model.ListBucketsResponse
import aws.sdk.kotlin.services.s3.paginators.listBucketsPaginated
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.collections.emptyList

class BucketRepositoryTest {
    @Test
    suspend fun listBucketNamesEmpty() {
        val mockS3Client = mockk<S3Client>()
        // Feels ugly mocking the static extension function, but it is necessary to avoid having to mock the entire
        // flow of paginated responses and buckets.
        mockkStatic("aws.sdk.kotlin.services.s3.paginators.PaginatorsKt")
        every { mockS3Client.listBucketsPaginated(any<ListBucketsRequest>()) } returns flowOfBuckets(emptyList<Bucket>())

        val bucketRepository = BucketRepository(mockS3Client)

        bucketRepository.listBucketNames().count()

        verify { mockS3Client.listBucketsPaginated(any<ListBucketsRequest>()) }
    }

    @Test
    suspend fun listBucketNamesNonEmpty() {
        val mockS3Client = mockk<S3Client>()
        // Feels ugly mocking the static extension function, but it is necessary to avoid having to mock the entire
        // flow of paginated responses and buckets.
        mockkStatic("aws.sdk.kotlin.services.s3.paginators.PaginatorsKt")
        every { mockS3Client.listBucketsPaginated(any<ListBucketsRequest>()) } returns flowOfBuckets(listOf(Bucket {
            name = "bucket1"
        }, Bucket {
            name = "bucket2"
        }))

        val bucketRepository = BucketRepository(mockS3Client)

        val listOfBucketNames = bucketRepository.listBucketNames()
            .toCollection(mutableListOf())
        assertEquals(listOf("bucket1", "bucket2"), listOfBucketNames)

        verify { mockS3Client.listBucketsPaginated(any<ListBucketsRequest>()) }
    }
    
    @Test
    fun listBucketNamesThrowingExceptionOnFirstFlow() {
        val mockS3Client = mockk<S3Client>()
        mockkStatic("aws.sdk.kotlin.services.s3.paginators.PaginatorsKt")


        every { mockS3Client.listBucketsPaginated(any<ListBucketsRequest>()) } throws RuntimeException("Failed to list buckets")

        val bucketRepository = BucketRepository(mockS3Client)

        assertThrows(RuntimeException::class.java) {
            runBlocking {
                bucketRepository.listBucketNames().toCollection(mutableListOf())
            }
        }

        verify { mockS3Client.listBucketsPaginated(any<ListBucketsRequest>()) }
    }

    @Test
    fun listBucketNamesThrowingExceptionOnSubsequentFlow() {
        val mockS3Client = mockk<S3Client>()
        // Preserve the PaginatorsKt.listBucketsPaginated, but mock the underlying S3Client.listBuckets to return a flow
        // that emits a bucket and then throws an exception on the next emission. This allows us to test that the exception
        // is thrown during processing of the flow, rather than during the initial call to listBucketsPaginated.
        // So, we are mocking the s3Client listBuckets, not listBucketsPaginated, which means we need to mock the flow
        // of ListBucketsResponse that listBucketsPaginated would return, rather than mocking listBucketsPaginated directly.

        coEvery { mockS3Client.listBuckets(any<ListBucketsRequest>()) } returns
            ListBucketsResponse {
                buckets = listOf(Bucket {
                    name = "bucket1"

                })
                continuationToken = "something to trigger pagination"
            } andThenThrows RuntimeException("Failed to list buckets on subsequent call")


        // Artificially limit the number of buckets to 1 to ensure that we attempt to process more than one bucket,
        // which will trigger the exception.
        val bucketRepository = BucketRepository(mockS3Client, maxBuckets = 1)

        var listOfBucketNames: List<String>? = null
        assertThrows(RuntimeException::class.java) {
            runBlocking {
                bucketRepository.listBucketNames().onEach {
                    listOfBucketNames = listOfBucketNames?.plus(it) ?: listOf(it)
                }.collect()
            }
        }

        // We expect that the list of bucket names is null because the exception should be thrown before any bucket names are collected.
        assertThat(listOfBucketNames).containsExactly("bucket1")
        coVerify(exactly = 2) { mockS3Client.listBuckets(any<ListBucketsRequest>()) }
    }

    private fun flowOfBuckets(emptyList: Any): Flow<ListBucketsResponse> {
        return flowOf(ListBucketsResponse {
            buckets = emptyList as List<Bucket>?
        })
    }
}