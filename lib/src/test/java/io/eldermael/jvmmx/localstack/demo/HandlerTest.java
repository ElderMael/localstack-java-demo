package io.eldermael.jvmmx.localstack.demo;

import cloud.localstack.LocalstackTestRunner;
import cloud.localstack.awssdkv2.TestUtils;
import cloud.localstack.docker.LocalstackDockerExtension;
import cloud.localstack.docker.annotation.LocalstackDockerProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import static org.assertj.core.api.BDDAssertions.then;

@ExtendWith(LocalstackDockerExtension.class)
@LocalstackDockerProperties(services = {
    "sqs"
})
public class HandlerTest {

  @Test
  public void testHandler() {
    // given
    var clientSQSV2 = TestUtils.getClientSQSV2();
    var response = clientSQSV2.createQueue(CreateQueueRequest.builder()
        .queueName("testqueue")
        .build());

    String queueUrl = response.queueUrl();

    var handler = new Handler(queueUrl);

    // when
    var result = handler.handleRequest(null, null);

    // then
    then(result).isEqualTo(null);
    var message = clientSQSV2.receiveMessage(ReceiveMessageRequest.builder()
        .queueUrl(queueUrl)
        .build());
    then(message.hasMessages()).isTrue();
    then(message.messages().get(0).body()).isEqualTo("test");

  }

}
