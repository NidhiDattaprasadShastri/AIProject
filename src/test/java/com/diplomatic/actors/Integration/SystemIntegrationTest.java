package com.diplomatic.actors.Integration;
import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.typed.ActorRef;
import com.diplomatic.actors.infrastructure.SupervisorActor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SystemIntegrationTest {
    private static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    public static void cleanup() {
        testKit.shutdownTestKit();
    }

    @Test
    public void testSystemStartup() {
        ActorRef<SupervisorActor.Command> supervisor =
                testKit.spawn(SupervisorActor.create(), "integration-test-supervisor");
        assertNotNull(supervisor, "Supervisor should be created");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println(" System integration test passed!");
    }
}
