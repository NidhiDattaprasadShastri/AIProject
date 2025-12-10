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

        // Load configuration from file
        Config config = ConfigFactory.parseFile(
                new File("src/main/resources/application-node2.conf")
        ).withFallback(ConfigFactory.load());

        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║       DIPLOMATIC ASSISTANT - NODE 2 (Intelligence)           ║");
        System.out.println("║       Port: 2552 | Roles: [intelligence, backend]            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");

        if (apiKey != null) {
            System.out.println("🤖 Using LLM Provider: CLAUDE");
            System.out.println("✓ API Key configured");
        } else {
            System.out.println("⚠️  No API Key configured");
        }

        System.out.println("✓ Config loaded from: application-node2.conf");
        System.out.println("✓ Provider: " + config.getString("akka.actor.provider"));
        System.out.println("✓ Port: " + config.getInt("akka.remote.artery.canonical.port"));

        // Create actor system (apiProvider parameter is ignored now)
        ActorSystem<IntelligenceNodeSupervisor.Command> system = ActorSystem.create(
                IntelligenceNodeSupervisor.create(apiKey, "CLAUDE"),
                "DiplomaticAssistantSystem",
                config
        );

        Cluster cluster = Cluster.get(system);

        System.out.println("\n🚀 Node 2 starting...");
        System.out.println("📍 Address: " + cluster.selfMember().address());
        System.out.println("🎭 Roles: " + cluster.selfMember().roles());
        System.out.println("⏳ Waiting for cluster formation (need 2 nodes)...\n");

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