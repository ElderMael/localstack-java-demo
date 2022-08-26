package io.eldermael.jvmmx.localstack.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
public class Upload implements RequestHandler<S3Event, String> {

  private final S3Client s3Client;
  private final String bucketName;

  public Upload() {
    this.bucketName = System.getenv("AWS_BUCKET_NAME");
    this.s3Client = S3Client.builder().build();
  }

  public Upload(S3Client s3Client, String bucketName) {
    this.s3Client = s3Client;
    this.bucketName = bucketName;
  }

  @Override
  public String handleRequest(S3Event input, Context context) {
    Optional<String> content = input.getRecords().stream().findFirst().map(record -> {
      var bucket = record.getS3().getBucket().getName();
      var fileName = record.getS3().getObject().getKey();

      try {
        byte[] bytes = this.s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build())
            .readAllBytes();

        var fileContent = new String(bytes, StandardCharsets.UTF_8);
        log.info("Content of S3 record file '{}': {}", fileName, fileContent);

        return fileContent;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    return content.orElse("failed");
  }
}
