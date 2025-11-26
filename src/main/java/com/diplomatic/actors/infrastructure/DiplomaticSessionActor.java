package com.diplomatic.actors.infrastructure;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.diplomatic.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiplomaticSessionActor extends AbstractBehavior<DiplomaticSessionActor.Command> {

    private final Logger logger = LoggerFactory.getLogger(DiplomaticSessionActor.class);
    private final String sessionId;
    private final ActorRef<ConversationHistoryActor.Command> historyActor;

    // References to Option B's intelligence actors (will be set when they're available)
    private ActorRef<RouteToClassifierMessage> classifierActor;
    private ActorRef<CulturalAnalysisRequestMessage> culturalActor;
    private ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor;

    // Track conversation context
    private int conversationTurnCount = 0;

    // Sealed interface for all commands this actor handles
    public interface Command {}

    // Process a user query
    public static final class ProcessQuery implements Command {
        public final UserQueryMessage queryMessage;

        public ProcessQuery(UserQueryMessage queryMessage) {
            this.queryMessage = queryMessage;
        }
    }

    // Internal message: Classification completed
    private static final class ClassificationComplete implements Command {
        public final ClassificationResultMessage result;
        public final UserQueryMessage originalQuery;

        public ClassificationComplete(ClassificationResultMessage result,
                                      UserQueryMessage originalQuery) {
            this.result = result;
            this.originalQuery = originalQuery;
        }
    }

    // Internal message: Cultural analysis completed
    private static final class CulturalAnalysisComplete implements Command {
        public final CulturalAnalysisResponseMessage response;
        public final UserQueryMessage originalQuery;

        public CulturalAnalysisComplete(CulturalAnalysisResponseMessage response,
                                        UserQueryMessage originalQuery) {
            this.response = response;
            this.originalQuery = originalQuery;
        }
    }

    // Internal message: Primitive analysis completed
    private static final class PrimitiveAnalysisComplete implements Command {
        public final DiplomaticPrimitiveResponseMessage response;
        public final UserQueryMessage originalQuery;

        public PrimitiveAnalysisComplete(DiplomaticPrimitiveResponseMessage response,
                                         UserQueryMessage originalQuery) {
            this.response = response;
            this.originalQuery = originalQuery;
        }
    }

    // Set intelligence actor references (called by SessionManager)
    public static final class SetIntelligenceActors implements Command {
        public final ActorRef<RouteToClassifierMessage> classifier;
        public final ActorRef<CulturalAnalysisRequestMessage> cultural;
        public final ActorRef<DiplomaticPrimitiveRequestMessage> primitives;

        public SetIntelligenceActors(ActorRef<RouteToClassifierMessage> classifier,
                ActorRef<CulturalAnalysisRequestMessage> cultural,
                ActorRef<DiplomaticPrimitiveRequestMessage> primitives) {
            this.classifier = classifier;
            this.cultural = cultural;
            this.primitives = primitives;
        }
    }

    private DiplomaticSessionActor(ActorContext<Command> context, String sessionId, ActorRef<ConversationHistoryActor.Command> historyActor) {
        super(context);
        this.sessionId = sessionId;
        this.historyActor = historyActor;
        logger.info("DiplomaticSessionActor created for session: {}", sessionId);
    }

    public static Behavior<Command> create(String sessionId, ActorRef<ConversationHistoryActor.Command> historyActor) {
        return Behaviors.setup(ctx -> new DiplomaticSessionActor(ctx, sessionId, historyActor));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ProcessQuery.class, this::onProcessQuery)
                .onMessage(SetIntelligenceActors.class, this::onSetIntelligenceActors)
                .onMessage(ClassificationComplete.class, this::onClassificationComplete)
                .onMessage(CulturalAnalysisComplete.class, this::onCulturalAnalysisComplete)
                .onMessage(PrimitiveAnalysisComplete.class, this::onPrimitiveAnalysisComplete)
                .build();
    }

    private Behavior<Command> onSetIntelligenceActors(SetIntelligenceActors cmd) {
        logger.info("Setting intelligence actor references for session: {}", sessionId);
        this.classifierActor = cmd.classifier;
        this.culturalActor = cmd.cultural;
        this.primitivesActor = cmd.primitives;
        return this;
    }

    private Behavior<Command> onProcessQuery(ProcessQuery cmd) {
        conversationTurnCount++;
        logger.info("Processing query #{} for session {}: {}",
                conversationTurnCount, sessionId, cmd.queryMessage.getQuery());
        if (classifierActor == null) {
            logger.warn("Intelligence actors not yet initialized, using mock response");
            sendMockResponse(cmd.queryMessage);
            return this;
        }

        ActorRef<ClassificationResultMessage> responseAdapter =
                getContext().messageAdapter(
                        ClassificationResultMessage.class,
                        result -> new ClassificationComplete(result, cmd.queryMessage)
                );
        RouteToClassifierMessage classifyMsg = new RouteToClassifierMessage(
                sessionId,
                cmd.queryMessage.getQuery(),
                responseAdapter
        );
        classifierActor.tell(classifyMsg);
        logger.info("Sent query to classifier for session: {}", sessionId);
        return this;
    }

    private Behavior<Command> onClassificationComplete(ClassificationComplete cmd) {
        logger.info("Classification complete for session {}: scenario={}, target={}, confidence={}",
                sessionId, cmd.result.getScenario(), cmd.result.getTargetActor(),
                cmd.result.getConfidence());
        String scenario = cmd.result.getScenario();
        if ("CULTURAL".equals(scenario)) {
            routeToCulturalAnalysis(cmd.result, cmd.originalQuery);
        } else if ("PRIMITIVE".equals(scenario)) {
            routeToPrimitiveAnalysis(cmd.result, cmd.originalQuery);
        } else {
            routeToPrimitiveAnalysis(cmd.result, cmd.originalQuery);
        }
        return this;
    }

    private void routeToCulturalAnalysis(ClassificationResultMessage classification,
                                         UserQueryMessage originalQuery) {
        logger.info("Routing to CulturalContextActor for session: {}", sessionId);
        ActorRef<CulturalAnalysisResponseMessage> responseAdapter =
                getContext().messageAdapter(
                        CulturalAnalysisResponseMessage.class,
                        response -> new CulturalAnalysisComplete(response, originalQuery)
                );
        String country = classification.getDetectedCountry() != null
                ? classification.getDetectedCountry()
                : "General";
        CulturalAnalysisRequest request = new CulturalAnalysisRequest(
                originalQuery.getQuery(),
                country,
                responseAdapter
        );
        culturalActor.tell(request);
    }

    private void routeToPrimitiveAnalysis(ClassificationResultMessage classification,
                                          UserQueryMessage originalQuery) {
        logger.info("Routing to DiplomaticPrimitivesActor for session: {}", sessionId);
        ActorRef<DiplomaticPrimitiveResponseMessage> responseAdapter =
                getContext().messageAdapter(
                        DiplomaticPrimitiveResponseMessage.class,
                        response -> new PrimitiveAnalysisComplete(response, originalQuery)
                );
        String primitive = classification.getDetectedPrimitive() != null
                ? classification.getDetectedPrimitive()
                : "GENERAL";
        DiplomaticPrimitiveRequestMessage request = new DiplomaticPrimitiveRequestMessage(
                primitive,
                originalQuery.getQuery(),
                responseAdapter
        );
        primitivesActor.tell(request);
    }

    private Behavior<Command> onCulturalAnalysisComplete(CulturalAnalysisComplete cmd) {
        logger.info("Cultural analysis complete for session: {}", sessionId);
        String response = cmd.response.getAnalysis();
        saveConversation(cmd.originalQuery.getQuery(), response);
        cmd.originalQuery.getReplyTo().tell(response);
        return this;
    }

    private Behavior<Command> onPrimitiveAnalysisComplete(PrimitiveAnalysisComplete cmd) {
        logger.info("Primitive analysis complete for session: {}", sessionId);
        String response = cmd.response.getResult();
        saveConversation(cmd.originalQuery.getQuery(), response);
        cmd.originalQuery.getReplyTo().tell(response);
        return this;
    }

    private void saveConversation(String query, String response) {
        SaveConversationMessage saveMsg = new SaveConversationMessage(sessionId, query, response);
        historyActor.tell(new ConversationHistoryActor.SaveConversation(saveMsg));
    }

    private void sendMockResponse(UserQueryMessage queryMessage) {
        String mockResponse = String.format(
                "[MOCK MODE] Session %s - Turn %d\n" +
                        "Query: %s\n\n" +
                        "Mock Response: I understand you're asking about diplomatic communication. " +
                        "Once the intelligence actors (Option B) are integrated, I'll provide " +
                        "culturally-informed guidance using the IDEA framework. For now, I'm operating " +
                        "in test mode with the infrastructure layer fully functional.",
                sessionId, conversationTurnCount, queryMessage.getQuery()
        );
        saveConversation(queryMessage.getQuery(), mockResponse);
        queryMessage.getReplyTo().tell(mockResponse);
        logger.info("Sent mock response for session: {}", sessionId);
    }
}
