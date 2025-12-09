package com.diplomatic;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import com.diplomatic.actors.infrastructure.SupervisorActor;
import com.diplomatic.messages.SessionCreatedMessage;
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

    public DiplomaticAssistantCLI(ActorSystem<SupervisorActor.Command> actorSystem) {
        this.actorSystem = actorSystem;
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
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║        CROSS-CULTURAL DIPLOMATIC ASSISTANT                    ║");
        System.out.println("║        Powered by AKKA Actor Model & AI                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");
        System.out.println("Welcome to your AI-powered diplomatic communication assistant!");
        System.out.println("Navigate cross-cultural diplomatic challenges with confidence.\n");
        System.out.println("🌍 Works with any country or cultural context");
        System.out.println("🤝 Provides culturally-informed guidance");
        System.out.println("📋 Uses IDEA Framework diplomatic primitives:\n");
        System.out.println("   PROPOSE   - Present new ideas or solutions");
        System.out.println("   CLARIFY   - Seek or provide understanding");
        System.out.println("   CONSTRAIN - Define boundaries or requirements");
        System.out.println("   REVISE    - Modify proposals based on feedback");
        System.out.println("   AGREE     - Reach consensus or accept terms");
        System.out.println("   ESCALATE  - Elevate issues to higher authority");
        System.out.println("   DEFER     - Postpone decisions for more information\n");
        System.out.println("Commands: 'help' for examples, 'exit' to quit\n");
        System.out.println("────────────────────────────────────────────────────────────────\n");
    }

    private boolean createSession() {
        System.out.print("Enter your name or ID (press Enter for auto-generated): ");
        String input = scanner.nextLine().trim();
        final String userId = input.isEmpty() ? "diplomat-" + System.currentTimeMillis() : input;
        System.out.println("\n✓ Creating your diplomatic consultation session...\n");
        try {
            CompletionStage<SessionCreatedMessage> result = AskPattern.ask(actorSystem,
                    replyTo -> new SupervisorActor.CreateSession(userId, replyTo),
                    TIMEOUT,
                    actorSystem.scheduler()
            );
            SessionCreatedMessage response = result.toCompletableFuture().get();
            currentSessionId = response.getSessionId();
            System.out.println("✓ Session created successfully!");
            System.out.println("📋 Session ID: " + currentSessionId);
            System.out.println("👤 User: " + response.getUserId());
            System.out.println("\nYou can now ask diplomatic questions about any country or culture.\n");
            System.out.println("────────────────────────────────────────────────────────────────\n");
            return true;

        } catch (Exception e) {
            logger.error("Failed to create session", e);
            System.err.println("❌ Error creating session: " + e.getMessage());
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
                System.out.println("\n✓ Thank you for using the Diplomatic Assistant. Goodbye!\n");
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
            System.out.println("\n🔄 Analyzing your diplomatic query...\n");
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
            System.out.println("🤖 Assistant:\n");
            System.out.println(formatResponse(response));
            System.out.println("\n────────────────────────────────────────────────────────────────\n");

        } catch (Exception e) {
            logger.error("Error processing query", e);
            System.err.println("\n❌ Error: " + e.getMessage());
            System.out.println("Please try again or type 'help' for examples.\n");
        }
    }

    private String formatResponse(String response) {
        return response.replace("\n\n", "\n  \n  ");
    }

    private void printHelpExamples() {
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("                        EXAMPLE QUERIES");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        System.out.println("📚 CULTURAL QUESTIONS:");
        System.out.println("  • How should I greet a Japanese diplomat?");
        System.out.println("  • What are gift-giving customs in Saudi Arabia?");
        System.out.println("  • How does hierarchy affect meetings in Korean culture?");
        System.out.println("  • What body language should I avoid in Brazil?");
        System.out.println("  • Explain indirect communication in Chinese negotiations\n");

        System.out.println("🤝 DIPLOMATIC PRIMITIVES (IDEA Framework):");
        System.out.println("\n  PROPOSE:");
        System.out.println("    - How should I propose a trade agreement with Germany?");
        System.out.println("    - Best way to introduce new terms with Indian counterparts?");
        System.out.println("\n  CLARIFY:");
        System.out.println("    - Help me clarify misunderstandings with French diplomats");
        System.out.println("    - How do I ask for clarification without offending in Japan?");
        System.out.println("\n  CONSTRAIN:");
        System.out.println("    - Setting deadlines with Middle Eastern partners");
        System.out.println("    - How to establish boundaries in Russian negotiations?");
        System.out.println("\n  REVISE:");
        System.out.println("    - Modifying proposals for Brazilian stakeholders");
        System.out.println("    - How to suggest changes without losing face in China?");
        System.out.println("\n  AGREE:");
        System.out.println("    - Finalizing agreements with Italian counterparts");
        System.out.println("    - What does consensus mean in Nigerian culture?");
        System.out.println("\n  ESCALATE:");
        System.out.println("    - When should I escalate issues to higher authority?");
        System.out.println("    - Protocol for elevating disputes in Turkish negotiations");
        System.out.println("\n  DEFER:");
        System.out.println("    - How to postpone decisions with Mexican partners?");
        System.out.println("    - Requesting more time in South Korean business culture\n");

        System.out.println("🌍 GENERAL DIPLOMATIC GUIDANCE:");
        System.out.println("  • Building trust with Turkish officials");
        System.out.println("  • Power dynamics in Arab diplomatic circles");
        System.out.println("  • Decision-making hierarchy in Japanese organizations");
        System.out.println("  • Conflict resolution styles in African cultures");
        System.out.println("  • Time perception differences in Latin American diplomacy\n");

        System.out.println("💡 TIPS:");
        System.out.println("  • Be specific about the country or culture");
        System.out.println("  • Mention the diplomatic context (negotiation, meeting, etc.)");
        System.out.println("  • Ask about specific primitives for structured guidance");
        System.out.println("  • Use cultural keywords for cultural analysis\n");

        System.out.println("════════════════════════════════════════════════════════════════\n");
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
        System.out.println("Please run DiplomaticAssistantApp instead.");
    }
}