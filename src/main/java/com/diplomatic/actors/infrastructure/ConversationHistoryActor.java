package com.diplomatic.actors.infrastructure;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.diplomatic.messages.SaveConversationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
public class ConversationHistoryActor extends AbstractBehavior<ConversationHistoryActor.Command> {

    private final Logger logger = LoggerFactory.getLogger(ConversationHistoryActor.class);
    private final Map<String, List<ConversationTurn>> conversationHistory;
    private int totalConversationsSaved = 0;

    public interface Command {}

    public static final class SaveConversation implements Command {
        public final SaveConversationMessage message;
        public SaveConversation(SaveConversationMessage message) {
            this.message = message;
        }
    }

    public static final class GetHistory implements Command {
        public final String sessionId;
        public GetHistory(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    public static final class GetStats implements Command {
        public static final GetStats INSTANCE = new GetStats();
        private GetStats() {}
    }

    public static final class ClearHistory implements Command {
        public final String sessionId;
        public ClearHistory(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    private ConversationHistoryActor(ActorContext<Command> context) {
        super(context);
        this.conversationHistory = new HashMap<>();
        logger.info("ConversationHistoryActor initialized");
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(ConversationHistoryActor::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(SaveConversation.class, this::onSaveConversation)
                .onMessage(GetHistory.class, this::onGetHistory)
                .onMessage(GetStats.class, this::onGetStats)
                .onMessage(ClearHistory.class, this::onClearHistory)
                .build();
    }

    private Behavior<Command> onSaveConversation(SaveConversation cmd) {
        String sessionId = cmd.message.getSessionId();
        String query = cmd.message.getQuery();
        String response = cmd.message.getResponse();
        logger.info("Saving conversation for session {}: query length={}, response length={}",
                sessionId, query.length(), response.length());
        ConversationTurn turn = new ConversationTurn(Instant.now(), query, response);
        conversationHistory.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(turn);
        totalConversationsSaved++;
        int turnCount = conversationHistory.get(sessionId).size();
        logger.info("Conversation saved. Session {} now has {} turns. Total conversations: {}",
                sessionId, turnCount, totalConversationsSaved);
        return this;
    }

    private Behavior<Command> onGetHistory(GetHistory cmd) {
        List<ConversationTurn> history = conversationHistory.get(cmd.sessionId);
        if (history == null || history.isEmpty()) {
            logger.info("No conversation history found for session: {}", cmd.sessionId);
        } else {
            logger.info("Retrieved {} conversation turns for session: {}",
                    history.size(), cmd.sessionId);
            printConversationHistory(cmd.sessionId, history);
        }
        return this;
    }

    private Behavior<Command> onGetStats(GetStats cmd) {
        logger.info("=== Conversation History Statistics ===");
        logger.info("Total sessions: {}", conversationHistory.size());
        logger.info("Total conversations saved: {}", totalConversationsSaved);
        for (Map.Entry<String, List<ConversationTurn>> entry : conversationHistory.entrySet()) {
            logger.info("  Session {}: {} turns", entry.getKey(), entry.getValue().size());
        }
        return this;
    }

    private Behavior<Command> onClearHistory(ClearHistory cmd) {
        List<ConversationTurn> removed = conversationHistory.remove(cmd.sessionId);
        if (removed != null) {
            logger.info("Cleared {} conversation turns for session: {}",
                    removed.size(), cmd.sessionId);
        } else {
            logger.info("No history to clear for session: {}", cmd.sessionId);
        }
        return this;
    }

    private void printConversationHistory(String sessionId, List<ConversationTurn> history) {
        logger.info("=== Conversation History for Session: {} ===", sessionId);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        for (int i = 0; i < history.size(); i++) {
            ConversationTurn turn = history.get(i);
            logger.info("Turn {}: [{}]", i + 1, turn.timestamp);
            logger.info("  Query: {}", truncate(turn.query, 100));
            logger.info("  Response: {}", truncate(turn.response, 100));
        }
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private static class ConversationTurn {
        final Instant timestamp;
        final String query;
        final String response;
        ConversationTurn(Instant timestamp, String query, String response) {
            this.timestamp = timestamp;
            this.query = query;
            this.response = response;
        }
    }
}
