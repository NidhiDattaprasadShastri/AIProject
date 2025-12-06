package com.diplomatic.actors;
import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.diplomatic.actors.infrastructure.ConversationHistoryActor;
import com.diplomatic.actors.infrastructure.DiplomaticSessionActor;
import com.diplomatic.messages.UserQueryMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class DiplomaticSessionActorTest {
    private static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    public static void cleanup() {
        testKit.shutdownTestKit();
    }

    @Test
    public void testSessionProcessesQueryInMockMode() {
        ActorRef<ConversationHistoryActor.Command> historyActor = testKit.spawn(ConversationHistoryActor.create(), "history-actor");
        ActorRef<DiplomaticSessionActor.Command> sessionActor = testKit.spawn(DiplomaticSessionActor.create("test-session-123", historyActor),
                        "session-actor");
        TestProbe<String> responseProbe = testKit.createTestProbe();
        UserQueryMessage query = new UserQueryMessage("test-session-123", "How should I negotiate with Japan?",
                responseProbe.getRef());
        sessionActor.tell(new DiplomaticSessionActor.ProcessQuery(query));
        String response = responseProbe.receiveMessage();
        assertNotNull(response, "Response should not be null");
        assertTrue(response.contains("MOCK MODE"), "Response should contain MOCK MODE");
        assertTrue(response.contains("Japan") || response.contains("negotiate"), "Response should mention Japan or negotiate");
        System.out.println(" Test passed! Received response:");
        System.out.println(response);
    }

    @Test
    public void testMultipleQueriesInSession() {
        ActorRef<ConversationHistoryActor.Command> historyActor = testKit.spawn(ConversationHistoryActor.create(), "history-actor-2");
        ActorRef<DiplomaticSessionActor.Command> sessionActor = testKit.spawn(DiplomaticSessionActor.create("test-session-456", historyActor),
                        "session-actor-2");
        TestProbe<String> responseProbe = testKit.createTestProbe();
        UserQueryMessage query1 = new UserQueryMessage("test-session-456", "Cultural norms in Kuwait?",
                responseProbe.getRef());
        sessionActor.tell(new DiplomaticSessionActor.ProcessQuery(query1));
        String response1 = responseProbe.receiveMessage();
        assertNotNull(response1);
        assertTrue(response1.contains("Turn 1"));
        UserQueryMessage query2 = new UserQueryMessage("test-session-456", "How to propose agreement?",
                responseProbe.getRef());
        sessionActor.tell(new DiplomaticSessionActor.ProcessQuery(query2));
        String response2 = responseProbe.receiveMessage();
        assertNotNull(response2);
        assertTrue(response2.contains("Turn 2"));
        System.out.println(" Multiple queries test passed!");
    }
}
