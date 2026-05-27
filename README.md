Demonstrating an approach to obtaining a list of S3 buckets associated with an account from AWS.

Motivated by a discussion on a pull request at a previous job.

I had concerns about ballooning memory utilisation if / when the number of buckets involved would
be larger at some point in the future.

Later on I have been contemplating the implications of de-coupling the fetching of the data from
the processing. By grabbing a page of results at a time, we should also consider scenarios of
failure partway through.

The code in this repository is quite different to what was involved, as I am trying out AWS's 
Kotlin library rather than the AWS Java SDK.

Also, rather than using a Sequence, I'm trying out Flow as that is part of the API for pagination.
