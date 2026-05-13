<<<<<<< HEAD
# Digital Therapy Assistant - C4 Architecture Diagrams

This directory contains the C4 model architecture diagrams for the Digital Therapy Assistant application, written in PlantUML format. The diagrams follow the [C4 model](https://c4model.com/) methodology, progressively zooming in from system context down to code-level detail.
=======
# MindBridge - Assignment 3 Architecture Diagrams

This directory contains the C4 model architecture diagrams for the MindBridge application, written in PlantUML format. The diagrams follow the [C4 model](https://c4model.com/) methodology and describe the Assignment 3 system shape: browser-based frontend, Spring Boot backend, persistence, AI integration, and deployment-oriented infrastructure.
>>>>>>> 2bb2ef62b9902fd4c36412ff39432e6f45bb2bf3

## Diagrams

### Level 1: System Context (`c4-context.puml`)

<<<<<<< HEAD
The highest-level view showing the Digital Therapy Assistant system in relation to its users (Patient, Therapist, System Administrator) and external systems (LLM Provider, Email Service, Healthcare EHR Systems). This diagram answers the question: "What is the system and who/what interacts with it?"

### Level 2: Container (`c4-container.puml`)

Zooms into the Digital Therapy Assistant system boundary to show the major containers: the CLI Application, Spring Boot Application, H2 Database, SimpleVector Store, and Knowledge Base. This diagram shows how these containers communicate and which technologies they use.

### Level 3: Component (`c4-component.puml`)

Zooms into the Spring Boot Application container to show its internal components: REST controllers (Auth, Session, Diary, Progress, Crisis), service layer (AuthService, SessionService, DiaryService, ProgressService, CrisisService, AiService), data repositories, and infrastructure components (RagContextBuilder, LlmClient, CrisisDetector, JWT security). This diagram shows the component-level architecture and their dependencies.
=======
The highest-level view showing the MindBridge system in relation to its users (Patient, Therapist, System Administrator) and external systems (LLM Provider, Email Service, Healthcare EHR Systems). This diagram answers the question: "What is the system and who/what interacts with it?"

### Level 2: Container (`c4-container.puml`)

Zooms into the MindBridge system boundary to show the major containers: the web frontend, the Spring Boot backend, the H2 database, the vector store, and the knowledge base. This diagram shows how these containers communicate and which technologies they use.

### Level 3: Component (`c4-component.puml`)

Zooms into the Spring Boot Application container to show its internal components: REST controllers (Auth, Session, Diary, Progress, Crisis, MCP), service layer (AuthService, SessionService, DiaryService, ProgressService, CrisisService, AiService), data repositories, and infrastructure components (RagContextBuilder, LLM integration, CrisisDetector, JWT security). This diagram shows the component-level architecture and their dependencies.
>>>>>>> 2bb2ef62b9902fd4c36412ff39432e6f45bb2bf3

### Level 4: Code (`c4-code.puml`)

Contains three diagrams that zoom into the code level:

<<<<<<< HEAD
- **Class Diagram (AI Service Module):** Shows the class structure of the AI subsystem including the `AiService` interface, `LlmClient`, `RagContextBuilder`, `SimpleVectorStore`, `CrisisDetector`, `EmbeddingService`, and `KnowledgeBaseLoader` with their fields, methods, and relationships.

- **Sequence Diagram (Chat Message Flow):** Traces a user chat message from the CLI through the SessionController, SessionService, AiService (with RAG context building and crisis detection), and back to the user with the AI-generated therapeutic response.

- **Sequence Diagram (Diary Entry with AI Analysis):** Traces the creation of a thought diary entry, including the AI-powered cognitive distortion suggestion step and the subsequent entry persistence.
=======
- **Class Diagram (AI Service Module):** Shows the class structure of the AI subsystem including the `AiService` interface, context-building helpers, vector store integration, crisis detection, and knowledge loading relationships.

- **Sequence Diagram (Chat Message Flow):** Traces a user chat message from the web frontend through the SessionController, SessionService, AiService (with RAG context building and crisis detection), and back to the user with the AI-generated therapeutic response.

- **Sequence Diagram (Diary Entry with AI Analysis):** Traces the creation of a thought diary entry from the web frontend, including the AI-powered cognitive distortion suggestion step and the subsequent entry persistence.

## Additional Assignment 3 Notes

The current diagram set covers the core application architecture. Assignment 3 submission may also require additional deployment-facing artifacts such as:

- deployment diagram
- CI/CD pipeline diagram
- cloud-hosting relationship documentation

Those artifacts should be added alongside this directory if the team includes them in the final submission package.
>>>>>>> 2bb2ef62b9902fd4c36412ff39432e6f45bb2bf3

## Rendering the Diagrams

These `.puml` files can be rendered using:

- **PlantUML Online Server:** Paste the contents at [https://www.plantuml.com/plantuml/uml](https://www.plantuml.com/plantuml/uml)
- **VS Code:** Install the "PlantUML" extension by jebbs
- **IntelliJ IDEA:** Install the "PlantUML Integration" plugin
- **Command Line:** `java -jar plantuml.jar c4-context.puml`

Note: The C4 diagrams require internet access on first render to download the C4-PlantUML library includes from GitHub.
