Demonstrating an approach to obtaining a list of S3 buckets associated with an account from AWS.

Motivated by a discussion on a pull request at a previous job.

I had concerns about ballooning memory utilisation if / when the number of buckets involved would
be larger at some point in the future.

Later on I have been contemplating the implications of de-coupling the fetching of the data from
the processing. By grabbing a page of results at a time, we should also consider scenarios of
failure partway through.

The code in this repository is quite different to what was involved, as I am trying out AWS's 
Kotlin library and integration tests involving chaos engineering with Localstack.

Also, rather than using a Sequence, I'm trying out Flow.

Let's start off with some calculations
listBuckets can return up to 10,000 results per response.
Each bucket has a region, name, creation date, and ARN.
Let's approximate that as 50 bytes,
50 * 10,000 = 500,000 = about half a megabyte