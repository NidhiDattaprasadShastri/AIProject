# 🎭 Cross-Cultural Diplomatic Assistant

## 📋 Project Overview

An AI-powered cross-cultural diplomatic communication assistant built with **Akka Cluster** and **Claude AI**. The system provides culturally-informed diplomatic guidance using the **IDEA Framework** (Integrated Diplomatic Enterprise Architecture Design) for structured negotiation and communication.

This project demonstrates a production-grade distributed actor system deployed across two cluster nodes, with complete implementation of Akka's communication patterns (tell, ask, forward) and real-time LLM integration.

---

## 🎯 Key Features

### Distributed Architecture
- **2-Node Akka Cluster** with role-based separation
- **Node 1 (Infrastructure)**: Session management, routing, conversation history
- **Node 2 (Intelligence)**: Query classification, cultural analysis, LLM processing

### IDEA Framework Integration
Seven diplomatic primitives for structured negotiation:
- **PROPOSE** - Present new ideas or solutions
- **CLARIFY** - Seek or provide understanding
- **CONSTRAIN** - Define boundaries or requirements
- **REVISE** - Modify proposals based on feedback
- **AGREE** - Reach consensus or accept terms
- **ESCALATE** - Elevate issues to higher authority
- **DEFER** - Postpone decisions for more information

### Intelligent Query Processing
- Automatic classification (Cultural vs. Diplomatic Primitive)
- Country/region detection
- Context-aware routing across cluster nodes
- Real-time cultural intelligence via Claude AI

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    AKKA CLUSTER                              │
│                                                              │
│  ┌────────────────────────┐      ┌─────────────────────────┐│
│  │   NODE 1 (Port 2551)   │◄────►│   NODE 2 (Port 2552)    ││
│  │   Infrastructure       │      │   Intelligence          ││
│  │                        │      │                         ││
│  │  - ClusterSupervisor   │      │  - IntelligenceNode     ││
│  │  - SessionManager      │      │    Supervisor           ││
│  │  - DiplomaticSession   │      │  - ScenarioClassifier   ││
│  │  - ConversationHistory │      │  - CulturalContext      ││
│  │                        │      │  - DiplomaticPrimitives ││
│  │                        │      │  - LLMProcessor         ││
│  └────────────────────────┘      └─────────────────────────┘│
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Message Flow

```
User Query (Node 1 CLI)
    │
    ├─► ClusterSupervisorActor
    │       │
    │       ├─► SessionManagerActor
    │       │       │
    │       │       └─► DiplomaticSessionActor
    │       │               │
    │       │               └─► [CLUSTER BOUNDARY]
    │       │                       │
    │       │                       ▼
    │       │               ScenarioClassifierActor (Node 2)
    │       │                       │
    │       │                       ├─► CulturalContextActor
    │       │                       │       │
    │       │                       │       └─► LLMProcessorActor
    │       │                       │               │
    │       │                       │               └─► Claude API
    │       │                       │
    │       │                       └─► DiplomaticPrimitivesActor
    │       │                               │
    │       │                               └─► LLMProcessorActor
    │       │                                       │
    │       │                                       └─► Claude API
    │       │
    │       └─► ConversationHistoryActor (fire-and-forget)
    │
    └─► Response to User
```

---

## 📦 Project Structure

```
diplomatic-assistant/
├── src/
│   ├── main/
│   │   ├── java/com/diplomatic/
│   │   │   │
│   │   │   ├── actors/
│   │   │   │   ├── infrastructure/
│   │   │   │   │   ├── ClusterSupervisorActor.java
│   │   │   │   │   ├── SessionManagerActor.java
│   │   │   │   │   ├── DiplomaticSessionActor.java
│   │   │   │   │   └── ConversationHistoryActor.java
│   │   │   │   │
│   │   │   │   └── intelligence/
│   │   │   │       ├── IntelligenceNodeSupervisor.java
│   │   │   │       ├── ScenarioClassifierActor.java
│   │   │   │       ├── CulturalContextActor.java
│   │   │   │       ├── DiplomaticPrimitivesActor.java
│   │   │   │       └── LLMProcessorActor.java
│   │   │   │
│   │   │   ├── messages/
│   │   │   │   ├── CborSerializable.java (interface)
│   │   │   │   ├── ClassificationResultMessage.java
│   │   │   │   ├── CulturalAnalysisRequest.java
│   │   │   │   ├── CulturalAnalysisRequestMessage.java
│   │   │   │   ├── CulturalAnalysisResponseMessage.java
│   │   │   │   ├── DiplomaticPrimitiveRequestMessage.java
│   │   │   │   ├── DiplomaticPrimitiveResponseMessage.java
│   │   │   │   ├── LLMRequestMessage.java
│   │   │   │   ├── LLMResponseMessage.java
│   │   │   │   ├── RouteToClassifierMessage.java
│   │   │   │   ├── SaveConversationMessage.java
│   │   │   │   └── SessionCreatedMessage.java
│   │   │   │
│   │   │   ├── Node1App.java (Infrastructure Node Entry)
│   │   │   └── Node2App.java (Intelligence Node Entry)
│   │   │
│   │   └── resources/
│   │       ├── application-node1.conf
│   │       ├── application-node2.conf
│   │       └── logback.xml
│   │
│   └── test/
│       └── java/com/diplomatic/
│           └── actors/
│               └── DiplomaticSessionActorTest.java
│
├── pom.xml
├── dependency-reduced-pom.xml
├── start-node1.sh
├── start-node2.sh
├── CLUSTER_DEPLOYMENT.md
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Claude API Key** from Anthropic
- **2 Terminal Windows** (one for each node)

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd diplomatic-assistant
```

2. **Set up Claude API credentials**
```bash
export LLM_API_KEY="sk-ant-your-api-key-here"
```

3. **Compile the project**
```bash
mvn clean compile
```

---

## 🎮 Running the Cluster

### Step 1: Start Node 1 (Infrastructure)

**Terminal 1:**
```bash
chmod +x start-node1.sh
./start-node1.sh
```

**Or manually:**
```bash
mvn exec:java \
    -Dexec.mainClass="com.diplomatic.Node1App" \
    -Dconfig.file=src/main/resources/application-node1.conf
```

**Expected Output:**
```
╔═══════════════════════════════════════════════════════════════╗
║       DIPLOMATIC ASSISTANT - NODE 1 (Infrastructure)         ║
║       Port: 2551 | Roles: [infrastructure, frontend]         ║
╚═══════════════════════════════════════════════════════════════╝

🚀 Node 1 starting...
📍 Address: akka://DiplomaticAssistantSystem@127.0.0.1:2551
🎭 Roles: [infrastructure, frontend]
⏳ Waiting for cluster formation...
```

### Step 2: Start Node 2 (Intelligence)

**Terminal 2:**
```bash
chmod +x start-node2.sh
./start-node2.sh
```

**Or manually:**
```bash
mvn exec:java \
    -Dexec.mainClass="com.diplomatic.Node2App" \
    -Dconfig.file=src/main/resources/application-node2.conf
```

**Expected Output:**
```
╔═══════════════════════════════════════════════════════════════╗
║       DIPLOMATIC ASSISTANT - NODE 2 (Intelligence)           ║
║       Port: 2552 | Roles: [intelligence, backend]            ║
╚═══════════════════════════════════════════════════════════════╝

🤖 Using LLM Provider: CLAUDE
✓ API Key configured
🚀 Node 2 starting...
📍 Address: akka://DiplomaticAssistantSystem@127.0.0.1:2552
```

### Step 3: Verify Cluster Formation

Both terminals should show:
```
✅ Member UP: akka://DiplomaticAssistantSystem@127.0.0.1:2551
✅ Member UP: akka://DiplomaticAssistantSystem@127.0.0.1:2552
🎉 CLUSTER READY! 2 nodes connected
```

### Step 4: Use the Interactive CLI

After ~15 seconds, Node 1 will start the interactive CLI:

```
╔═══════════════════════════════════════════════════════════════╗
║  CROSS-CULTURAL DIPLOMATIC ASSISTANT                         ║
║  Powered by Akka Cluster & IDEA Framework                    ║
╚═══════════════════════════════════════════════════════════════╝

Enter your name (or press Enter for 'Diplomat'): 
```

---

## 💬 Usage Examples

### Cultural Questions

```
You: How should I greet Japanese diplomats at a formal meeting?

╔═══════════════════════════════════════════════════════════════╗
║  DIPLOMATIC ASSISTANT RESPONSE                                ║
╚═══════════════════════════════════════════════════════════════╝

In Japanese diplomatic culture, formal greetings emphasize respect 
and hierarchy:

1. **Bow First**: A respectful bow (15-30 degrees) before any handshake
2. **Business Card Exchange**: Use both hands, read carefully before 
   pocketing (meishi koukan)
3. **Titles Matter**: Address by title + san (e.g., "Ambassador Tanaka-san")
4. **Wait for Seniority**: Let the highest-ranking person initiate

Avoid: Overly firm handshakes, prolonged direct eye contact, using 
first names immediately.

───────────────────────────────────────────────────────────────
```

### Diplomatic Primitives

```
You: How should I propose a trade agreement with Germany?

╔═══════════════════════════════════════════════════════════════╗
║  DIPLOMATIC ASSISTANT RESPONSE                                ║
╚═══════════════════════════════════════════════════════════════╝

# Strategy: PROPOSE Primitive
Germans value directness, thoroughness, and data-driven proposals.

## Key Actions:
1. **Prepare Comprehensive Documentation**: Detailed analysis with 
   supporting data and risk assessments
2. **Be Direct and Clear**: Avoid ambiguity; state terms explicitly
3. **Anticipate Scrutiny**: Prepare for critical questions and 
   challenges to assumptions
4. **Show Long-term Value**: Emphasize sustainability and mutual benefit

## Expected Outcomes:
- Thorough questioning of all assumptions
- Requests for additional documentation
- Focus on legal compliance and process adherence

## Next Steps:
1. Develop formal written proposal with appendices
2. Schedule presentation meeting (allow 2+ hours)
3. Prepare backup data for every claim
4. Allow 2-4 weeks for internal review process

[Primitive: PROPOSE]

───────────────────────────────────────────────────────────────
```

### Available Commands

- **help** - Display example queries and usage guide
- **exit** or **quit** - End session and shutdown
- Any other input - Process as diplomatic query

---

## 🔑 API Setup

### Getting Your Claude API Key

1. Visit [console.anthropic.com](https://console.anthropic.com)
2. Sign up or log in to your account
3. Navigate to **API Keys** in the sidebar
4. Click **Create Key**
5. Copy your API key (starts with `sk-ant-`)
6. Set the environment variable:

```bash
export LLM_API_KEY="sk-ant-your-key-here"
```

**Model Used:** `claude-sonnet-4-20250514`

---

## ⚙️ Configuration

### Node 1 Configuration (`application-node1.conf`)

```hocon
akka {
  loglevel = "INFO"
  actor {
    provider = cluster
    serialization-bindings {
      "com.diplomatic.messages.CborSerializable" = jackson-cbor
    }
  }
  remote.artery {
    canonical {
      hostname = "127.0.0.1"
      port = 2551
    }
  }
  cluster {
    seed-nodes = [
      "akka://DiplomaticAssistantSystem@127.0.0.1:2551",
      "akka://DiplomaticAssistantSystem@127.0.0.1:2552"
    ]
    roles = ["infrastructure", "frontend"]
  }
}
```

### Node 2 Configuration (`application-node2.conf`)

```hocon
akka {
  loglevel = "INFO"
  actor {
    provider = cluster
    serialization-bindings {
      "com.diplomatic.messages.CborSerializable" = jackson-cbor
    }
  }
  remote.artery {
    canonical {
      hostname = "127.0.0.1"
      port = 2552
    }
  }
  cluster {
    seed-nodes = [
      "akka://DiplomaticAssistantSystem@127.0.0.1:2551",
      "akka://DiplomaticAssistantSystem@127.0.0.1:2552"
    ]
    roles = ["intelligence", "backend"]
  }
}
```

---

## 🎓 Academic Context - IDEA Framework

This project implements the **IDEA Framework** (Integrated Diplomatic Enterprise Architecture Design), which applies computational diplomacy concepts to enterprise architecture and international relations.

### Research Foundation

The IDEA Framework treats diplomatic dialogue and technical architecture as integrated systems rather than separate processes. Key concepts:

- **Computational Diplomacy Primitives**: Formalized diplomatic actions (PROPOSE, CLARIFY, CONSTRAIN, etc.)
- **Cross-Cultural Communication Models**: Hofstede dimensions, cultural intelligence integration
- **Enterprise Architecture Alignment**: TOGAF and Zachman Framework integration
- **Transparent AI Systems**: Using open models (Claude) with reproducible prompting

### Academic Validation

This framework has been validated through expert interviews, including:
- Turkish Ambassador to Mauritania (confirmed power dynamics challenges)
- Enterprise architecture practitioners across multiple sectors
- Cross-cultural communication experts

---

## 📚 Project Requirements Demonstrated

This project fulfills all academic requirements:

### ✅ 1. Akka Cluster (2 Nodes)
- Node 1 (Port 2551): Infrastructure/Frontend
- Node 2 (Port 2552): Intelligence/Backend
- Proper seed node configuration and cluster formation

### ✅ 2. Service-Specific Actors

**Node 1 (4 actors):**
- ClusterSupervisorActor - Cluster management
- SessionManagerActor - Session lifecycle
- DiplomaticSessionActor - Query orchestration
- ConversationHistoryActor - Persistence

**Node 2 (5 actors):**
- IntelligenceNodeSupervisor - Intelligence coordination
- ScenarioClassifierActor - Query classification
- CulturalContextActor - Cultural intelligence
- DiplomaticPrimitivesActor - IDEA Framework logic
- LLMProcessorActor - Claude API integration

### ✅ 3. Communication Patterns

**TELL (fire-and-forget):**
```java
// DiplomaticSessionActor → ConversationHistoryActor
historyManager.tell(new ConversationHistoryActor.SaveConversation(
    new SaveConversationMessage(sessionId, query, response)));
```

**ASK (request-response via adapter):**
```java
// DiplomaticSessionActor → ScenarioClassifierActor
ActorRef<ClassificationResultMessage> adapter = getContext().messageAdapter(
    ClassificationResultMessage.class,
    result -> new HandleClassification(result, query)
);
classifierActor.tell(new RouteToClassifierMessage(sessionId, query, adapter));
```

**FORWARD (preserve sender context):**
```java
// DiplomaticSessionActor → CulturalContextActor (preserving replyTo)
culturalActor.tell(new CulturalAnalysisRequest(
    query, country, adapter)); // adapter preserves original sender
```

### ✅ 4. LLM Integration
- LLMProcessorActor handles all Claude API communication
- Async processing using CompletableFuture
- Error handling and fallback responses
- Model: claude-sonnet-4-20250514

### ✅ 5. Complete Message Flow
1. User query → Node 1 CLI
2. Route through ClusterSupervisor → SessionManager → DiplomaticSession
3. Ask classifier (Node 2) for scenario detection
4. Forward to Cultural/Primitives actors
5. LLM processing via Claude API
6. Tell history actor (fire-and-forget)
7. Return response to user

---

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=DiplomaticSessionActorTest
```

### Manual Testing Checklist

- [ ] Both nodes start successfully
- [ ] Cluster forms (2 nodes connected)
- [ ] Intelligence actors register with receptionist
- [ ] Session creation works
- [ ] Cultural queries route correctly
- [ ] Primitive queries route correctly
- [ ] Claude API responses are received
- [ ] Conversation history saves
- [ ] Graceful shutdown works

---

## 🛠️ Troubleshooting

### Cluster Won't Form

**Problem:** Nodes don't see each other

**Solution:**
```bash
# Check ports are available
lsof -i :2551
lsof -i :2552

# Ensure both nodes use same system name
grep "DiplomaticAssistantSystem" src/main/resources/*.conf
```

### API Key Not Working

**Problem:** `LLM_API_KEY not found` or API errors

**Solution:**
```bash
# Verify environment variable
echo $LLM_API_KEY

# Re-export if needed
export LLM_API_KEY="sk-ant-your-key-here"

# Check API key is valid at console.anthropic.com
```

### Node 2 Won't Start

**Problem:** Intelligence actors fail to initialize

**Solution:**
- Ensure Node 1 is running first
- Wait 5-10 seconds after Node 1 starts
- Check Node 1 logs show "CLUSTER READY"
- Verify API key is set

### "Actor Not Found" Errors

**Problem:** Intelligence actors not discovered

**Solution:**
- Wait 10-15 seconds after cluster formation
- Check Node 2 logs show "registered with receptionist"
- Verify both nodes show "CLUSTER READY"
- Restart both nodes if needed

---

## 📊 Performance Characteristics

- **Concurrent Sessions**: Up to 100 active sessions per node
- **Response Time**: 2-5 seconds (depends on Claude API latency)
- **Actor Throughput**: 16 concurrent operations per node
- **Fault Tolerance**: Automatic restart on failure (3 attempts)
- **Cluster Recovery**: Automatic rejoin after network partition

---

## 🔒 Security Considerations

- **API Keys**: Never commit to version control
- **Environment Variables**: Always use for sensitive data
- **Session Data**: Stored in-memory only (not persistent)
- **Network**: Local deployment only (127.0.0.1)
- **Logging**: Sensitive data not logged by default

---

## 📝 Building for Production

### Create Executable JAR

```bash
mvn clean package
```

This creates: `target/AIProject-1.0-SNAPSHOT.jar`

### Run from JAR

```bash
# Node 1
java -Dconfig.file=src/main/resources/application-node1.conf \
     -jar target/AIProject-1.0-SNAPSHOT.jar \
     com.diplomatic.Node1App

# Node 2
java -Dconfig.file=src/main/resources/application-node2.conf \
     -jar target/AIProject-1.0-SNAPSHOT.jar \
     com.diplomatic.Node2App
```

---

## 🚦 Stopping the Cluster

**Graceful Shutdown:**
1. Type `exit` in Node 1 CLI
2. Press `Ctrl+C` in Node 2 terminal
3. Press `Ctrl+C` in Node 1 terminal (if needed)

**Force Stop:**
```bash
# Find processes
ps aux | grep java | grep Diplomatic

# Kill processes
kill -9 <PID>
```

---

## 📖 Additional Documentation

- **CLUSTER_DEPLOYMENT.md** - Detailed deployment guide with screenshots
- **pom.xml** - Maven dependencies and build configuration
- **logback.xml** - Logging configuration

---

## 👥 Author

**Yasmin Almousa**  
MS in Software Engineering Systems  
Northeastern University
**Nidhi Dattaprasad Shastri**  
MS in Software Engineering Systems  
Northeastern University
 
**Course**: CSYE 7374 - Intro to AI Agent Infrastructure

---

## 🎯 Project Status

**Status:** ✅ Complete and Operational

**Implemented Features:**
- ✅ 2-node Akka Cluster with role separation
- ✅ 9 service-specific actors (4 on Node 1, 5 on Node 2)
- ✅ All communication patterns (tell, ask, forward)
- ✅ Claude AI integration
- ✅ Complete message flow across cluster
- ✅ IDEA Framework diplomatic primitives
- ✅ Cultural intelligence system
- ✅ Interactive CLI interface
- ✅ Conversation history persistence
- ✅ Error handling and fault tolerance

**Future Enhancements:**
- Web-based user interface
- REST API for programmatic access
- Multi-language support (beyond English)
- Persistent conversation storage (database)
- Analytics dashboard
- Additional LLM providers

---

## 📜 License

This is an academic research project developed for educational purposes at Northeastern University.

---

## 🙏 Acknowledgments

- **Anthropic** - Claude AI API
- **Lightbend/Akka** - Actor model framework
- **Northeastern University** - Academic support
- **Professor Yusuf Ozbek** - Project guidance
- **Professor Kal Bugrara** - Project guidance
- **Ambassador Emrah Bey** - IDEA Framework validation

---

**Last Updated:** December 2025  
**Version:** 1.0  
**Model:** claude-sonnet-4-20250514