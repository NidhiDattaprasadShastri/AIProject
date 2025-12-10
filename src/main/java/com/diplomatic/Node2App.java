package com.diplomatic;

import akka.actor.typed.ActorSystem;
import akka.cluster.typed.Cluster;
import com.diplomatic.actors.intelligence.IntelligenceNodeSupervisor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

/**
 * Node 2 - Intelligence/Backend Node
 * Runs on port 2552
 */
public class Node2App {

    public static void main(String[] args) {
        // Get API key
        String apiKey = System.getenv("LLM_API_KEY");
        String apiProvider = System.getenv("LLM_PROVIDER");

        if (apiProvider == null || apiProvider.isEmpty()) {
            apiProvider = "CLAUDE";
        }

        // Load configuration from file
        Config config = ConfigFactory.parseFile(
                new File("src/main/resources/application-node2.conf")
        ).withFallback(ConfigFactory.load());

        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║       DIPLOMATIC ASSISTANT - NODE 2 (Intelligence)           ║");
        System.out.println("║       Port: 2552 | Roles: [intelligence, backend]            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");

        if (apiKey != null) {
            System.out.println("🤖 Using LLM Provider: " + apiProvider);
            System.out.println("✓ API Key configured");
        } else {
            System.out.println("⚠️  No API Key - running in MOCK mode");
        }

        System.out.println("✓ Config loaded from: application-node2.conf");
        System.out.println("✓ Provider: " + config.getString("akka.actor.provider"));
        System.out.println("✓ Port: " + config.getInt("akka.remote.artery.canonical.port"));

        final String finalApiKey = apiKey;
        final String finalApiProvider = apiProvider;

        // Create actor system
        ActorSystem<IntelligenceNodeSupervisor.Command> system = ActorSystem.create(
                IntelligenceNodeSupervisor.create(finalApiKey, finalApiProvider),
                "DiplomaticAssistantSystem",
                config
        );

        Cluster cluster = Cluster.get(system);

        System.out.println("\n🚀 Node 2 starting...");
        System.out.println("📍 Address: " + cluster.selfMember().address());
        System.out.println("🎭 Roles: " + cluster.selfMember().roles());
        System.out.println("⏳ Waiting for cluster formation (need 2 nodes)...\n");

        // DON'T explicitly join, let seed-nodes handle it

        // Initialize intelligence actors after a delay to ensure cluster is formed
        new Thread(() -> {
            try {
                Thread.sleep(3000); // Wait 3 seconds for cluster formation
                System.out.println("🧠 Initializing intelligence actors...");
                system.tell(new IntelligenceNodeSupervisor.Initialize());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        // Keep system alive
        system.getWhenTerminated().toCompletableFuture().join();
    }
}