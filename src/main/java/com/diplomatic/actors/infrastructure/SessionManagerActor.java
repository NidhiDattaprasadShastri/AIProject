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
    public interface Command {}

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
                .onMessage(CreateSession.class, this::onCreateSession)
                .onMessage(RouteToSession.class, this::onRouteToSession)
                .onMessage(EndSession.class, this::onEndSession)
                .build();
    }

    private Behavior<Command> onCreateSession(CreateSession cmd) {
        String sessionId = UUID.randomUUID().toString();
        logger.info("Creating new session {} for user {}", sessionId, cmd.userId);
        ActorRef<DiplomaticSessionActor.Command> sessionActor = getContext().spawn(DiplomaticSessionActor.create(sessionId, historyActor),
                        "session-" + sessionId);
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
