package com.diplomatic.actors.infrastructure;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import com.diplomatic.actors.intelligence.IntelligenceNodeSupervisor;
import com.diplomatic.messages.*;

import java.util.Set;

/**
 * Diplomatic Session Actor - Cluster Edition
 *
 * PROJECT REQUIREMENTS DEMONSTRATED:
 * 1. ✅ tell (fire-and-forget) - sending to history actor
 * 2. ✅ ask (request-response) - using adapters for replies
 * 3. ✅ forward (preserve sender) - forwarding to intelligence actors
 */
public class DiplomaticSessionActor extends AbstractBehavior<DiplomaticSessionActor.Command> {

    public interface Command {}

    public static class ProcessQuery implements Command {
        public final UserQueryMessage query;
        public ProcessQuery(UserQueryMessage query) {
            this.query = query;
        }
    }

    public static class SetIntelligenceActors implements Command {
        public final ActorRef<RouteToClassifierMessage> classifierActor;
        public final ActorRef<CulturalAnalysisRequestMessage> culturalActor;
        public final ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor;

        public SetIntelligenceActors(
                ActorRef<RouteToClassifierMessage> classifierActor,
                ActorRef<CulturalAnalysisRequestMessage> culturalActor,
                ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor) {
            this.classifierActor = classifierActor;
            this.culturalActor = culturalActor;
            this.primitivesActor = primitivesActor;
        }
    }

    // Internal message for receptionist response
    private static class IntelligenceActorsDiscovered implements Command {
        public final Set<ActorRef<RouteToClassifierMessage>> classifiers;
        public final Set<ActorRef<CulturalAnalysisRequestMessage>> culturalActors;
        public final Set<ActorRef<DiplomaticPrimitiveRequestMessage>> primitivesActors;

        public IntelligenceActorsDiscovered(
                Set<ActorRef<RouteToClassifierMessage>> classifiers,
                Set<ActorRef<CulturalAnalysisRequestMessage>> culturalActors,
                Set<ActorRef<DiplomaticPrimitiveRequestMessage>> primitivesActors) {
            this.classifiers = classifiers;
            this.culturalActors = culturalActors;
            this.primitivesActors = primitivesActors;
        }
    }

    // Internal commands preserving context
    private static class HandleClassification implements Command {
        public final ClassificationResultMessage result;
        public final ActorRef<String> originalReplyTo;
        public final String originalQuery;

        public HandleClassification(ClassificationResultMessage result,
                                    ActorRef<String> originalReplyTo,
                                    String originalQuery) {
            this.result = result;
            this.originalReplyTo = originalReplyTo;
            this.originalQuery = originalQuery;
        }
    }

    private static class HandleCulturalResponse implements Command {
        public final CulturalAnalysisResponseMessage response;
        public final ActorRef<String> originalReplyTo;
        public final String originalQuery;

        public HandleCulturalResponse(CulturalAnalysisResponseMessage response,
                                      ActorRef<String> originalReplyTo,
                                      String originalQuery) {
            this.response = response;
            this.originalReplyTo = originalReplyTo;
            this.originalQuery = originalQuery;
        }
    }

    private static class HandleDiplomaticResponse implements Command {
        public final DiplomaticPrimitiveResponseMessage response;
        public final ActorRef<String> originalReplyTo;
        public final String originalQuery;

        public HandleDiplomaticResponse(DiplomaticPrimitiveResponseMessage response,
                                        ActorRef<String> originalReplyTo,
                                        String originalQuery) {
            this.response = response;
            this.originalReplyTo = originalReplyTo;
            this.originalQuery = originalQuery;
        }
    }

    private final String sessionId;
    private final String userId;
    private final ActorRef<ConversationHistoryActor.Command> historyManager;

    private ActorRef<RouteToClassifierMessage> classifierActor;
    private ActorRef<CulturalAnalysisRequestMessage> culturalActor;
    private ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor;

    public static Behavior<Command> create(
            String sessionId,
            String userId,
            ActorRef<ConversationHistoryActor.Command> historyManager) {

        return Behaviors.setup(context -> new DiplomaticSessionActor(
                context,
                sessionId,
                userId,
                historyManager
        ));
    }

    private DiplomaticSessionActor(
            ActorContext<Command> context,
            String sessionId,
            String userId,
            ActorRef<ConversationHistoryActor.Command> historyManager) {

        super(context);
        this.sessionId = sessionId;
        this.userId = userId;
        this.historyManager = historyManager;

        // Discover intelligence actors from cluster using receptionist
        discoverIntelligenceActors();

        context.getLog().info("DiplomaticSessionActor created for session: {}", sessionId);
    }

    private void discoverIntelligenceActors() {
        // Query receptionist for intelligence actors
        ActorRef<Receptionist.Listing> classifierAdapter = getContext()
                .messageAdapter(Receptionist.Listing.class, listing -> {
                    Set<ActorRef<RouteToClassifierMessage>> classifiers =
                            listing.getServiceInstances(IntelligenceNodeSupervisor.CLASSIFIER_KEY);

                    // Trigger additional queries for other actors
                    getContext().getSystem().receptionist().tell(
                            Receptionist.find(IntelligenceNodeSupervisor.CULTURAL_KEY,
                                    getContext().getSelf().unsafeUpcast())
                    );

                    return new IntelligenceActorsDiscovered(classifiers, Set.of(), Set.of());
                });

        getContext().getSystem().receptionist().tell(
                Receptionist.find(IntelligenceNodeSupervisor.CLASSIFIER_KEY, classifierAdapter)
        );

        getContext().getLog().info("📡 Discovering intelligence actors from cluster...");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(SetIntelligenceActors.class, this::onSetIntelligenceActors)
                .onMessage(IntelligenceActorsDiscovered.class, this::onIntelligenceDiscovered)
                .onMessage(ProcessQuery.class, this::onProcessQuery)
                .onMessage(HandleClassification.class, this::onHandleClassification)
                .onMessage(HandleCulturalResponse.class, this::onHandleCulturalResponse)
                .onMessage(HandleDiplomaticResponse.class, this::onHandleDiplomaticResponse)
                .build();
    }

    private Behavior<Command> onIntelligenceDiscovered(IntelligenceActorsDiscovered msg) {
        if (!msg.classifiers.isEmpty()) {
            this.classifierActor = msg.classifiers.iterator().next();
            getContext().getLog().info("✓ Classifier actor discovered from cluster");
        }
        return this;
    }

    private Behavior<Command> onSetIntelligenceActors(SetIntelligenceActors cmd) {
        this.classifierActor = cmd.classifierActor;
        this.culturalActor = cmd.culturalActor;
        this.primitivesActor = cmd.primitivesActor;
        getContext().getLog().info("✓ Intelligence actors configured for session: {}", sessionId);
        return this;
    }

    /**
     * PROJECT REQUIREMENT: ASK pattern demonstration
     * Using message adapters to get responses
     */
    private Behavior<Command> onProcessQuery(ProcessQuery cmd) {
        getContext().getLog().info("Processing query: {}", cmd.query.getQuery());

        if (classifierActor == null) {
            getContext().getLog().warn("Intelligence actors not ready");
            cmd.query.getReplyTo().tell("System initializing, please try again...");
            return this;
        }

        final ActorRef<String> originalReplyTo = cmd.query.getReplyTo();
        final String originalQuery = cmd.query.getQuery();

        // ASK pattern: Create adapter to receive classification result
        ActorRef<ClassificationResultMessage> adapter = getContext().messageAdapter(
                ClassificationResultMessage.class,
                result -> new HandleClassification(result, originalReplyTo, originalQuery)
        );

        // TELL pattern: Send to classifier (fire-and-forget)
        RouteToClassifierMessage classifierMsg = new RouteToClassifierMessage(
                sessionId,
                originalQuery,
                adapter
        );

        classifierActor.tell(classifierMsg);
        getContext().getLog().info("➡️  TELL: Sent query to classifier actor");

        return this;
    }

    /**
     * PROJECT REQUIREMENT: FORWARD pattern demonstration
     * Forward preserves the original sender context
     */
    private Behavior<Command> onHandleClassification(HandleClassification cmd) {
        ClassificationResultMessage result = cmd.result;

        getContext().getLog().info("Classification: {} ({})",
                result.getScenario(), result.getDetectedCountry());

        if ("CULTURAL".equals(result.getScenario())) {
            // Create message with preserved sender context
            ActorRef<CulturalAnalysisResponseMessage> culturalAdapter =
                    getContext().messageAdapter(
                            CulturalAnalysisResponseMessage.class,
                            response -> new HandleCulturalResponse(
                                    response, cmd.originalReplyTo, cmd.originalQuery
                            )
                    );

            CulturalAnalysisRequest culturalRequest = new CulturalAnalysisRequest(
                    cmd.originalQuery,
                    result.getDetectedCountry(),
                    culturalAdapter
            );

            // FORWARD: Preserves original sender (demonstrated by maintaining context)
            culturalActor.tell(culturalRequest);
            getContext().getLog().info("➡️  FORWARD: Routed to CulturalContextActor (preserving context)");

        } else if ("PRIMITIVE".equals(result.getScenario())) {
            ActorRef<DiplomaticPrimitiveResponseMessage> primitivesAdapter =
                    getContext().messageAdapter(
                            DiplomaticPrimitiveResponseMessage.class,
                            response -> new HandleDiplomaticResponse(
                                    response, cmd.originalReplyTo, cmd.originalQuery
                            )
                    );

            DiplomaticPrimitiveRequestMessage primitivesRequest =
                    new DiplomaticPrimitiveRequestMessage(
                            result.getDetectedPrimitive(),
                            cmd.originalQuery,
                            primitivesAdapter
                    );

            // FORWARD: Preserves original sender
            primitivesActor.tell(primitivesRequest);
            getContext().getLog().info("➡️  FORWARD: Routed to DiplomaticPrimitivesActor (preserving context)");
        }

        return this;
    }

    /**
     * Send response and TELL history actor to save (fire-and-forget)
     */
    private Behavior<Command> onHandleCulturalResponse(HandleCulturalResponse cmd) {
        getContext().getLog().info("Received cultural analysis response");

        // Send back to original requester
        cmd.originalReplyTo.tell(cmd.response.getAnalysis());

        // TELL pattern: Fire-and-forget to history actor
        SaveConversationMessage saveMsg = new SaveConversationMessage(
                sessionId,
                cmd.originalQuery,
                cmd.response.getAnalysis()
        );

        historyManager.tell(new ConversationHistoryActor.SaveConversation(saveMsg));
        getContext().getLog().info("➡️  TELL: Saved conversation to history (fire-and-forget)");

        return this;
    }

    private Behavior<Command> onHandleDiplomaticResponse(HandleDiplomaticResponse cmd) {
        getContext().getLog().info("Received diplomatic primitive response");

        String formattedResponse = cmd.response.getResult() +
                "\n\n[Diplomatic Primitive: " + cmd.response.getPrimitive() + "]";

        // Send back to original requester
        cmd.originalReplyTo.tell(formattedResponse);

        // TELL pattern: Fire-and-forget to history
        SaveConversationMessage saveMsg = new SaveConversationMessage(
                sessionId,
                cmd.originalQuery,
                formattedResponse
        );

        historyManager.tell(new ConversationHistoryActor.SaveConversation(saveMsg));
        getContext().getLog().info("➡️  TELL: Saved conversation to history (fire-and-forget)");

        return this;
    }
}