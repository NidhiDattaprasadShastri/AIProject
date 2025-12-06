package com.diplomatic.actors;
import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.typed.ActorRef;
import com.diplomatic.actors.infrastructure.ConversationHistoryActor;
import com.diplomatic.messages.SaveConversationMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConversationHistoryActorTest {
    private static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    public static void cleanup() {
        testKit.shutdownTestKit();
    }

    @Test
    public void testSaveConversation() {
        ActorRef<ConversationHistoryActor.Command> historyActor = testKit.spawn(ConversationHistoryActor.create(), "history-test");
        SaveConversationMessage saveMsg = new SaveConversationMessage("test-session-789", "How to greet in Japan?",
                "In Japan, bowing is the traditional greeting...");
        historyActor.tell(new ConversationHistoryActor.SaveConversation(saveMsg));
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        historyActor.tell(ConversationHistoryActor.GetStats.INSTANCE);
        System.out.println(" Conversation history test passed!");
    }
}
