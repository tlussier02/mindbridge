# Digital Therapy Assistant - C4 Architecture Diagrams

This directory contains the C4 model architecture diagrams for the Digital Therapy Assistant application, written in PlantUML format. The diagrams follow the [C4 model](https://c4model.com/) methodology, progressively zooming in from system context down to code-level detail.

## Diagrams

### Level 1: System Context (`c4-context.puml`)

The highest-level view showing the Digital Therapy Assistant system in relation to its users (Patient, Therapist, System Administrator) and external systems (LLM Provider, Email Service, Healthcare EHR Systems). This diagram answers the question: "What is the system and who/what interacts with it?"

### Level 2: Container (`c4-container.puml`)

Zooms into the Digital Therapy Assistant system boundary to show the major containers: the CLI Application, Spring Boot Application, H2 Database, SimpleVector Store, and Knowledge Base. This diagram shows how these containers communicate and which technologies they use.

### Level 3: Component (`c4-component.puml`)

Zooms into the Spring Boot Application container to show its internal components: REST controllers (Auth, Session, Diary, Progress, Crisis), service layer (AuthService, SessionService, DiaryService, ProgressService, CrisisService, AiService), data repositories, and infrastructure components (RagContextBuilder, LlmClient, CrisisDetector, JWT security). This diagram shows the component-level architecture and their dependencies.

### Level 4: Code (`c4-code.puml`)

Contains three diagrams that zoom into the code level:

- **Class Diagram (AI Service Module):** Shows the class structure of the AI subsystem including the `AiService` interface, `LlmClient`, `RagContextBuilder`, `SimpleVectorStore`, `CrisisDetector`, `EmbeddingService`, and `KnowledgeBaseLoader` with their fields, methods, and relationships.

- **Sequence Diagram (Chat Message Flow):** Traces a user chat message from the CLI through the SessionController, SessionService, AiService (with RAG context building and crisis detection), and back to the user with the AI-generated therapeutic response.

- **Sequence Diagram (Diary Entry with AI Analysis):** Traces the creation of a thought diary entry, including the AI-powered cognitive distortion suggestion step and the subsequent entry persistence.

## Rendering the Diagrams

These `.puml` files can be rendered using:

- **PlantUML Online Server:** Paste the contents at [https://www.plantuml.com/plantuml/uml](https://www.plantuml.com/plantuml/uml)
- **VS Code:** Install the "PlantUML" extension by jebbs
- **IntelliJ IDEA:** Install the "PlantUML Integration" plugin
- **Command Line:** `java -jar plantuml.jar c4-context.puml`

Note: The C4 diagrams require internet access on first render to download the C4-PlantUML library includes from GitHub.
