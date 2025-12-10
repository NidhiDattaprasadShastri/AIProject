package com.diplomatic;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.typed.Cluster;
import com.diplomatic.actors.infrastructure.ClusterSupervisorActor;
import com.diplomatic.messages.SessionCreatedMessage;
import com.diplomatic.messages.UserQueryMessage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Node 1 - FIXED VERSION - Uses response handler instead of ask pattern!
 */
public class Node1App {

    private static ActorRef<ClusterSupervisorActor.Command> supervisorRef;
    private static ActorSystem<Void> system;

    public static void main(String[] args) {
        Config config = ConfigFactory.parseFile(
                new File("src/main/resources/application-node1.conf")
        ).withFallback(ConfigFactory.load());

        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║       DIPLOMATIC ASSISTANT - NODE 1 (Infrastructure)         ║");
        System.out.println("║       Port: 2551 | Roles: [infrastructure, frontend]         ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");

        System.out.println("✓ Config loaded from: application-node1.conf");
        System.out.println("✓ Provider: " + config.getString("akka.actor.provider"));
        System.out.println("✓ Port: " + config.getInt("akka.remote.artery.canonical.port"));

        system = ActorSystem.create(
                Behaviors.setup(context -> {
                    supervisorRef = context.spawn(
                            ClusterSupervisorActor.createInfrastructure(),
                            "cluster-supervisor"
                    );

                    System.out.println("✅ ClusterSupervisor spawned");
                    supervisorRef.tell(new ClusterSupervisorActor.MonitorCluster());

                    return Behaviors.empty();
                }),
                "DiplomaticAssistantSystem",
                config
        );

        Cluster cluster = Cluster.get(system);

        System.out.println("\n🚀 Node 1 starting...");
        System.out.println("📍 Address: " + cluster.selfMember().address());
        System.out.println("🎭 Roles: " + cluster.selfMember().roles());
        System.out.println("⏳ Waiting for cluster formation...\n");

        new Thread(() -> {
            try {
                Thread.sleep(15000);
                System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
                System.out.println("║  CLUSTER INITIALIZED - STARTING INTERACTIVE CLI               ║");
                System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");
                startInteractiveCLI();
            } catch (Exception e) {
                System.err.println("Error in CLI thread: " + e.getMessage());
            }
        }).start();

        system.getWhenTerminated().toCompletableFuture().join();
    }

    private static void startInteractiveCLI() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  CROSS-CULTURAL DIPLOMATIC ASSISTANT                         ║");
        System.out.println("║  Powered by Akka Cluster & IDEA Framework                    ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");

        System.out.print("Enter your name (or press Enter for 'Diplomat'): ");
        String userName = scanner.nextLine().trim();
        if (userName.isEmpty()) {
            userName = "Diplomat";
        }

        System.out.println("\n✅ Welcome, " + userName + "!");
        System.out.println("⏳ Creating diplomatic consultation session...\n");

        String sessionId = createSession(userName);
        if (sessionId == null) {
            scanner.close();
            return;
        }

        System.out.println("\n" + "─".repeat(63));
        System.out.println("Ready! Type your questions or 'help' for examples, 'exit' to quit");
        System.out.println("─".repeat(63) + "\n");

        runQueryLoop(scanner, sessionId);
        scanner.close();
    }

    private static String createSession(String userName) {
        try {
            // FIXED: Use CompletableFuture with tell pattern
            CompletableFuture<String> sessionFuture = new CompletableFuture<>();

            // Create response handler behavior
            Behavior<SessionCreatedMessage> responseHandlerBehavior = Behaviors.receive(
                    (context, msg) -> {
                        System.out.println("✅ Session response received!");
                        sessionFuture.complete(msg.getSessionId());
                        return Behaviors.stopped();
                    }
            );

            // Spawn the response handler
            ActorRef<SessionCreatedMessage> responseHandler =
                    system.systemActorOf(responseHandlerBehavior,
                            "session-response-" + System.currentTimeMillis(),
                            akka.actor.typed.Props.empty());

            supervisorRef.tell(new ClusterSupervisorActor.CreateSession(userName, responseHandler));

            String sessionId = sessionFuture.get(10, TimeUnit.SECONDS);

            System.out.println("✅ Session created: " + sessionId);
            System.out.println("👤 User: " + userName);

            return sessionId;

        } catch (Exception e) {
            System.err.println("❌ Failed to create session: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static void runQueryLoop(Scanner scanner, String sessionId) {
        while (true) {
            System.out.print("You: ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println("\n✅ Thank you for using the Diplomatic Assistant!\n");
                System.out.println("💡 Press Ctrl+C to stop Node 1 and Node 2\n");
                break;
            }

            if (input.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            processQuery(sessionId, input);
        }
    }

    private static void processQuery(String sessionId, String query) {
        System.out.println("\n📤 Sending query through cluster...");
        System.out.println("   Node 1 → Node 2 → Claude API\n");

        try {
            // FIXED: Use CompletableFuture with tell pattern
            CompletableFuture<String> responseFuture = new CompletableFuture<>();

            // Create response handler behavior
            Behavior<String> responseHandlerBehavior = Behaviors.receive(
                    (context, msg) -> {
                        System.out.println("✅ Query response received!");
                        responseFuture.complete(msg);
                        return Behaviors.stopped();
                    }
            );

            // Spawn the response handler
            ActorRef<String> responseHandler =
                    system.systemActorOf(responseHandlerBehavior,
                            "query-response-" + System.currentTimeMillis(),
                            akka.actor.typed.Props.empty());

            UserQueryMessage queryMsg = new UserQueryMessage(sessionId, query, responseHandler);
            supervisorRef.tell(new ClusterSupervisorActor.RouteQuery(queryMsg));

            String response = responseFuture.get(30, TimeUnit.SECONDS);

            System.out.println("╔═══════════════════════════════════════════════════════════════╗");
            System.out.println("║  DIPLOMATIC ASSISTANT RESPONSE                                ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");
            System.out.println(response);
            System.out.println("\n" + "─".repeat(63) + "\n");

        } catch (Exception e) {
            System.err.println("❌ Error processing query: " + e.getMessage());
            System.err.println("The intelligence actors might not be ready. Please try again.\n");
        }
    }

    private static void printHelp() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  EXAMPLE QUERIES                                              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");
        System.out.println("🌍 Cultural Questions:");
        System.out.println("  • How should I greet Japanese diplomats?");
        System.out.println("  • What are Moroccan business etiquette norms?");
        System.out.println("  • Cultural considerations for Kuwait?");
        System.out.println("  • Japanese business culture and hierarchy\n");
        System.out.println("🤝 Diplomatic Primitives (IDEA Framework):");
        System.out.println("  • How to propose a trade deal with Canada?");
        System.out.println("  • Help me clarify terms with Turkish officials");
        System.out.println("  • Setting deadlines in Japanese negotiations");
        System.out.println("  • When to escalate in Mauritanian talks?\n");
    }
}