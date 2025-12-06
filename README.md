# 🎭 Diplomatic Assistant - Infrastructure Layer (Option A)

## 📋 Project Overview

This is the **infrastructure and foundation layer** of the Cross-Cultural Diplomatic Assistant, implementing the **Akka Actor Model** for concurrent session management, intelligent message routing, and conversation persistence.

### 🎯 Purpose

Provides a scalable, fault-tolerant backend infrastructure that manages diplomatic consultation sessions and coordinates with AI intelligence actors to deliver culturally-informed diplomatic guidance.

---

## 🏗️ Architecture

### Actor Hierarchy
```
SupervisorActor (Root Guardian)
├── SessionManagerActor
│   └── DiplomaticSessionActor (per user session)
└── ConversationHistoryActor
```

### Integration with Option B (Intelligence Layer)
```
DiplomaticSessionActor
    ↓
[Routes to Option B actors]
    ├→ ScenarioClassifierActor
    ├→ CulturalContextActor  
    ├→ DiplomaticPrimitivesActor
    └→ LLMProcessorActor
```

---

## 🎭 Components

### **1. SupervisorActor**
- Root system guardian
- Manages actor lifecycle and fault tolerance
- Implements supervision strategies (restart on failure)
- Spawns SessionManager and ConversationHistory actors

### **2. SessionManagerActor**
- Creates and manages user sessions
- Routes queries to appropriate session actors
- Handles session lifecycle (create/destroy)
- Maintains map of active sessions

### **3. DiplomaticSessionActor**
- Orchestrates individual user conversations
- Routes queries to ScenarioClassifierActor (Option B)
- Processes responses from intelligence actors
- Maintains conversation context
- **Currently operates in MOCK mode** until Option B integration

### **4. ConversationHistoryActor**
- Persists all conversation data
- In-memory storage with logging
- Maintains conversation statistics
- Provides history retrieval functionality

---

## 📦 Message Protocol

All message classes defined in `com.diplomatic.messages`:

### Session Management
- `StartSessionMessage` - Create new session
- `SessionCreatedMessage` - Session confirmation
- `EndSessionMessage` - Terminate session
- `SessionEndedMessage` - Termination confirmation

### Query Processing
- `UserQueryMessage` - User input
- `QueryResponseMessage` - System response

### Routing (Integration with Option B)
- `RouteToClassifierMessage` - Send to classifier
- `ClassificationResultMessage` - Classification result
- `CulturalAnalysisRequest/Response` - Cultural intelligence
- `DiplomaticPrimitiveRequest/Response` - IDEA framework

### LLM Processing (Option B)
- `LLMRequestMessage` - LLM API call
- `LLMResponseMessage` - LLM response

### Persistence
- `SaveConversationMessage` - Save conversation turn
- `ConversationSavedMessage` - Save confirmation

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Git** (for version control)

### Installation
```bash
# Clone repository
git clone <repository-url>
cd diplomatic-assistant

# Compile
mvn clean compile

# Run tests
mvn test

# Run application
mvn exec:java -Dexec.mainClass="com.diplomatic.Main"
```

### Building Executable JAR
```bash
mvn clean package
java -jar target/AIProject-1.0-SNAPSHOT.jar
```

---

## 💻 Usage

### Starting the CLI
```bash
mvn exec:java -Dexec.mainClass="com.diplomatic.Main"
```

### Example Interaction
```
╔════════════════════════════════════════════════════════════════╗
║        CROSS-CULTURAL DIPLOMATIC ASSISTANT                     ║
║        Powered by AKKA Actor Model & AI                        ║
╚════════════════════════════════════════════════════════════════╝

Enter your name or ID: John Diplomat

✅ Session created successfully!
📋 Session ID: a1b2c3d4-5678-90ef-ghij-klmnopqrstuv

💬 Start your consultation:

You: How should I negotiate with Japan?

🤖 Assistant:
[MOCK MODE] I understand you're asking about diplomatic communication...
```

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

### Current Test Coverage

- ✅ DiplomaticSessionActor - Query processing in mock mode
- ✅ ConversationHistoryActor - Data persistence
- ⏳ Integration tests (pending Option B completion)

---

## ⚙️ Configuration

### `application.conf`
```hocon
akka {
  loglevel = "INFO"
  actor {
    provider = local
    default-dispatcher {
      fixed-pool-size = 16
    }
  }
}

diplomatic-assistant {
  session-timeout-minutes = 30
  max-active-sessions = 100
}
```

### Logging

Configured in `src/main/resources/logback.xml`
- Default level: INFO
- Outputs to console
- Includes actor system logs

---

## 🔌 Integration Points for Option B

### Required Intelligence Actors

Option B needs to provide these actor references:
```java
ActorRef<RouteToClassifierMessage> classifierActor
ActorRef<CulturalAnalysisRequestMessage> culturalActor
ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor
```

### Integration Method

Pass actor references to SessionManager:
```java
sessionManager.tell(new SessionManagerActor.SetIntelligenceActors(
    classifierActor,
    culturalActor,
    primitivesActor
));
```

### Message Flow
```
User → DiplomaticSessionActor
    ↓
RouteToClassifierMessage → ScenarioClassifierActor (Option B)
    ↓
ClassificationResultMessage → DiplomaticSessionActor
    ↓
[Routes to Cultural OR Primitives actor based on classification]
    ↓
Response → User
    ↓
SaveConversationMessage → ConversationHistoryActor
```

---

## 📊 Project Status

### ✅ Completed

- [x] All 4 infrastructure actors
- [x] Complete message protocol (14 messages)
- [x] Terminal CLI interface
- [x] Maven project setup
- [x] Configuration files
- [x] Unit tests
- [x] Mock mode operation

### ⏳ Pending (Integration Weekend)

- [ ] Integration with Option B intelligence actors
- [ ] End-to-end testing with real AI responses
- [ ] Performance optimization
- [ ] Extended error handling

---

## 📁 Project Structure
```
diplomatic-assistant/
├── pom.xml
├── README.md
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/com/diplomatic/
│   │   │   ├── actors/infrastructure/
│   │   │   │   ├── SupervisorActor.java
│   │   │   │   ├── SessionManagerActor.java
│   │   │   │   ├── DiplomaticSessionActor.java
│   │   │   │   └── ConversationHistoryActor.java
│   │   │   ├── messages/
│   │   │   │   ├── StartSessionMessage.java
│   │   │   │   ├── UserQueryMessage.java
│   │   │   │   ├── RouteToClassifierMessage.java
│   │   │   │   └── ... (14 total)
│   │   │   ├── models/
│   │   │   │   ├── Session.java
│   │   │   │   ├── ConversationEntry.java
│   │   │   │   └── UserContext.java
│   │   │   ├── Main.java
│   │   │   └── DiplomaticAssistantCLI.java
│   │   └── resources/
│   │       ├── application.conf
│   │       └── logback.xml
│   └── test/
│       └── java/com/diplomatic/actors/
│           └── DiplomaticSessionActorTest.java
└── target/ (generated)
```

---

## 🤝 Contributing

This is **Option A** of a two-person project:

- **Option A (this repo)**: Infrastructure & Foundation
- **Option B**: Intelligence & Domain Logic (AI actors)

### For Option B Developer

All message classes are ready in `com.diplomatic.messages`. Use them to build:
- ScenarioClassifierActor
- CulturalContextActor
- DiplomaticPrimitivesActor
- LLMProcessorActor

---

## 🐛 Troubleshooting

### "Cannot find symbol" errors
```bash
mvn clean compile
```

### Tests fail
```bash
mvn clean test
```

### Application won't start
Check Java version:
```bash
java -version  # Should be 17+
```

---

## 📝 License

[Your License Here]

---

## 👤 Author

**Option A - Infrastructure Lead**  
[Your Name]  
[Your Email]

**Project**: Cross-Cultural Diplomatic Assistant  
**Course**: [Your Course Name]  
**Date**: December 2024

---

## 🎯 Next Steps

1. ✅ Complete Option A development (DONE)
2. ⏳ Wait for Option B completion
3. 🔄 Integration weekend (Dec 7-8)
4. 🚀 Final testing and deployment

---

**Status**: ✅ Option A Complete - Ready for Integration