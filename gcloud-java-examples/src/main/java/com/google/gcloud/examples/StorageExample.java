/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gcloud.examples;

import com.google.gcloud.AuthCredentials;
import com.google.gcloud.AuthCredentials.ServiceAccountAuthCredentials;
import com.google.gcloud.RetryParams;
import com.google.gcloud.spi.StorageRpc.Tuple;
import com.google.gcloud.storage.BatchRequest;
import com.google.gcloud.storage.BatchResponse;
import com.google.gcloud.storage.Blob;
import com.google.gcloud.storage.BlobInfo;
import com.google.gcloud.storage.BlobReadChannel;
import com.google.gcloud.storage.BlobWriteChannel;
import com.google.gcloud.storage.Bucket;
import com.google.gcloud.storage.BucketInfo;
import com.google.gcloud.storage.Storage;
import com.google.gcloud.storage.Storage.ComposeRequest;
import com.google.gcloud.storage.Storage.CopyRequest;
import com.google.gcloud.storage.Storage.SignUrlOption;
import com.google.gcloud.storage.StorageFactory;
import com.google.gcloud.storage.StorageOptions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * An example of using the Google Cloud Storage.
 * <p>
 * This example demonstrates a simple/typical storage usage.
 * <p>
 * Steps needed for running the example:
 * <ol>
 * <li>login using gcloud SDK - {@code gcloud auth login}.</li>
 * <li>compile using maven - {@code mvn compile}</li>
 * <li>run using maven -
 * {@code mvn exec:java -Dexec.mainClass="com.google.gcloud.examples.StorageExample"
 * -Dexec.args="[<project_id>] list [<bucket>]| info [<bucket> [<file>]]|
 *  download <bucket> <path> [local_file]| upload <local_file> <bucket> [<path>]|
 *  delete <bucket> <path>+| cp <from_bucket> <from_path> <to_bucket> <to_path>|
 *  compose <bucket> <from_path>+ <to_path>| update_metadata <bucket> <file> [key=value]*|
 *  sign_url <service_account_private_key_file> <service_account_email> <bucket> <path>"}
 * </li>
 * </ol>
 *
 * The first parameter is an optional project_id (logged-in project will be used if not supplied).
 * Second parameter is a Storage operation (list, delete, compose,...) to demonstrate the its
 * usage. Any other arguments are specific to the operation.
 * See each action's run method for the specific Storage interaction.
 */
public class StorageExample {

  private static final Map<String, StorageAction> ACTIONS = new HashMap<>();

  private static abstract class StorageAction<T> {

    abstract void run(Storage storage, T request) throws Exception;

    abstract T parse(Storage storage, String... args) throws Exception;

    protected String params() {
      return "";
    }
  }

  private static abstract class BlobAction extends StorageAction<Blob> {

    @Override
    Blob parse(Storage storage, String... args) {
      if (args.length != 2) {
        throw new IllegalArgumentException();
      }
      return new Blob(storage, args[0], args[1]);
    }

    @Override
    public String params() {
      return "<bucket> <path>";
    }
  }

  private static abstract class BlobsAction extends StorageAction<Blob[]> {

    @Override
    Blob[] parse(Storage storage, String... args) {
      if (args.length < 2) {
        throw new IllegalArgumentException();
      }
      Blob[] blobs = new Blob[args.length - 1];
      for (int i = 1; i < args.length; i++) {
        blobs[i - 1] = new Blob(storage, args[0], args[i]);
      }
      return blobs;
    }

    @Override
    public String params() {
      return "<bucket> <path>+";
    }
  }

  /**
   * This class demonstrates how to retrieve Bucket or Blob metadata.
   * If more than one blob is supplied a Batch operation would be used to get all blobs metadata
   * in a single RPC.
   *
   * @see <a href="https://cloud.google.com/storage/docs/json_api/v1/objects/get">Objects: get</a>
   */
  private static class InfoAction extends BlobsAction {
    @Override
    public void run(Storage storage, Blob... blobs) {
      if (blobs.length == 1) {
        if (blobs[0].info().name().isEmpty()) {
          // get Bucket
          Bucket bucket = new Bucket(storage, blobs[0].info().bucket());
          System.out.println("Bucket info: " + bucket.reload().info());
        } else {
          // get Blob
          System.out.println("Blob info: " + blobs[0].reload().info());
        }
      } else {
        // use batch to get multiple blobs.
        BatchRequest.Builder batch = BatchRequest.builder();
        for (Blob blob : blobs) {
          batch.get(blob.info().bucket(), blob.info().name());
        }
        BatchResponse response = storage.apply(batch.build());
        for (BatchResponse.Result<BlobInfo> result : response.gets()) {
          System.out.println(result.get());
        }
      }
    }

    @Override
    Blob[] parse(Storage storage, String... args) {
      if (args.length < 2) {
        return new Blob[] {new Blob(storage, args[0], "")};
      }
      return super.parse(storage, args);
    }

    @Override
    public String params() {
      return "<bucket> [<path>+]";
    }
  }

  /**
   * This class demonstrates how to delete a blob.
   * If more than one blob is supplied a Batch operation would be used to delete all requested
   * blobs in a single RPC.
   *
   * @see <a href="https://cloud.google.com/storage/docs/json_api/v1/objects/delete">Objects: delete</a>
   */
  private static class DeleteAction extends BlobsAction {
    @Override
    public void run(Storage storage, Blob... blobs) {
      if (blobs.length == 1) {
        boolean wasDeleted = blobs[0].delete();
        if (wasDeleted) {
          System.out.println("Blob " + blobs[0].info() + " was deleted");
        }
      } else {
        // use batch operation
        BatchRequest.Builder batch = BatchRequest.builder();
        for (Blob blob : blobs) {
          batch.delete(blob.info().bucket(), blob.info().name());
        }
        int index = 0;
        BatchResponse response = storage.apply(batch.build());
        for (BatchResponse.Result<Boolean> result : response.deletes()) {
          if (result.get()) {
            // request order is maintained
            System.out.println("Blob " + blobs[index].info() + " was deleted");
          }
          index++;
        }
      }
    }
  }

  /**
   * This class demonstrates how to list buckets or a bucket's blobs.
   *
   * @see <a href="https://cloud.google.com/storage/docs/json_api/v1/objects/list">Objects: list</a>
   */
  private static class ListAction extends StorageAction<String> {

    @Override
    String parse(Storage storage, String... args) {
      if (args.length == 0) {
        return null;
      }
      if (args.length == 1) {
        return args[0];
      }
      throw new IllegalArgumentException();
    }

    @Override
    public void run(Storage storage, String bucketName) {
      if (bucketName == null) {
        // list buckets
        for (BucketInfo b : storage.list()) {
          System.out.println(b);
        }
      } else {
        // list a bucket's blobs
        Bucket bucket = new Bucket(storage, bucketName);
        for (Blob b : bucket.list()) {
          System.out.println(b.info());
        }
      }
    }

    @Override
    public String params() {
      return "[<bucket>]";
    }
  }

  /**
   * This class demonstrates how to create a new Blob or to update its content.
   *
   * @see <a href="https://cloud.google.com/storage/docs/json_api/v1/objects/insert">Objects: insert</a>
   */
  private static class UploadAction extends StorageAction<Tuple<Path, Blob>> {
    @Override
    public void run(Storage storage, Tuple<Path, Blob> tuple) throws Exception {
      run(storage, tuple.x(), tuple.y());
    }

    private void run(Storage storage, Path uploadFrom, Blob blob) throws IOException {
      if (Files.size(uploadFrom) > 1_000_000) {
        // When content is not available or large (1MB or more) it is recommended
        // to write it in chunks via the blob's channel writer.
        try (BlobWriteChannel writer = blob.writer()) {
          byte[] buffer = new byte[1024];
          try (InputStream input = Files.newInputStream(uploadFrom)) {
            int limit;
            while ((limit = input.read(buffer)) >= 0) {
              try {
                writer.write(ByteBuffer.wrap(buffer, 0, limit));
              } catch (Exception ex) {
                ex.printStackTrace();
              }
            }
          }
        }
      } else {
        byte[] bytes = Files.readAllBytes(uploadFrom);
        // create the blob in one request.
        storage.create(blob.info(), bytes);
      }
      System.out.println("Blob was created");
    }

    @Override
    Tuple<Path, Blob> parse(Storage storage, String... args) throws IOException {
      if (args.length < 2 || args.length > 3) {
        throw new IllegalArgumentException();
      }
      Path path = Paths.get(args[0]);
      String contentType = Files.probeContentType(path);
      String blob = args.length < 3 ? path.getFileName().toString() : args[2];
      BlobInfo info = BlobInfo.builder(args[1], blob).contentType(contentType).build();
      return Tuple.of(path, new Blob(storage, info));
    }

    @Override
    public String params() {
      return "<local_file> <bucket> [<path>]";
    }
  }

  /**
   * This class demonstrates how read a blob's content.
   * The example will dump the content to a local file if one was given or write
   * it to stdout otherwise.
   *
   * @see <a href="https://cloud.google.com/storage/docs/json_api/v1/objects/get">Objects: get</a>
   */
  private static class DownloadAction extends StorageAction<Tuple<Blob, Path>> {

    @Override
    public void run(Storage storage, Tuple<Blob, Path> tuple) throws IOException {
      run(storage, tuple.x(), tuple.y());
    }

    private void run(Storage storage, Blob blob, Path downloadTo) throws IOException {
      blob = blob.reload();
      if (!blob.exists()) {
        System.out.println("No such object");
        return;
      }
      PrintStream writeTo = System.out;
      if (downloadTo != null) {
        writeTo = new PrintStream(new FileOutputStream(downloadTo.toFile()));
      }
      if (blob.info().size() < 1_000_000) {
        // Blob is small read all its content in one request
        byte[] content = blob.content();
        writeTo.write(content);
      } else {
        // When Blob size is big or unknown use the blob's channel reader.
        try (BlobReadChannel reader = blob.reader()) {
          WritableByteChannel channel = Channels.newChannel(writeTo);
          ByteBuffer bytes = ByteBuffer.allocate(64 * 1024);
          while (reader.read(bytes) > 0) {
            bytes.flip();
            channel.write(bytes);
            bytes.clear();
          }
        }
      }
      if (downloadTo == null) {
        writeTo.println();
      } else {
        writeTo.close();
      }
    }

    @Override
    Tuple<Blob, Path> parse(Storage storage, String... args) {
      if (args.length < 2 || args.length > 3) {
        throw new IllegalArgumentException();
      }
      Path path;
      if (args.length > 2) {
        path = Paths.get(args[2]);
        if (Files.isDirectory(path)) {
          path = path.resolve(Paths.get(args[1]).getFileName());
        }
      } else {
        path = null;
      }
      return Tuple.of(new Blob(storage, args[0], args[1]), path);
    }

    @Override
    public String params() {
      return "<bucket> <path> [local_file]";
    }
  }

  /**
   * This class demonstrates how to use the copy command.
   *
   * @see <a href="https://cloud.google.com/storage/docs/json_api/v1/objects/copy">Objects: copy</a>
   */
  private static class CopyAction extends StorageAction<CopyRequest> {
    @Override
    public void run(Storage storage, CopyRequest request) {
      BlobInfo copiedBlobInfo = storage.copy(request);
      System.out.println("Copied " + copiedBlobInfo);
    }

    @Override
    CopyRequest parse(Storage storage, String... args) {
      if (args.length != 4) {
        throw new IllegalArgumentException();
      }
      return CopyRequest.of(args[0], args[1], BlobInfo.builder(args[2], args[3]).build());
    }

    @Override
    public String params() {
      return "<from_bucket> <from_path> <to_bucket> <to_path>";
    }
  }

  /**
   * This class demonstrates how to use the compose command.
   *
   * @see <a href="https://cloud.google.com/storage/docs/json_api/v1/objects/compose">Objects: compose</a>
   */
  private static class ComposeAction extends StorageAction<ComposeRequest> {
    @Override
    public void run(Storage storage, ComposeRequest request) {
      BlobInfo composedBlobInfo = storage.compose(request);
      System.out.println("Composed " + composedBlobInfo);
    }

    @Override
    ComposeRequest parse(Storage storage, String... args) {
      if (args.length < 3) {
        throw new IllegalArgumentException();
      }
      ComposeRequest.Builder request = ComposeRequest.builder();
      request.target(BlobInfo.builder(args[0], args[args.length - 1]).build());
      for (int i = 1; i < args.length - 1; i++) {
        request.addSource(args[i]);
      }
      return request.build();
    }

    @Override
    public String params() {
      return "<bucket> <from_path>+ <to_path>";
    }
  }

  /**
   * This class demonstrates how to update a blob's metadata.
   *
   * @see <a href="https://cloud.google.com/storage/docs/json_api/v1/objects/update">Objects: update</a>
   */
  private static class UpdateMetadataAction extends
      StorageAction<Tuple<Blob, Map<String, String>>> {

    @Override
    public void run(Storage storage, Tuple<Blob, Map<String, String>> tuple)
        throws IOException {
      run(storage, tuple.x(), tuple.y());
    }

    private void run(Storage storage, Blob blob, Map<String, String> metadata) {
      blob = blob.reload();
      if (!blob.exists()) {
        System.out.println("No such object");
        return;
      }
      Blob updateBlob = blob.update(blob.info().toBuilder().metadata(metadata).build());
      System.out.println("Updated " + updateBlob.info());
    }

    @Override
    Tuple<Blob, Map<String, String>> parse(Storage storage, String... args) {
      if (args.length < 2) {
        throw new IllegalArgumentException();
      }
      Blob blob = new Blob(storage, args[0], args[1]);
      Map<String, String> metadata = new HashMap<>();
      for (int i = 2; i < args.length; i++) {
        int idx = args[i].indexOf('=');
        if (idx < 0) {
          metadata.put(args[i], "");
        } else {
          metadata.put(args[i].substring(0, idx), args[i].substring(idx + 1));
        }
      }
      return Tuple.of(blob, metadata);
    }

    @Override
    public String params() {
      return "<bucket> <path> [local_file]";
    }
  }

  /**
   * This class demonstrates how to sign a url.
   * URL will be valid for 1 day.
   *
   * @see <a href="https://cloud.google.com/storage/docs/access-control#Signed-URLs">Signed URLs</a>
   */
  private static class SignUrlAction extends
      StorageAction<Tuple<ServiceAccountAuthCredentials, Blob>> {

    private static final char[] PASSWORD = "notasecret".toCharArray();

    @Override
    public void run(Storage storage, Tuple<ServiceAccountAuthCredentials, Blob> tuple)
        throws Exception {
      run(storage, tuple.x(), tuple.y());
    }

    private void run(Storage storage, ServiceAccountAuthCredentials cred, Blob blob)
        throws IOException {
      System.out.println("Signed URL: " +
          blob.signUrl(1, TimeUnit.DAYS, SignUrlOption.serviceAccount(cred)));
    }

    @Override
    Tuple<ServiceAccountAuthCredentials, Blob> parse(Storage storage, String... args)
        throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException,
        UnrecoverableKeyException {
      if (args.length != 4) {
        throw new IllegalArgumentException();
      }
      KeyStore keystore = KeyStore.getInstance("PKCS12");
      keystore.load(Files.newInputStream(Paths.get(args[0])), PASSWORD);
      PrivateKey privateKey = (PrivateKey) keystore.getKey("privatekey", PASSWORD);
      ServiceAccountAuthCredentials cred = AuthCredentials.createFor(args[1], privateKey);
      return Tuple.of(cred, new Blob(storage, args[2], args[3]));
    }

    @Override
    public String params() {
      return "<service_account_private_key_file> <service_account_email> <bucket> <path>";
    }
  }

  static {
    ACTIONS.put("info", new InfoAction());
    ACTIONS.put("delete", new DeleteAction());
    ACTIONS.put("list", new ListAction());
    ACTIONS.put("upload", new UploadAction());
    ACTIONS.put("download", new DownloadAction());
    ACTIONS.put("cp", new CopyAction());
    ACTIONS.put("compose", new ComposeAction());
    ACTIONS.put("update_metadata", new UpdateMetadataAction());
    ACTIONS.put("sign_url", new SignUrlAction());
  }

  public static void printUsage() {
    StringBuilder actionAndParams = new StringBuilder();
    for (Map.Entry<String, StorageAction> entry : ACTIONS.entrySet()) {
      actionAndParams.append("\n\t").append(entry.getKey());

      String param = entry.getValue().params();
      if (param != null && !param.isEmpty()) {
        actionAndParams.append(' ').append(param);
      }
    }
    System.out.printf("Usage: %s [<project_id>] operation <args>*%s%n",
        StorageExample.class.getSimpleName(), actionAndParams);
  }

  @SuppressWarnings("unchecked")
  public static void main(String... args) throws Exception {
    if (args.length < 1) {
      System.out.println("Missing required project id and action");
      printUsage();
      return;
    }
    StorageOptions.Builder optionsBuilder =
        StorageOptions.builder().retryParams(RetryParams.getDefaultInstance());
    StorageAction action;
    if (args.length >= 2 && !ACTIONS.containsKey(args[0])) {
      optionsBuilder.projectId(args[0]);
      action = ACTIONS.get(args[1]);
      args = Arrays.copyOfRange(args, 2, args.length);
    } else {
      action = ACTIONS.get(args[0]);
      args = Arrays.copyOfRange(args, 1, args.length);
    }
    if (action == null) {
      System.out.println("Unrecognized action.");
      printUsage();
      return;
    }
    Storage storage = StorageFactory.instance().get(optionsBuilder.build());
    Object request;
    try {
      request = action.parse(storage, args);
    } catch (IllegalArgumentException ex) {
      System.out.println("Invalid input for action '" + args[1] + "'");
      System.out.println("Expected: " + action.params());
      return;
    } catch (Exception ex) {
      System.out.println("Failed to parse request.");
      ex.printStackTrace();
      return;
    }
    action.run(storage, request);
  }
}
