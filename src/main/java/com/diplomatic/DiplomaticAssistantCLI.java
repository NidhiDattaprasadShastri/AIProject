package com.diplomatic;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import com.diplomatic.actors.infrastructure.SupervisorActor;
import com.diplomatic.messages.SessionCreatedMessage;
import com.diplomatic.messages.StartSessionMessage;
import com.diplomatic.messages.UserQueryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.CompletionStage;

public class DiplomaticAssistantCLI {
    private static final Logger logger = LoggerFactory.getLogger(DiplomaticAssistantCLI.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private final ActorSystem<SupervisorActor.Command> actorSystem;
    private String currentSessionId;
    private final Scanner scanner;

    public DiplomaticAssistantCLI() {
        this.actorSystem = ActorSystem.create(SupervisorActor.create(), "diplomatic-assistant");
        this.scanner = new Scanner(System.in);
        logger.info("Diplomatic Assistant CLI initialized");
    }

    public void start() {
        printWelcomeBanner();
        if (!createSession()) {
            logger.error("Failed to create session. Exiting.");
            shutdown();
            return;
        }
        conversationLoop();
        shutdown();
    }

    private void printWelcomeBanner() {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║        CROSS-CULTURAL DIPLOMATIC ASSISTANT                     ║");
        System.out.println("║        Powered by AKKA Actor Model & AI                        ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
        System.out.println("Welcome to your AI-powered diplomatic communication assistant!");
        System.out.println("I can help you navigate cross-cultural diplomatic challenges.\n");
        System.out.println("Supported country pairs:");
        System.out.println("  • Kuwait ↔ Morocco");
        System.out.println("  • Canada ↔ Japan");
        System.out.println("  • Turkey ↔ Mauritania\n");
        System.out.println("Using IDEA Framework primitives:");
        System.out.println("  • PROPOSE, CLARIFY, CONSTRAIN, REVISE");
        System.out.println("  • AGREE, ESCALATE, DEFER\n");
        System.out.println("Commands: 'help' for guidance, 'exit' to quit\n");
        System.out.println("─────────────────────────────────────────────────────────────────\n");
    }

    private boolean createSession() {
        System.out.print("Enter your name or ID: ");
        String input = scanner.nextLine().trim();
        final String userId = input.isEmpty() ? "diplomat-" + System.currentTimeMillis() : input;
        System.out.println("\n Creating your diplomatic consultation session...\n");
        try {
            CompletionStage<SessionCreatedMessage> result = AskPattern.ask(actorSystem,
                    replyTo -> new SupervisorActor.CreateSession(userId, replyTo),
                    TIMEOUT,
                    actorSystem.scheduler()
            );
            SessionCreatedMessage response = result.toCompletableFuture().get();
            currentSessionId = response.getSessionId();
            System.out.println(" Session created successfully!");
            System.out.println(" Session ID: " + currentSessionId);
            System.out.println(" User: " + response.getUserId());
            System.out.println("\nYou can now ask diplomatic questions.\n");
            System.out.println("─────────────────────────────────────────────────────────────────\n");
            return true;

        } catch (Exception e) {
            logger.error("Failed to create session", e);
            System.err.println(" Error creating session: " + e.getMessage());
            return false;
        }
    }
            private void conversationLoop() {
        System.out.println("Start your consultation (type 'exit' to quit, 'help' for examples):\n");
        while (true) {
            System.out.print("You: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }
            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println("\n Thank you for using the Diplomatic Assistant. Goodbye!\n");
                break;
            }
            if (input.equalsIgnoreCase("help")) {
                printHelpExamples();
                continue;
            }
            processQuery(input);
        }
    }

    private void processQuery(String query) {
        try {
            System.out.println("\n Analyzing your diplomatic query...\n");
            CompletionStage<String> result = AskPattern.ask(actorSystem,
                    (ActorRef<String> replyTo) -> {
                        UserQueryMessage queryMsg = new UserQueryMessage(
                                currentSessionId,
                                query,
                                replyTo
                        );
                        return new SupervisorActor.RouteQuery(queryMsg);
                    },
                    TIMEOUT,
                    actorSystem.scheduler()
            );
            String response = result.toCompletableFuture().get();
            System.out.println(" Assistant:\n");
            System.out.println(formatResponse(response));
            System.out.println("\n─────────────────────────────────────────────────────────────────\n");

        } catch (Exception e) {
            logger.error("Error processing query", e);
            System.err.println("\n Error: " + e.getMessage());
            System.out.println("Please try again or type 'help' for examples.\n");
        }
    }

    private String formatResponse(String response) {
        return response.replace("\n\n", "\n  \n  ");
    }

    private void printHelpExamples() {
        System.out.println("\nEXAMPLE QUERIES:\n");
        System.out.println("Cultural Questions:");
        System.out.println("  • How should I greet a Japanese diplomat?");
        System.out.println("  • What are the cultural norms for gift-giving in Kuwait?");
        System.out.println("  • How do Moroccan negotiation styles differ from Canadian?\n");
        System.out.println("Diplomatic Primitives (IDEA Framework):");
        System.out.println("  • How should I propose a new trade agreement with Japan?");
        System.out.println("  • Help me clarify misunderstandings with a Kuwaiti counterpart");
        System.out.println("  • What constraints should I set in negotiations with Turkey?");
        System.out.println("  • How do I revise my proposal for a Mauritanian delegation?");
        System.out.println("  • When should I escalate issues to higher authority?\n");
        System.out.println("General Diplomatic Guidance:");
        System.out.println("  • What's the profit-fear-power dynamic in Arab diplomacy?");
        System.out.println("  • How does hierarchy affect decision-making in Japanese culture?");
        System.out.println("  • Tips for building trust with Turkish diplomats?\n");
        System.out.println("─────────────────────────────────────────────────────────────────\n");
    }

    private void shutdown() {
        System.out.println("Shutting down system...");
        try {
            actorSystem.terminate();
            actorSystem.getWhenTerminated().toCompletableFuture().get();
            System.out.println("System shutdown complete.\n");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
        scanner.close();
    }

    public static void main(String[] args) {
        try {
            DiplomaticAssistantCLI cli = new DiplomaticAssistantCLI();
            cli.start();
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
