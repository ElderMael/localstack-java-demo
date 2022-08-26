package io.eldermael.jvmmx.localstack.demo;

import cloud.localstack.awssdkv2.TestUtils;
import cloud.localstack.docker.LocalstackDockerExtension;
import cloud.localstack.docker.annotation.LocalstackDockerProperties;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;


@ExtendWith(LocalstackDockerExtension.class)
@ExtendWith(MockitoExtension.class)
@LocalstackDockerProperties(services = {
    "sqs", "s3"
})
public class HandlerTest {

  @Mock
  private Context context;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private S3EventNotification.S3EventNotificationRecord s3EventNotificationRecord;

  @Test
  public void testHandler() {
    // given
    given(this.context.getFunctionName()).willReturn("api.Handler");
    var clientSQSV2 = TestUtils.getClientSQSV2();
    var response = clientSQSV2.createQueue(CreateQueueRequest.builder()
        .queueName("testqueue")
        .build());

    String queueUrl = response.queueUrl();

    var handler = new Handler(queueUrl, clientSQSV2);
    var input = new SNSEvent()
        .withRecords(of(new SNSEvent.SNSRecord()
            .withEventSource("SNS Source")
            .withSns(
                new SNSEvent.SNS().withMessage("Hi")
            )
        ));

    // when
    var result = handler.handleRequest(input, this.context);

    // then
    then(result.getStatusCode()).isEqualTo(200);
    var message = clientSQSV2.receiveMessage(ReceiveMessageRequest.builder()
        .queueUrl(queueUrl)
        .build());
    then(message.hasMessages()).isTrue();
    then(message.messages().get(0).body()).isEqualTo("test");
  }

  @Test
  public void testHandlerSubscription() {
    // given
    String bucketName = "uploads";
    String fileName = "filename.txt";
    S3Client clientS3V2 = TestUtils.getClientS3V2();
    clientS3V2.createBucket(CreateBucketRequest.builder()
        .bucket(bucketName)
        .build());

    clientS3V2.putObject(PutObjectRequest.builder()
        .bucket(bucketName)
        .key(fileName)
        .build(), RequestBody.fromString("test"));

    given(this.s3EventNotificationRecord.getS3().getBucket().getName()).willReturn(bucketName);
    given(this.s3EventNotificationRecord.getS3().getObject().getKey()).willReturn(fileName);

    var handler = new Upload(clientS3V2, bucketName);

    // when
    var response = handler.handleRequest(new S3Event(
        of(this.s3EventNotificationRecord)
    ), this.context);

    assertThat(response).isEqualTo("test");

  }

}
