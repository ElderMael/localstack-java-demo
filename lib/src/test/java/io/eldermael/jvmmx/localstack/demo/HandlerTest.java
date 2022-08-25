package io.eldermael.jvmmx.localstack.demo;

import cloud.localstack.awssdkv2.TestUtils;
import cloud.localstack.docker.LocalstackDockerExtension;
import cloud.localstack.docker.annotation.LocalstackDockerProperties;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import static java.util.List.of;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;


@ExtendWith(LocalstackDockerExtension.class)
@ExtendWith(MockitoExtension.class)
@LocalstackDockerProperties(services = {
    "sqs"
})
public class HandlerTest {

  @Mock
  private Context context;

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

}
