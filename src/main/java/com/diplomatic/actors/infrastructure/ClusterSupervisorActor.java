package com.diplomatic.actors.infrastructure;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Subscribe;
import akka.cluster.ClusterEvent;
import com.diplomatic.actors.intelligence.IntelligenceNodeSupervisor;
import com.diplomatic.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.collection.Iterator;

/**
 * Cluster-Aware Supervisor for Node 1 (Infrastructure)
 * COMPLETE CORRECTED VERSION - NO MORE ERRORS!
 */
public class ClusterSupervisorActor extends AbstractBehavior<ClusterSupervisorActor.Command> {

    private final Logger logger = LoggerFactory.getLogger(ClusterSupervisorActor.class);
    private final Cluster cluster;
    private final ActorRef<SessionManagerActor.Command> sessionManager;
    private boolean clusterReady = false;

    private ActorRef<RouteToClassifierMessage> discoveredClassifier;
    private ActorRef<CulturalAnalysisRequestMessage> discoveredCultural;
    private ActorRef<DiplomaticPrimitiveRequestMessage> discoveredPrimitives;

    public static final ServiceKey<Command> SUPERVISOR_KEY =
            ServiceKey.create(Command.class, "cluster-supervisor");

    // ========================================================================
    // COMMAND MESSAGES
    // ========================================================================

    public interface Command {}

    public static final class MonitorCluster implements Command {}

    public static final class CreateSession implements Command {
        public final String userId;
        public final ActorRef<SessionCreatedMessage> replyTo;

        public CreateSession(String userId, ActorRef<SessionCreatedMessage> replyTo) {
            this.userId = userId;
            this.replyTo = replyTo;
        }
    }

    // CORRECTED: RouteQuery with separate fields (NO UserQueryMessage!)
    public static final class RouteQuery implements Command {
        public final String sessionId;
        public final String query;
        public final ActorRef<String> replyTo;

        public RouteQuery(String sessionId, String query, ActorRef<String> replyTo) {
            this.sessionId = sessionId;
            this.query = query;
            this.replyTo = replyTo;
        }
    }

    private static final class ClusterEventMessage implements Command {
        public final ClusterEvent.MemberEvent event;
        public ClusterEventMessage(ClusterEvent.MemberEvent event) {
            this.event = event;
        }
    }

    private static final class ClusterReachabilityChange implements Command {
        public final ClusterEvent.ReachabilityEvent event;
        public ClusterReachabilityChange(ClusterEvent.ReachabilityEvent event) {
            this.event = event;
        }
    }

    private static final class ActorsRegistered implements Command {
        public final Receptionist.Listing listing;
        public ActorsRegistered(Receptionist.Listing listing) {
            this.listing = listing;
        }
    }

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    public static Behavior<Command> createInfrastructure() {
        return Behaviors.setup(ClusterSupervisorActor::new);
    }

    private ClusterSupervisorActor(ActorContext<Command> context) {
        super(context);
        this.cluster = Cluster.get(context.getSystem());

        context.getSystem().receptionist().tell(
                Receptionist.register(SUPERVISOR_KEY, context.getSelf())
        );

        // Regular child actor (NO singleton!)
        this.sessionManager = context.spawn(
                SessionManagerActor.create(),
                "session-manager"
        );

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  NODE 1: Infrastructure Supervisor Started                ║");
        System.out.println("║  Actors: SessionManager (NO SINGLETON), ConversationHistory║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println("📡 Registered ClusterSupervisor with receptionist");
        System.out.println("✅ SessionManager spawned as regular actor");
    }

    // ========================================================================
    // MESSAGE HANDLERS
    // ========================================================================

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(MonitorCluster.class, this::onMonitorCluster)
                .onMessage(ActorsRegistered.class, this::onActorsRegistered)
                .onMessage(CreateSession.class, this::onCreateSession)
                .onMessage(RouteQuery.class, this::onRouteQuery)
                .onMessage(ClusterEventMessage.class, this::onClusterEvent)
                .onMessage(ClusterReachabilityChange.class, this::onReachabilityChange)
                .build();
    }

    private Behavior<Command> onMonitorCluster(MonitorCluster cmd) {
        ActorRef<ClusterEvent.MemberEvent> memberEventAdapter = getContext()
                .messageAdapter(ClusterEvent.MemberEvent.class, ClusterEventMessage::new);

        cluster.subscriptions().tell(Subscribe.create(memberEventAdapter, ClusterEvent.MemberEvent.class));

        ActorRef<ClusterEvent.ReachabilityEvent> reachabilityAdapter = getContext()
                .messageAdapter(ClusterEvent.ReachabilityEvent.class, ClusterReachabilityChange::new);

        cluster.subscriptions().tell(Subscribe.create(reachabilityAdapter, ClusterEvent.ReachabilityEvent.class));

        System.out.println("📡 Cluster monitoring activated");
        System.out.println("🔍 Current cluster state: " + cluster.state());
        System.out.println("📊 Current members in cluster:");

        Iterator<akka.cluster.Member> members = cluster.state().members().iterator();
        while (members.hasNext()) {
            akka.cluster.Member member = members.next();
            System.out.println("   - " + member.uniqueAddress() + " [" + member.roles() + "]");
        }

        return this;
    }

    private Behavior<Command> onActorsRegistered(ActorsRegistered msg) {
        if (msg.listing.isForKey(IntelligenceNodeSupervisor.CLASSIFIER_KEY)) {
            var instances = msg.listing.getServiceInstances(IntelligenceNodeSupervisor.CLASSIFIER_KEY);
            if (!instances.isEmpty()) {
                discoveredClassifier = instances.iterator().next();
                System.out.println("✅ VERIFIED: Classifier actor registered");
                System.out.println("   Location: " + discoveredClassifier);

                // 🧪 DIRECT TEST - Does Node 1 → Node 2 messaging work at all?
                System.out.println("\n🧪🧪🧪 TESTING: Sending test message to Node 2 classifier...");

                ActorRef<ClassificationResultMessage> testAdapter = getContext().messageAdapter(
                        ClassificationResultMessage.class,
                        result -> {
                            System.out.println("\n✅✅✅ TEST SUCCESSFUL! Node 1 CAN talk to Node 2!");
                            System.out.println("    Received: " + result.getScenario() + " - " + result.getDetectedCountry());
                            System.out.println("    This proves cluster messaging works!\n");
                            return new MonitorCluster();
                        }
                );

                RouteToClassifierMessage testMsg = new RouteToClassifierMessage(
                        "test-session",
                        "test query about japan culture",
                        testAdapter
                );

                discoveredClassifier.tell(testMsg);
                System.out.println("🧪 Test message sent!");
                System.out.println("   Waiting for response from Node 2...\n");
            }
        } else if (msg.listing.isForKey(IntelligenceNodeSupervisor.CULTURAL_KEY)) {
            var instances = msg.listing.getServiceInstances(IntelligenceNodeSupervisor.CULTURAL_KEY);
            if (!instances.isEmpty()) {
                discoveredCultural = instances.iterator().next();
                System.out.println("✅ VERIFIED: Cultural actor registered");
                System.out.println("   Location: " + discoveredCultural);
            }
        } else if (msg.listing.isForKey(IntelligenceNodeSupervisor.PRIMITIVES_KEY)) {
            var instances = msg.listing.getServiceInstances(IntelligenceNodeSupervisor.PRIMITIVES_KEY);
            if (!instances.isEmpty()) {
                discoveredPrimitives = instances.iterator().next();
                System.out.println("✅ VERIFIED: Primitives actor registered");
                System.out.println("   Location: " + discoveredPrimitives);
            }
        }

        if (discoveredClassifier != null && discoveredCultural != null && discoveredPrimitives != null) {
            System.out.println("🔗 All intelligence actors discovered!");
            sessionManager.tell(new SessionManagerActor.SetIntelligenceActors(
                    discoveredClassifier, discoveredCultural, discoveredPrimitives));
        }

        return this;
    }

    private Behavior<Command> onClusterEvent(ClusterEventMessage msg) {
        System.out.println("🔔 Cluster event: " + msg.event.getClass().getSimpleName());

        if (msg.event instanceof ClusterEvent.MemberUp) {
            ClusterEvent.MemberUp memberUp = (ClusterEvent.MemberUp) msg.event;
            System.out.println("✅ Member UP: " + memberUp.member().uniqueAddress() +
                    " with roles " + memberUp.member().roles());

            int memberCount = cluster.state().members().size();
            System.out.println("🔍 Total members in cluster: " + memberCount);

            if (memberCount >= 2 && !clusterReady) {
                clusterReady = true;
                System.out.println("🎉 CLUSTER READY! " + memberCount + " nodes connected");
                System.out.println("✓ Infrastructure node operational");
                System.out.println("🔍 Subscribing to actor registrations...");

                ActorRef<Receptionist.Listing> adapter = getContext()
                        .messageAdapter(Receptionist.Listing.class, ActorsRegistered::new);

                getContext().getSystem().receptionist().tell(
                        Receptionist.subscribe(IntelligenceNodeSupervisor.CLASSIFIER_KEY, adapter));
                getContext().getSystem().receptionist().tell(
                        Receptionist.subscribe(IntelligenceNodeSupervisor.CULTURAL_KEY, adapter));
                getContext().getSystem().receptionist().tell(
                        Receptionist.subscribe(IntelligenceNodeSupervisor.PRIMITIVES_KEY, adapter));
            }
        } else if (msg.event instanceof ClusterEvent.MemberJoined) {
            ClusterEvent.MemberJoined joined = (ClusterEvent.MemberJoined) msg.event;
            System.out.println("👋 Member JOINED: " + joined.member().uniqueAddress());
        } else if (msg.event instanceof ClusterEvent.MemberRemoved) {
            ClusterEvent.MemberRemoved removed = (ClusterEvent.MemberRemoved) msg.event;
            System.out.println("⚠️ Member REMOVED: " + removed.member().uniqueAddress());
            clusterReady = false;
        }

        return this;
    }

    private Behavior<Command> onReachabilityChange(ClusterReachabilityChange msg) {
        if (msg.event instanceof ClusterEvent.UnreachableMember) {
            ClusterEvent.UnreachableMember unreachable = (ClusterEvent.UnreachableMember) msg.event;
            System.out.println("❌ Node UNREACHABLE: " + unreachable.member().uniqueAddress());
        } else if (msg.event instanceof ClusterEvent.ReachableMember) {
            ClusterEvent.ReachableMember reachable = (ClusterEvent.ReachableMember) msg.event;
            System.out.println("✅ Node REACHABLE: " + reachable.member().uniqueAddress());
        }
        return this;
    }

    private Behavior<Command> onCreateSession(CreateSession cmd) {
        if (!clusterReady) {
            System.out.println("⏳ Cluster not ready yet");
            cmd.replyTo.tell(new SessionCreatedMessage("pending", cmd.userId));
            return this;
        }

        System.out.println("➡️  Routing session creation to SessionManager: " + cmd.userId);
        sessionManager.tell(new SessionManagerActor.CreateSession(cmd.userId, cmd.replyTo));
        return this;
    }

    // THIS IS THE CORRECTED METHOD!
    private Behavior<Command> onRouteQuery(RouteQuery cmd) {
        if (!clusterReady) {
            System.out.println("⏳ Cluster not ready");
            cmd.replyTo.tell("System initializing, please wait...");
            return this;
        }

        System.out.println("➡️  Routing query to SessionManager");
        System.out.println("   Session: " + cmd.sessionId);
        System.out.println("   Query: " + cmd.query);

        // CORRECTED: Pass 3 separate parameters instead of UserQueryMessage
        sessionManager.tell(new SessionManagerActor.RouteToSession(
                cmd.sessionId,
                cmd.query,
                cmd.replyTo
        ));

        System.out.println("✅ Routed to SessionManager");

        return this;
    }
}