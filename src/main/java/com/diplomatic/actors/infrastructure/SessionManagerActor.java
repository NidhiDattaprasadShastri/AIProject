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

    // ✅ ADDED: Shutdown message for cluster singleton
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
        public final UserQueryMessage queryMessage;
        public RouteToSession(UserQueryMessage queryMessage) {
            this.queryMessage = queryMessage;
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
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(SessionManagerActor::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Shutdown.class, this::onShutdown)  // ✅ ADDED: Handle shutdown
                .onMessage(SetIntelligenceActors.class, this::onSetIntelligenceActors)
                .onMessage(CreateSession.class, this::onCreateSession)
                .onMessage(RouteToSession.class, this::onRouteToSession)
                .onMessage(EndSession.class, this::onEndSession)
                .build();
    }

    // ✅ ADDED: Shutdown handler
    private Behavior<Command> onShutdown(Shutdown cmd) {
        logger.info("SessionManagerActor shutting down gracefully");
        return Behaviors.stopped();
    }

    private Behavior<Command> onSetIntelligenceActors(SetIntelligenceActors cmd) {
        this.classifierActor = cmd.classifierActor;
        this.culturalActor = cmd.culturalActor;
        this.primitivesActor = cmd.primitivesActor;
        this.intelligenceActorsReady = true;

        logger.info("Intelligence actors configured in SessionManager");

        // Update all existing sessions with intelligence actors
        for (ActorRef<DiplomaticSessionActor.Command> sessionActor : activeSessions.values()) {
            sessionActor.tell(new DiplomaticSessionActor.SetIntelligenceActors(
                    classifierActor, culturalActor, primitivesActor
            ));
        }
        logger.info("Intelligence actors set for {} existing sessions", activeSessions.size());

        return this;
    }

    private Behavior<Command> onCreateSession(CreateSession cmd) {
        String sessionId = UUID.randomUUID().toString();
        logger.info("Creating new session {} for user {}", sessionId, cmd.userId);

        // Create session with simplified parameters
        ActorRef<DiplomaticSessionActor.Command> sessionActor = getContext().spawn(
                DiplomaticSessionActor.create(sessionId, cmd.userId, historyActor),
                "session-" + sessionId
        );

        // If intelligence actors are ready, configure them immediately
        if (intelligenceActorsReady) {
            sessionActor.tell(new DiplomaticSessionActor.SetIntelligenceActors(
                    classifierActor, culturalActor, primitivesActor
            ));
            logger.info("Intelligence actors set for new session {}", sessionId);
        } else {
            logger.warn("Intelligence actors not yet ready for session {}, will be set when available", sessionId);
        }

        activeSessions.put(sessionId, sessionActor);
        cmd.replyTo.tell(new SessionCreatedMessage(sessionId, cmd.userId));
        logger.info("Session {} created successfully. Active sessions: {}", sessionId, activeSessions.size());

        return this;
    }

    private Behavior<Command> onRouteToSession(RouteToSession cmd) {
        String sessionId = cmd.queryMessage.getSessionId();
        ActorRef<DiplomaticSessionActor.Command> sessionActor = activeSessions.get(sessionId);

        if (sessionActor == null) {
            logger.warn("No active session found for: {}", sessionId);
            cmd.queryMessage.getReplyTo().tell("Error: Session not found. Please start a new session.");
            return this;
        }

        logger.info("Routing query to session: {}", sessionId);
        sessionActor.tell(new DiplomaticSessionActor.ProcessQuery(cmd.queryMessage));
        return this;
    }

    private Behavior<Command> onEndSession(EndSession cmd) {
        logger.info("Ending session: {}", cmd.sessionId);
        ActorRef<DiplomaticSessionActor.Command> sessionActor = activeSessions.remove(cmd.sessionId);
        if (sessionActor != null) {
            getContext().stop(sessionActor);
            logger.info("Session {} ended. Active sessions: {}", cmd.sessionId, activeSessions.size());
        }
        return this;
    }
}