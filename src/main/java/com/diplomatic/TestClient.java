package com.diplomatic;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Adapter;
import com.diplomatic.actors.infrastructure.ClusterSupervisorActor;
import com.diplomatic.messages.SessionCreatedMessage;
import com.diplomatic.messages.UserQueryMessage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Scanner;
import java.time.Duration;

/**
 * Interactive Test Client for Cross-Cultural Diplomatic Assistant
 */
public class TestClient {

    public static void main(String[] args) {
        String configString = """
            akka {
              loglevel = "INFO"
              
              actor {
                provider = cluster
                
                serialization-bindings {
                  "com.diplomatic.messages.CborSerializable" = jackson-cbor
                }
                
                allow-java-serialization = on
                warn-about-java-serializer-usage = off
              }
              
              remote.artery {
                canonical.hostname = "127.0.0.1"
                canonical.port = 0
              }
              
              cluster {
                seed-nodes = [
                  "akka://DiplomaticAssistantSystem@127.0.0.1:2551"
                ]
                roles = ["client"]
              }
            }
            """;

        Config config = ConfigFactory.parseString(configString);

        printBanner();

        System.out.println("🔗 Connecting to cluster...");

        ActorSystem<ClientActor.Command> system = ActorSystem.create(
                ClientActor.create(),
                "DiplomaticAssistantSystem",
                config
        );

        try {
            Thread.sleep(3000); // Wait for cluster join
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("✅ Connected to cluster!");
        System.out.println("🔍 Looking up ClusterSupervisor on Node 1...\n");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Scanner scanner = new Scanner(System.in);

        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  CROSS-CULTURAL DIPLOMATIC ASSISTANT - CLUSTER MODE          ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");

        System.out.print("Enter your name (or press Enter for 'Diplomat'): ");
        String userName = scanner.nextLine().trim();
        if (userName.isEmpty()) userName = "Diplomat";

        System.out.println("\n✅ Welcome, " + userName + "!");
        System.out.println("⏳ Creating session through cluster...\n");

        system.tell(new ClientActor.CreateSession(userName));

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n" + "─".repeat(63));
        System.out.println("Ready! Type your questions or 'help' for examples, 'exit' to quit");
        System.out.println("─".repeat(63) + "\n");

        // Interactive loop
        while (true) {
            System.out.print("You: ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println("\n✅ Thank you for using the Diplomatic Assistant!\n");
                try {
                    system.terminate();
                    system.getWhenTerminated().toCompletableFuture().get();
                } catch (Exception e) {
                    // Ignore
                }
                scanner.close();
                System.exit(0);
            }

            if (input.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            System.out.println("\n📤 Sending to cluster...\n");
            system.tell(new ClientActor.SendQuery(input));

            try {
                Thread.sleep(10000); // Wait for LLM response
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void printBanner() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  CROSS-CULTURAL DIPLOMATIC ASSISTANT                         ║");
        System.out.println("║  Powered by Akka Cluster & IDEA Framework                    ║");
        System.out.println("║  Team: Nidhi Shastri & Yasmin Almousa                        ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");
    }

    private static void printHelp() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  EXAMPLE QUERIES                                              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println("\n🌍 Cultural Questions:");
        System.out.println("  • How should I greet Japanese diplomats?");
        System.out.println("  • What are Moroccan business etiquette norms?");
        System.out.println("  • Cultural considerations for Kuwait?\n");
        System.out.println("🤝 Diplomatic Primitives (IDEA Framework):");
        System.out.println("  • How to propose a trade deal with Canada?");
        System.out.println("  • Help me clarify terms with Turkish officials");
        System.out.println("  • Setting deadlines in Japanese negotiations\n");
    }

    private static class ClientActor {
        interface Command {}

        static class CreateSession implements Command {
            final String userName;
            CreateSession(String userName) { this.userName = userName; }
        }

        static class SupervisorResolved implements Command {
            final ActorRef<ClusterSupervisorActor.Command> supervisor;
            SupervisorResolved(ActorRef<ClusterSupervisorActor.Command> supervisor) {
                this.supervisor = supervisor;
            }
        }

        static class SessionCreated implements Command {
            final SessionCreatedMessage msg;
            SessionCreated(SessionCreatedMessage msg) { this.msg = msg; }
        }

        static class SendQuery implements Command {
            final String query;
            SendQuery(String query) { this.query = query; }
        }

        static class QueryResponse implements Command {
            final String response;
            QueryResponse(String response) { this.response = response; }
        }

        static Behavior<Command> create() {
            return Behaviors.setup(context -> {
                System.out.println("🔍 Resolving ClusterSupervisor using actor selection...");

                // Use actor selection to directly address the supervisor on Node 1
                String supervisorPath = "akka://DiplomaticAssistantSystem@127.0.0.1:2551/user/cluster-supervisor";

                context.getSystem().classicSystem().actorSelection(supervisorPath)
                        .resolveOne(Duration.ofSeconds(5))
                        .whenComplete((actorRef, throwable) -> {
                            if (throwable == null) {
                                ActorRef<ClusterSupervisorActor.Command> typedRef =
                                        Adapter.toTyped(actorRef);
                                context.getSelf().tell(new SupervisorResolved(typedRef));
                            } else {
                                System.err.println("❌ Failed to resolve supervisor: " + throwable.getMessage());
                                throwable.printStackTrace();
                            }
                        });

                return waitingForSupervisor(context);
            });
        }

        private static Behavior<Command> waitingForSupervisor(ActorContext<Command> context) {
            return Behaviors.receive(Command.class)
                    .onMessage(SupervisorResolved.class, msg -> {
                        System.out.println("✅ Connected to ClusterSupervisor on Node 1!");
                        return active(context, msg.supervisor, null);
                    })
                    .onMessage(CreateSession.class, msg -> {
                        // Retry after delay
                        context.scheduleOnce(
                                Duration.ofSeconds(1),
                                context.getSelf(),
                                msg
                        );
                        return Behaviors.same();
                    })
                    .onMessage(SendQuery.class, msg -> {
                        System.out.println("⏳ Still connecting...");
                        return Behaviors.same();
                    })
                    .build();
        }

        private static Behavior<Command> active(
                ActorContext<Command> context,
                ActorRef<ClusterSupervisorActor.Command> supervisor,
                String sessionId) {

            return Behaviors.receive(Command.class)
                    .onMessage(CreateSession.class, msg -> {
                        ActorRef<SessionCreatedMessage> adapter = context.messageAdapter(
                                SessionCreatedMessage.class, SessionCreated::new
                        );

                        supervisor.tell(new ClusterSupervisorActor.CreateSession(msg.userName, adapter));
                        System.out.println("📤 Session creation sent to Node 1");
                        return Behaviors.same();
                    })
                    .onMessage(SessionCreated.class, msg -> {
                        System.out.println("✅ Session created: " + msg.msg.getSessionId());
                        System.out.println("👤 User: " + msg.msg.getUserId());
                        return active(context, supervisor, msg.msg.getSessionId());
                    })
                    .onMessage(SendQuery.class, msg -> {
                        if (sessionId == null) {
                            System.out.println("❌ No session - waiting...");
                            return Behaviors.same();
                        }

                        ActorRef<String> responseAdapter = context.messageAdapter(
                                String.class, QueryResponse::new
                        );

                        UserQueryMessage queryMsg = new UserQueryMessage(sessionId, msg.query, responseAdapter);
                        supervisor.tell(new ClusterSupervisorActor.RouteQuery(queryMsg));

                        System.out.println("📡 Query sent:");
                        System.out.println("   Client → Node 1 → Node 2 → Claude API");
                        System.out.println("⏳ Waiting for response (this may take 5-10 seconds)...");

                        return Behaviors.same();
                    })
                    .onMessage(QueryResponse.class, msg -> {
                        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
                        System.out.println("║  DIPLOMATIC ASSISTANT RESPONSE                                ║");
                        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");
                        System.out.println(msg.response);
                        System.out.println("\n" + "─".repeat(63) + "\n");
                        return Behaviors.same();
                    })
                    .build();
        }
    }
}