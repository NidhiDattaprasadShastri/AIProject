package com.diplomatic;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Join;
import com.diplomatic.actors.infrastructure.ClusterSupervisorActor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node 1 - Infrastructure/Frontend Node
 * Runs on port 2551
 *
 * Actors on this node:
 * - SupervisorActor
 * - SessionManagerActor
 * - DiplomaticSessionActor (instances)
 * - ConversationHistoryActor
 */
public class Node1App {
    private static final Logger logger = LoggerFactory.getLogger(Node1App.class);

    public static void main(String[] args) {
        // Load Node 1 configuration
        Config config = ConfigFactory.load("application-node1.conf");

        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║       DIPLOMATIC ASSISTANT - NODE 1 (Infrastructure)         ║");
        System.out.println("║       Port: 2551 | Roles: [infrastructure, frontend]         ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");

        // Create actor system
        ActorSystem<ClusterSupervisorActor.Command> system = ActorSystem.create(
                ClusterSupervisorActor.createInfrastructure(),
                "DiplomaticAssistantSystem",
                config
        );

        // Get cluster instance
        Cluster cluster = Cluster.get(system);

        logger.info("🚀 Node 1 starting...");
        logger.info("📍 Address: {}", cluster.selfMember().address());
        logger.info("🎭 Roles: {}", cluster.selfMember().roles());

        // Join the cluster (will wait for Node 2)
        cluster.manager().tell(Join.create(cluster.selfMember().address()));

        logger.info("⏳ Waiting for cluster formation (need 2 nodes)...");

        // Register cluster event listeners
        system.tell(new ClusterSupervisorActor.MonitorCluster());

        // Keep system alive
        system.getWhenTerminated().toCompletableFuture().join();
    }
}