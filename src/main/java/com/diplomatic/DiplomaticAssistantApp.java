package com.diplomatic;

import akka.actor.typed.ActorSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiplomaticAssistantApp {

    private static final Logger logger = LoggerFactory.getLogger(DiplomaticAssistantApp.class);

    public static void main(String[] args) {
        String apiKey = System.getenv("LLM_API_KEY");
        String apiProvider = System.getenv("LLM_PROVIDER");

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("⚠️  No LLM_API_KEY environment variable found.");
            logger.warn("Please set your API key:");
            logger.warn("  For Claude: export LLM_API_KEY=your-claude-key");
            logger.warn("  For OpenAI: export LLM_API_KEY=your-openai-key");
            logger.warn("");
            logger.warn("System will run in MOCK mode without real LLM integration.");
            apiKey = null;
        }

        if (apiProvider == null || apiProvider.isEmpty()) {
            apiProvider = "CLAUDE";
            if (apiKey != null) {
                logger.info("No LLM_PROVIDER set, defaulting to CLAUDE");
            }
        }

        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║        CROSS-CULTURAL DIPLOMATIC ASSISTANT                     ║");
        System.out.println("║        Powered by AKKA Actor Model & AI                        ║");
        System.out.println("║        Part A (Infrastructure) + Part B (Intelligence)         ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

        if (apiKey != null) {
            System.out.println("🤖 Using LLM Provider: " + apiProvider);
            System.out.println("✓ API Key configured");
        } else {
            System.out.println("⚠️  Running in MOCK mode (no API key)");
        }
        System.out.println("📡 Starting distributed actor system...\n");

        ActorSystem<SupervisorActor.Command> system =
                ActorSystem.create(SupervisorActor.create(), "DiplomaticAssistantSystem");

        try {
            if (apiKey != null) {
                logger.info("🧠 Initializing intelligence actors...");
                system.tell(new SupervisorActor.InitializeIntelligence(apiKey, apiProvider));

                Thread.sleep(1000);

                System.out.println("✓ Intelligence actors initialized");
            }

            System.out.println("✓ System ready\n");

            DiplomaticAssistantCLI cli = new DiplomaticAssistantCLI(system);
            cli.start();

        } catch (Exception e) {
            logger.error("❌ Error during system startup", e);
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            system.terminate();
            System.exit(1);
        }
    }
}