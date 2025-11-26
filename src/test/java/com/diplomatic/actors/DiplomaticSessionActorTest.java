package com.diplomatic.actors;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.diplomatic.actors.infrastructure.ConversationHistoryActor;
import com.diplomatic.actors.infrastructure.DiplomaticSessionActor;
import com.diplomatic.messages.UserQueryMessage;
import org.junit.ClassRule;
import org.junit.Test;

public class DiplomaticSessionActorTest {
    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void testSessionProcessesQueryInMockMode() {
        ActorRef<ConversationHistoryActor.Command> historyActor =
                testKit.spawn(ConversationHistoryActor.create());
        ActorRef<DiplomaticSessionActor.Command> sessionActor =
                testKit.spawn(DiplomaticSessionActor.create("test-session-123", historyActor));
        TestProbe<String> responseProbe = testKit.createTestProbe();
        UserQueryMessage query = new UserQueryMessage("test-session-123",
                "How should I negotiate with Japan?", responseProbe.getRef());
        sessionActor.tell(new DiplomaticSessionActor.ProcessQuery(query));
        String response = responseProbe.receiveMessage();
        assert response.contains("MOCK MODE");
        assert response.contains("Japan");
        System.out.println("Received response: " + response);
    }
}
