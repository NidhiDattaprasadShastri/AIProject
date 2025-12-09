package com.diplomatic;

import akka.actor.typed.ActorSystem;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Join;
import com.diplomatic.actors.intelligence.IntelligenceNodeSupervisor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node 2 - Intelligence/Backend Node
 * Runs on port 2552
 *
 * Actors on this node:
 * - IntelligenceSupervisorActor
 * - ScenarioClassifierActor
 * - CulturalContextActor
 * - DiplomaticPrimitivesActor
 * - LLMProcessorActor
 */
public class Node2App {
    private static final Logger logger = LoggerFactory.getLogger(Node2App.class);

    public static void main(String[] args) {
        // Get API configuration
        String apiKey = System.getenv("LLM_API_KEY");
        String apiProvider = System.getenv("LLM_PROVIDER");

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("⚠️  No LLM_API_KEY environment variable found.");
            logger.warn("Set your API key: export LLM_API_KEY=your-key");
            logger.warn("System will run in MOCK mode.");
            apiKey = null;
        }

        if (apiProvider == null || apiProvider.isEmpty()) {
            apiProvider = "CLAUDE";
        }

        // Load Node 2 configuration
        Config config = ConfigFactory.load("application-node2.conf");

        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║       DIPLOMATIC ASSISTANT - NODE 2 (Intelligence)           ║");
        System.out.println("║       Port: 2552 | Roles: [intelligence, backend]            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");

        if (apiKey != null) {
            System.out.println("🤖 Using LLM Provider: " + apiProvider);
            System.out.println("✓ API Key configured");
        } else {
            System.out.println("⚠️  Running in MOCK mode (no API key)");
        }

        // Create actor system
        final String finalApiKey = apiKey;
        final String finalApiProvider = apiProvider;

        ActorSystem<IntelligenceNodeSupervisor.Command> system = ActorSystem.create(
                IntelligenceNodeSupervisor.create(finalApiKey, finalApiProvider),
                "DiplomaticAssistantSystem",
                config
        );

        // Get cluster instance
        Cluster cluster = Cluster.get(system);

        logger.info("🚀 Node 2 starting...");
        logger.info("📍 Address: {}", cluster.selfMember().address());
        logger.info("🎭 Roles: {}", cluster.selfMember().roles());

        // Join the cluster
        cluster.manager().tell(Join.create(cluster.selfMember().address()));

        logger.info("⏳ Waiting for cluster formation (need 2 nodes)...");

        // Initialize intelligence actors once cluster is up
        system.tell(new IntelligenceNodeSupervisor.Initialize());

        // Keep system alive
        system.getWhenTerminated().toCompletableFuture().join();
    }
}