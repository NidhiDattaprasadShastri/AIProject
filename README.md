# 🎭 Cross-Cultural Diplomatic Assistant

## 📋 Project Overview

An AI-powered cross-cultural diplomatic communication assistant built with the **Akka Actor Model** and **Large Language Models (LLM)**. The system provides culturally-informed diplomatic guidance using the **IDEA Framework** (Integrated Diplomatic Enterprise Architecture Design) for structured negotiation and communication.

### 🎯 Purpose

Helps diplomats, international business professionals, and cross-cultural communicators navigate complex diplomatic scenarios by:
- **Analyzing cultural contexts** for any country or region
- **Applying diplomatic primitives** (PROPOSE, CLARIFY, CONSTRAIN, REVISE, AGREE, ESCALATE, DEFER)
- **Providing actionable guidance** informed by cultural intelligence
- **Managing concurrent sessions** with fault-tolerant actor architecture

---

## 🏗️ Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    USER INTERFACE LAYER                      │
│                  DiplomaticAssistantCLI                      │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                   INFRASTRUCTURE LAYER                        │
│                                                              │
│  SupervisorActor (Root Guardian)                            │
│       │                                                      │
│       ├──► SessionManagerActor                              │
│       │       └──► DiplomaticSessionActor (per session)     │
│       │                                                      │
│       └──► ConversationHistoryActor                         │
│                                                              │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                   INTELLIGENCE LAYER                         │
│                                                              │
│  IntelligenceSupervisorActor                                │
│       │                                                      │
│       ├──► ScenarioClassifierActor                          │
│       │                                                      │
│       ├──► CulturalContextActor                             │
│       │                                                      │
│       ├──► DiplomaticPrimitivesActor                        │
│       │                                                      │
│       └──► LLMProcessorActor                                │
│                   │                                          │
│                   ├──► Claude API (Anthropic)               │
│                   └──► OpenAI API                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Actor Components

#### Infrastructure Layer (Part A)
- **SupervisorActor**: Root guardian with fault tolerance strategies
- **SessionManagerActor**: Manages user sessions and routing
- **DiplomaticSessionActor**: Orchestrates individual consultations
- **ConversationHistoryActor**: Persists conversation data

#### Intelligence Layer (Part B)
- **IntelligenceSupervisorActor**: Spawns and manages intelligence actors
- **ScenarioClassifierActor**: Classifies queries as CULTURAL or PRIMITIVE
- **CulturalContextActor**: Provides cultural intelligence
- **DiplomaticPrimitivesActor**: Applies IDEA Framework primitives
- **LLMProcessorActor**: Handles API calls to Claude/OpenAI

### Message Flow

```
User Query
    │
    ▼
DiplomaticSessionActor
    │
    ├──► ScenarioClassifierActor
    │         │
    │         └──► Classification Result
    │
    ├──► CulturalContextActor (if CULTURAL)
    │         │
    │         └──► Cultural Analysis
    │
    └──► DiplomaticPrimitivesActor (if PRIMITIVE)
              │
              └──► Diplomatic Guidance
                        │
                        ▼
                   LLMProcessorActor
                        │
                        └──► Claude/OpenAI API
                                  │
                                  ▼
                            Response to User
                                  │
                                  ▼
                        ConversationHistoryActor
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **API Key** for Claude (Anthropic) or OpenAI
- **Git** (for version control)

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd diplomatic-assistant
```

2. **Set up API credentials**

**For Claude (Recommended):**
```bash
export LLM_API_KEY="your-anthropic-api-key"
export LLM_PROVIDER="CLAUDE"
```

**For OpenAI:**
```bash
export LLM_API_KEY="your-openai-api-key"
export LLM_PROVIDER="OPENAI"
```

3. **Compile the project**
```bash
mvn clean compile
```

4. **Run tests**
```bash
mvn test
```

5. **Run the application**
```bash
mvn exec:java -Dexec.mainClass="com.diplomatic.DiplomaticAssistantApp"
```

### Building Executable JAR

```bash
mvn clean package
java -jar target/AIProject-1.0-SNAPSHOT.jar
```

---

## 💻 Usage

### Starting the Application

```bash
mvn exec:java -Dexec.mainClass="com.diplomatic.DiplomaticAssistantApp"
```

### Command-Line Interface

```
╔═══════════════════════════════════════════════════════════════╗
║        CROSS-CULTURAL DIPLOMATIC ASSISTANT                    ║
║        Powered by AKKA Actor Model & AI                       ║
╚═══════════════════════════════════════════════════════════════╝

🤖 Using LLM Provider: CLAUDE
✓ API Key configured
🔡 Starting distributed actor system...

✓ Intelligence actors initialized
✓ System ready

Enter your name or ID (press Enter for auto-generated):
```

### Example Interactions

#### Cultural Question
```
You: How should I greet a Japanese diplomat at a formal meeting?

🤖 Assistant:

In Japanese diplomatic culture, greeting protocols emphasize respect and 
hierarchy. Key considerations:

1. BOW FIRST: A respectful bow (15-30 degrees) before handshake
2. EXCHANGE BUSINESS CARDS: Use both hands, read carefully before pocketing
3. USE TITLES: Address by title + san (e.g., "Ambassador Tanaka-san")
4. WAIT FOR SENIOR: Let the highest-ranking person greet first

Avoid: Overly firm handshakes, prolonged eye contact, casual first names
```

#### Diplomatic Primitive
```
You: How should I propose a new trade agreement with Germany?

🤖 Assistant:

When proposing trade agreements with German counterparts, follow these 
strategies:

STRATEGY: Germans value directness, thoroughness, and data-driven proposals

KEY ACTIONS:
1. Present detailed analysis with supporting data
2. Use clear, direct language without ambiguity
3. Prepare for critical questions and challenges
4. Show long-term sustainability and risk mitigation

EXPECTED OUTCOMES:
- Thorough questioning of assumptions
- Request for additional documentation
- Emphasis on legal compliance and process

NEXT STEPS:
- Prepare comprehensive written proposal
- Schedule formal presentation meeting
- Allow time for internal review process

[Diplomatic Primitive: PROPOSE]
```

### Available Commands

- `help` - Display example queries and usage guide
- `exit` or `quit` - End session and shutdown
- Any other input - Process as diplomatic query

---

## ⚙️ Configuration

### Application Configuration (`application.conf`)

```hocon
akka {
  loglevel = "INFO"
  
  actor {
    provider = local
    
    default-dispatcher {
      type = Dispatcher
      executor = "thread-pool-executor"
      thread-pool-executor {
        fixed-pool-size = 16
      }
      throughput = 5
    }
  }
}

diplomatic-assistant {
  session-timeout-minutes = 30
  max-active-sessions = 100
}
```

### Logging Configuration (`logback.xml`)

Located in `src/main/resources/logback.xml`:
- Default level: INFO
- Console output with timestamps
- Actor system logging enabled

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `LLM_API_KEY` | API key for Claude or OpenAI | Required |
| `LLM_PROVIDER` | `CLAUDE` or `OPENAI` | `CLAUDE` |

---

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=DiplomaticSessionActorTest
```

### Current Test Coverage

- ✅ Actor message handling
- ✅ Session management
- ✅ Message routing
- ✅ Classification logic
- ✅ History persistence

---

## 📦 Project Structure

```
diplomatic-assistant/
├── pom.xml                          # Maven configuration
├── README.md                        # This file
├── .gitignore
│
├── src/
│   ├── main/
│   │   ├── java/com/diplomatic/
│   │   │   │
│   │   │   ├── actors/
│   │   │   │   ├── infrastructure/
│   │   │   │   │   ├── SupervisorActor.java
│   │   │   │   │   ├── SessionManagerActor.java
│   │   │   │   │   ├── DiplomaticSessionActor.java
│   │   │   │   │   └── ConversationHistoryActor.java
│   │   │   │   │
│   │   │   │   └── intelligence/
│   │   │   │       ├── IntelligenceSupervisorActor.java
│   │   │   │       ├── ScenarioClassifierActor.java
│   │   │   │       ├── CulturalContextActor.java
│   │   │   │       ├── DiplomaticPrimitivesActor.java
│   │   │   │       └── LLMProcessorActor.java
│   │   │   │
│   │   │   ├── messages/
│   │   │   │   ├── UserQueryMessage.java
│   │   │   │   ├── ClassificationResultMessage.java
│   │   │   │   ├── CulturalAnalysisRequest.java
│   │   │   │   ├── CulturalAnalysisRequestMessage.java
│   │   │   │   ├── CulturalAnalysisResponseMessage.java
│   │   │   │   ├── DiplomaticPrimitiveRequestMessage.java
│   │   │   │   ├── DiplomaticPrimitiveResponseMessage.java
│   │   │   │   ├── LLMRequestMessage.java
│   │   │   │   ├── LLMResponseMessage.java
│   │   │   │   ├── SaveConversationMessage.java
│   │   │   │   └── ... (17 total message classes)
│   │   │   │
│   │   │   ├── Models/
│   │   │   │   ├── Session.java
│   │   │   │   ├── ConversationEntry.java
│   │   │   │   └── UserContext.java
│   │   │   │
│   │   │   ├── DiplomaticAssistantApp.java    # Main entry point
│   │   │   └── DiplomaticAssistantCLI.java    # User interface
│   │   │
│   │   └── resources/
│   │       ├── application.conf               # Akka configuration
│   │       └── logback.xml                   # Logging configuration
│   │
│   └── test/
│       └── java/com/diplomatic/
│           └── actors/
│               └── DiplomaticSessionActorTest.java
│
└── target/                                    # Compiled output (generated)
```

---

## 🔑 API Setup Guide

### Getting Claude API Key (Anthropic)

1. Visit [console.anthropic.com](https://console.anthropic.com)
2. Sign up or log in
3. Navigate to "API Keys"
4. Create a new key
5. Copy and set as environment variable:
   ```bash
   export LLM_API_KEY="sk-ant-..."
   ```

### Getting OpenAI API Key

1. Visit [platform.openai.com](https://platform.openai.com)
2. Sign up or log in
3. Navigate to "API Keys"
4. Create a new key
5. Copy and set as environment variable:
   ```bash
   export LLM_API_KEY="sk-..."
   export LLM_PROVIDER="OPENAI"
   ```

### Supported Models

- **Claude**: `claude-sonnet-4-20250514` (default)
- **OpenAI**: `gpt-4`

---

## 🎓 IDEA Framework

The system implements the **IDEA Framework** (Integrated Diplomatic Enterprise Architecture Design) with seven diplomatic primitives:

| Primitive | Purpose | Example Usage |
|-----------|---------|---------------|
| **PROPOSE** | Present new ideas, terms, or solutions | "How should I propose a partnership with China?" |
| **CLARIFY** | Seek or provide understanding | "Help me clarify contract terms with French partners" |
| **CONSTRAIN** | Define boundaries and limitations | "Setting deadlines in Middle Eastern negotiations" |
| **REVISE** | Modify proposals based on feedback | "How to suggest changes without offense in Japan?" |
| **AGREE** | Reach consensus or accept terms | "Finalizing agreements with Brazilian counterparts" |
| **ESCALATE** | Elevate to higher authority | "When to escalate issues in Turkish negotiations?" |
| **DEFER** | Postpone decisions strategically | "Requesting more time in Korean business culture" |

---

## 🛠️ Development

### Adding New Intelligence Actors

1. Create actor class in `actors/intelligence/`
2. Define message protocol
3. Register in `IntelligenceSupervisorActor`
4. Update routing in `DiplomaticSessionActor`

### Extending Cultural Knowledge

The system uses LLM-powered cultural intelligence, so expanding coverage requires:
1. Update country detection in `ScenarioClassifierActor`
2. Add cultural keywords if needed
3. No hard-coded cultural data required

### Custom LLM Provider

Implement in `LLMProcessorActor`:
```java
private String callCustomLLMAPI(String prompt) throws Exception {
    // Your implementation
}
```

---

## 🐛 Troubleshooting

### Application won't start
```bash
# Check Java version
java -version  # Should be 17+

# Clean and rebuild
mvn clean compile
```

### "Cannot find symbol" errors
```bash
# Clean Maven cache
mvn clean install -U
```

### API Key not recognized
```bash
# Verify environment variable
echo $LLM_API_KEY

# Set in current session
export LLM_API_KEY="your-key-here"
```

### Actor system errors
- Check `application.conf` syntax
- Review logs in console output
- Ensure all dependencies are present

### No LLM responses
- Verify API key is valid
- Check network connectivity
- Review LLMProcessorActor logs
- Ensure API provider matches key type

---

## 📊 Performance Characteristics

- **Concurrent Sessions**: Up to 100 active sessions
- **Response Time**: 2-5 seconds typical (depends on LLM API)
- **Actor Throughput**: 16 concurrent operations
- **Fault Tolerance**: Auto-restart on failure (3 attempts)

---

## 🔐 Security Considerations

- **API Keys**: Never commit to version control
- **Environment Variables**: Use `.env` files or system environment
- **Session Data**: Stored in-memory only (not persistent)
- **Logging**: Be cautious about logging sensitive diplomatic content

---

## 📚 Academic Context

This project implements concepts from:
- **IDEA Framework**: Computational diplomacy primitives
- **Actor Model**: Concurrent, distributed systems (Carl Hewitt, 1973)
- **Enterprise Architecture**: TOGAF, Zachman Framework integration
- **Cross-Cultural Communication**: Hofstede dimensions, cultural intelligence

### Related Research
- Computational models of negotiation
- Task-oriented dialogue systems
- Cross-cultural communication patterns
- Enterprise architecture frameworks

---

## 🤝 Contributing

This is an academic research project. For questions or collaboration:
- Review the IDEA Framework documentation
- Check existing issues and message protocols
- Follow the actor model patterns established
- Maintain separation between infrastructure and intelligence layers

---

## 📄 License

[Your License Here]

---

## 👤 Author

**Yasmin**  
MS in Software Engineering Systems  
Northeastern University  
Systems Engineer at General Dynamics Mission Systems

**Advisor**: Professor Kal Bugrara

---

## 🎯 Project Status

✅ **Complete and Operational**
- Infrastructure Layer (Part A): Complete
- Intelligence Layer (Part B): Complete
- LLM Integration: Claude & OpenAI supported
- CLI Interface: Fully functional
- Message Protocol: 17 message types implemented
- Actor System: Fault-tolerant and concurrent

**Future Enhancements**:
- Web-based user interface
- REST API for programmatic access
- Conversation analytics dashboard
- Multi-language support
- Persistent conversation storage

---

**Last Updated**: December 2024  
**Version**: 1.0-SNAPSHOT