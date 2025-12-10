package com.diplomatic.actors.intelligence;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.Cluster;
import com.diplomatic.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intelligence Node Supervisor for Node 2
 *
 * PROJECT REQUIREMENT: Manages intelligence/LLM actors on dedicated cluster node
 *
 * Spawned actors:
 * - ScenarioClassifierActor
 * - CulturalContextActor
 * - DiplomaticPrimitivesActor
 * - LLMProcessorActor
 */
public class IntelligenceNodeSupervisor extends AbstractBehavior<IntelligenceNodeSupervisor.Command> {

    private final Logger logger = LoggerFactory.getLogger(IntelligenceNodeSupervisor.class);
    private final Cluster cluster;
    private final String apiKey;
    private final String apiProvider;

    // Service keys for cluster-wide actor discovery
    public static final ServiceKey<RouteToClassifierMessage> CLASSIFIER_KEY =
            ServiceKey.create(RouteToClassifierMessage.class, "classifier");

    public static final ServiceKey<CulturalAnalysisRequestMessage> CULTURAL_KEY =
            ServiceKey.create(CulturalAnalysisRequestMessage.class, "cultural");

    public static final ServiceKey<DiplomaticPrimitiveRequestMessage> PRIMITIVES_KEY =
            ServiceKey.create(DiplomaticPrimitiveRequestMessage.class, "primitives");

    private ActorRef<RouteToClassifierMessage> classifierActor;
    private ActorRef<CulturalAnalysisRequestMessage> culturalActor;
    private ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor;
    private ActorRef<LLMRequestMessage> llmActor;

    public interface Command {}

    public static final class Initialize implements Command {}

    public static Behavior<Command> create(String apiKey, String apiProvider) {
        return Behaviors.setup(context -> new IntelligenceNodeSupervisor(context, apiKey, apiProvider));
    }

    private IntelligenceNodeSupervisor(ActorContext<Command> context, String apiKey, String apiProvider) {
        super(context);
        this.cluster = Cluster.get(context.getSystem());
        this.apiKey = apiKey;
        this.apiProvider = apiProvider;

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  NODE 2: Intelligence Supervisor Created                  ║");
        System.out.println("║  Roles: " + cluster.selfMember().roles() + "                              ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Initialize.class, this::onInitialize)
                .build();
    }

    private Behavior<Command> onInitialize(Initialize cmd) {
        System.out.println("🧠 Initializing intelligence actors on Node 2...");

        try {
            // 1. Spawn LLM Processor Actor
            this.llmActor = getContext().spawn(
                    LLMProcessorActor.create(apiKey, apiProvider),
                    "llm-processor"
            );
            System.out.println("✓ LLMProcessorActor spawned");

            // 2. Spawn Scenario Classifier Actor
            this.classifierActor = getContext().spawn(
                    ScenarioClassifierActor.create(),
                    "scenario-classifier"
            );
            System.out.println("✓ ScenarioClassifierActor spawned");

            // Register classifier with receptionist for cluster-wide discovery
            getContext().getSystem().receptionist().tell(
                    Receptionist.register(CLASSIFIER_KEY, classifierActor)
            );
            System.out.println("📡 Classifier registered with receptionist");

            // 3. Spawn Cultural Context Actor
            this.culturalActor = getContext().spawn(
                    CulturalContextActor.create(llmActor),
                    "cultural-context"
            );
            System.out.println("✓ CulturalContextActor spawned");

            // Register cultural actor
            getContext().getSystem().receptionist().tell(
                    Receptionist.register(CULTURAL_KEY, culturalActor)
            );
            System.out.println("📡 Cultural actor registered with receptionist");

            // 4. Spawn Diplomatic Primitives Actor
            this.primitivesActor = getContext().spawn(
                    DiplomaticPrimitivesActor.create(llmActor),
                    "diplomatic-primitives"
            );
            System.out.println("✓ DiplomaticPrimitivesActor spawned");

            // Register primitives actor
            getContext().getSystem().receptionist().tell(
                    Receptionist.register(PRIMITIVES_KEY, primitivesActor)
            );
            System.out.println("📡 Primitives actor registered with receptionist");

            System.out.println("🎉 All intelligence actors initialized and registered!");
            System.out.println("✅ Node 2 ready to process queries");

        } catch (Exception e) {
            logger.error("❌ Failed to initialize intelligence actors", e);
            throw new RuntimeException("Intelligence initialization failed", e);
        }

        return this;
    }
}