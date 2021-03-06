Google Cloud Java Client
==========================

Java idiomatic client for [Google Cloud Platform][cloud-platform] services.

[![Build Status](https://travis-ci.org/GoogleCloudPlatform/gcloud-java.svg?branch=master)](https://travis-ci.org/GoogleCloudPlatform/gcloud-java)
[![Coverage Status](https://coveralls.io/repos/GoogleCloudPlatform/gcloud-java/badge.svg?branch=master)](https://coveralls.io/r/GoogleCloudPlatform/gcloud-java?branch=master)
[![Maven](https://img.shields.io/maven-central/v/com.google.gcloud/gcloud-java.svg)]( https://img.shields.io/maven-central/v/com.google.gcloud/gcloud-java.svg)

-  [Homepage] (https://googlecloudplatform.github.io/gcloud-java/)
-  [API Documentation] (http://googlecloudplatform.github.io/gcloud-java/apidocs)

This client supports the following Google Cloud Platform services:

-  [Google Cloud Datastore] (#google-cloud-datastore)
-  [Google Cloud Storage] (#google-cloud-storage)

> Note: This client is a work-in-progress, and may occasionally
> make backwards-incompatible changes.

Quickstart
----------
Add this to your pom.xml file
```xml
<dependency>
  <groupId>com.google.gcloud</groupId>
  <artifactId>gcloud-java</artifactId>
  <version>0.0.9</version>
</dependency>
```

Example Applications
--------------------

- [`DatastoreExample`](https://github.com/GoogleCloudPlatform/gcloud-java/blob/master/gcloud-java-examples/src/main/java/com/google/gcloud/examples/DatastoreExample.java) - A simple command line interface for the Cloud Datastore
  - Read more about using this application on the [`gcloud-java-examples` docs page](http://googlecloudplatform.github.io/gcloud-java/apidocs/?com/google/gcloud/examples/DatastoreExample.html).
- [`StorageExample`](https://github.com/GoogleCloudPlatform/gcloud-java/blob/master/gcloud-java-examples/src/main/java/com/google/gcloud/examples/StorageExample.java) - A simple command line interface providing some of Cloud Storage's functionality
  - Read more about using this application on the [`gcloud-java-examples` docs page](http://googlecloudplatform.github.io/gcloud-java/apidocs/?com/google/gcloud/examples/StorageExample.html).

Authentication
--------------

There are multiple ways to authenticate to use Google Cloud services.

1. When using `gcloud-java` libraries from within Compute/App Engine, no additional authentication steps are necessary.
2. When using `gcloud-java` libraries elsewhere, there are two options:
  * [Generate a JSON service account key](https://cloud.google.com/storage/docs/authentication?hl=en#service_accounts).  Supply a path to the downloaded JSON credentials file when building the options supplied to datastore/storage constructor.
  * If running locally for development/testing, you can use use [Google Cloud SDK](https://cloud.google.com/sdk/?hl=en).  To use the SDK authentication, [download the SDK](https://cloud.google.com/sdk/?hl=en) if you haven't already.  Then login using the SDK (`gcloud auth login` in command line), and set your current project using `gcloud config set project PROJECT_ID`.

Google Cloud Datastore
----------------------

- [API Documentation][datastore-api]
- [Official Documentation][cloud-datastore-docs]

*Follow the [activation instructions][cloud-datastore-activation] to use the Google Cloud Datastore API with your project.*

#### Preview

Here is a code snippet showing a simple usage example from within Compute/App Engine.  Note that you must [supply credentials](#authentication) and a project ID if running this snippet elsewhere.

```java
import com.google.gcloud.datastore.Datastore;
import com.google.gcloud.datastore.DatastoreFactory;
import com.google.gcloud.datastore.DatastoreOptions;
import com.google.gcloud.datastore.DateTime;
import com.google.gcloud.datastore.Entity;
import com.google.gcloud.datastore.Key;
import com.google.gcloud.datastore.KeyFactory;

Datastore datastore = DatastoreFactory.instance().get(DatastoreOptions.getDefaultInstance());
KeyFactory keyFactory = datastore.newKeyFactory().kind(KIND);
Key key = keyFactory.newKey(keyName);
Entity entity = datastore.get(key);
if (entity == null) {
  entity = Entity.builder(key)
      .set("name", "John Do")
      .set("age", 30)
      .set("access_time", DateTime.now())
      .build();
  datastore.put(entity);
} else {
  System.out.println("Updating access_time for " + entity.getString("name"));
  entity = Entity.builder(entity)
      .set("access_time", DateTime.now())
      .build();
  datastore.update(entity);
}
```

Google Cloud Storage
----------------------

- [API Documentation][storage-api]
- [Official Documentation][cloud-storage-docs]

*Follow the [activation instructions][cloud-storage-activation] to use the Google Cloud Storage API with your project.*

#### Preview

Here is a code snippet showing a simple usage example from within Compute/App Engine.  Note that you must [supply credentials](#authentication) and a project ID if running this snippet elsewhere.

```java
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.gcloud.storage.Blob;
import com.google.gcloud.storage.Storage;
import com.google.gcloud.storage.StorageFactory;
import com.google.gcloud.storage.StorageOptions;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

Storage storage = StorageFactory.instance().get(StorageOptions.getDefaultInstance());
Blob blob = new Blob(storage, "bucket", "blob_name");
if (!blob.exists()) {
  storage2.create(blob.info(), "Hello, Cloud Storage!".getBytes(UTF_8));
} else {
  System.out.println("Updating content for " + blob.info().name());
  byte[] prevContent = blob.content();
  System.out.println(new String(prevContent, UTF_8));
  WritableByteChannel channel = blob.writer();
  channel.write(ByteBuffer.wrap("Updated content".getBytes(UTF_8)));
  channel.close();
}
```

Java Versions
-------------

Java 7 or above is required for using this client.

Testing
-------

This library provides tools to help write tests for code that uses gcloud-java services.

See [TESTING] to read more about using our testing helpers.

Versioning
----------

This library follows [Semantic Versioning] (http://semver.org/).

It is currently in major version zero (``0.y.z``), which means that anything
may change at any time and the public API should not be considered
stable.

Contributing
------------

Contributions to this library are always welcome and highly encouraged.

See [CONTRIBUTING] for more information on how to get started.

Please note that this project is released with a Contributor Code of Conduct. By participating in this project you agree to abide by its terms. See [Code of Conduct][code-of-conduct] for more information.

License
-------

Apache 2.0 - See [LICENSE] for more information.


[CONTRIBUTING]:https://github.com/GoogleCloudPlatform/gcloud-java/blob/master/CONTRIBUTING.md
[code-of-conduct]:https://github.com/GoogleCloudPlatform/gcloud-java/blob/master/CODE_OF_CONDUCT.md
[LICENSE]: https://github.com/GoogleCloudPlatform/gcloud-java/blob/master/LICENSE
[TESTING]: https://github.com/GoogleCloudPlatform/gcloud-java/blob/master/TESTING.md
[cloud-platform]: https://cloud.google.com/
[cloud-datastore]: https://cloud.google.com/datastore/docs
[cloud-datastore-docs]: https://cloud.google.com/datastore/docs
[cloud-datastore-activation]: https://cloud.google.com/datastore/docs/activate
[datastore-api]: http://googlecloudplatform.github.io/gcloud-java/apidocs/index.html?com/google/gcloud/datastore/package-summary.html

[cloud-pubsub]: https://cloud.google.com/pubsub/
[cloud-pubsub-docs]: https://cloud.google.com/pubsub/docs

[cloud-storage]: https://cloud.google.com/storage/
[cloud-storage-docs]: https://cloud.google.com/storage/docs/overview
[cloud-storage-create-bucket]: https://cloud.google.com/storage/docs/cloud-console#_creatingbuckets
[cloud-storage-activation]: https://cloud.google.com/storage/docs/signup
[storage-api]: http://googlecloudplatform.github.io/gcloud-java/apidocs/index.html?com/google/gcloud/storage/package-summary.html
