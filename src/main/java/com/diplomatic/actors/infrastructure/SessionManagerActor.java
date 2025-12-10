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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManagerActor extends AbstractBehavior<SessionManagerActor.Command> {

    private final Logger logger = LoggerFactory.getLogger(SessionManagerActor.class);
    private final Map<String, ActorRef<DiplomaticSessionActor.Command>> activeSessions;
    private final ActorRef<ConversationHistoryActor.Command> historyActor;

    private ActorRef<RouteToClassifierMessage> classifierActor;
    private ActorRef<CulturalAnalysisRequestMessage> culturalActor;
    private ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor;
    private boolean intelligenceActorsReady = false;

    public interface Command {}

    public static final class Shutdown implements Command {
        public static final Shutdown INSTANCE = new Shutdown();
        private Shutdown() {}
    }

    public static final class CreateSession implements Command {
        public final String userId;
        public final ActorRef<SessionCreatedMessage> replyTo;
        public CreateSession(String userId, ActorRef<SessionCreatedMessage> replyTo) {
            this.userId = userId;
            this.replyTo = replyTo;
        }
    }

    public static final class RouteToSession implements Command {
        public final String sessionId;
        public final String query;
        public final ActorRef<String> replyTo;

        public RouteToSession(String sessionId, String query, ActorRef<String> replyTo) {
            this.sessionId = sessionId;
            this.query = query;
            this.replyTo = replyTo;
        }
    }

    public static final class EndSession implements Command {
        public final String sessionId;
        public EndSession(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    public static final class SetIntelligenceActors implements Command {
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

    private SessionManagerActor(ActorContext<Command> context) {
        super(context);
        this.activeSessions = new HashMap<>();
        this.historyActor = context.spawn(ConversationHistoryActor.create(), "conversation-history");
        logger.info("SessionManagerActor initialized");
        System.out.println("✅ SessionManagerActor initialized");
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(SessionManagerActor::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Shutdown.class, this::onShutdown)
                .onMessage(SetIntelligenceActors.class, this::onSetIntelligenceActors)
                .onMessage(CreateSession.class, this::onCreateSession)
                .onMessage(RouteToSession.class, this::onRouteToSession)
                .onMessage(EndSession.class, this::onEndSession)
                .build();
    }

    private Behavior<Command> onShutdown(Shutdown cmd) {
        logger.info("SessionManagerActor shutting down gracefully");
        return Behaviors.stopped();
    }

    private Behavior<Command> onSetIntelligenceActors(SetIntelligenceActors cmd) {
        this.classifierActor = cmd.classifierActor;
        this.culturalActor = cmd.culturalActor;
        this.primitivesActor = cmd.primitivesActor;
        this.intelligenceActorsReady = true;

        System.out.println("🔗 SessionManager received intelligence actors!");
        System.out.println("   Classifier: " + classifierActor);
        System.out.println("   Cultural: " + culturalActor);
        System.out.println("   Primitives: " + primitivesActor);
        System.out.println("📋 Active sessions count: " + activeSessions.size());

        for (Map.Entry<String, ActorRef<DiplomaticSessionActor.Command>> entry : activeSessions.entrySet()) {
            entry.getValue().tell(new DiplomaticSessionActor.SetIntelligenceActors(
                    classifierActor, culturalActor, primitivesActor));
        }
        System.out.println("✅ Intelligence actors set for " + activeSessions.size() + " existing sessions");

        return this;
    }

    private Behavior<Command> onCreateSession(CreateSession cmd) {
        String sessionId = UUID.randomUUID().toString();
        logger.info("Creating new session {} for user {}", sessionId, cmd.userId);
        System.out.println("🆕 Creating session: " + sessionId);

        ActorRef<DiplomaticSessionActor.Command> sessionActor = getContext().spawn(
                DiplomaticSessionActor.create(sessionId, cmd.userId, historyActor),
                "session-" + sessionId
        );

        activeSessions.put(sessionId, sessionActor);
        System.out.println("✅ Session actor spawned and stored");

        if (intelligenceActorsReady) {
            sessionActor.tell(new DiplomaticSessionActor.SetIntelligenceActors(
                    classifierActor, culturalActor, primitivesActor));
            System.out.println("✅ Intelligence actors set for new session");
        }

        cmd.replyTo.tell(new SessionCreatedMessage(sessionId, cmd.userId));
        return this;
    }

    private Behavior<Command> onRouteToSession(RouteToSession cmd) {
        System.out.println("📨 SessionManager.onRouteToSession");
        System.out.println("   Session: " + cmd.sessionId);
        System.out.println("   Query: " + cmd.query);

        ActorRef<DiplomaticSessionActor.Command> sessionActor = activeSessions.get(cmd.sessionId);

        if (sessionActor == null) {
            System.out.println("❌ Session not found!");
            cmd.replyTo.tell("Error: Session not found");
            return this;
        }

        System.out.println("✅ Session found: " + sessionActor);

        // First set the response handler
        sessionActor.tell(new DiplomaticSessionActor.SetResponseHandler(cmd.replyTo));
        System.out.println("   ✓ Response handler set");

        // Then send the query
        sessionActor.tell(new DiplomaticSessionActor.ProcessQuery(cmd.query));
        System.out.println("   ✓ ProcessQuery sent");

        return this;
    }

    private Behavior<Command> onEndSession(EndSession cmd) {
        logger.info("Ending session: {}", cmd.sessionId);
        ActorRef<DiplomaticSessionActor.Command> sessionActor = activeSessions.remove(cmd.sessionId);
        if (sessionActor != null) {
            getContext().stop(sessionActor);
        }
        return this;
    }
}