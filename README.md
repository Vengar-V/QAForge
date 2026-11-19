QAForge

QAForge is a platform for QA engineers that analyzes a project's Git history, identifies the riskiest files, and uses AI to generate test scenarios. It also provides a workspace for managing test cases and bug reports.

Features
Project import — connect a Git repository or upload a ZIP archive (up to 500 MB).
Commit analysis — parses history with JGit and detects bug-fix commits by keywords (fix, bug, hotfix, patch, etc.).
Risk Map — scores each file from 0 to 10: risk score = 60% bug density + 40% change frequency.
AI test scenario generator — generates scenarios for risky files via the Groq API (llama-3.3-70b-versatile).
QA Workspace — CRUD for test scenarios and bug reports, including bug report attachments.
Authentication — registration and login with JWT tokens.
Tech Stack

Backend

Java 21, Spring Boot 3.3.5
Spring Web, Spring Data JPA, Spring Security, Validation, WebFlux (HTTP client)
PostgreSQL + Flyway (migrations)
JJWT, Eclipse JGit, Lombok, Hypersistence Utils
Maven

Frontend

React + React Router (frontend/src)
HTML screen prototypes (frontend/*.html): login, dashboard, import, risk map, AI generator, QA workspace
Project Structure
QAForge/
├── pom.xml
├── src/main/java/com/qaforge/
│   ├── controller/     # REST controllers
│   ├── service/        # Business logic (Git analysis, AI, auth, projects)
│   ├── repository/     # Spring Data JPA repositories
│   ├── entity/         # JPA entities
│   ├── dto/            # Request/response DTOs
│   └── security/       # JWT, Spring Security config
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/   # Flyway migrations V1–V4
└── frontend/
    ├── src/            # React app
    └── *.html          # UI prototypes
Getting Started
Prerequisites
JDK 21
Maven 3.9+
PostgreSQL 14+
Node.js 18+ (for the frontend)
1. Database
sql
CREATE DATABASE qaforge;

Tables are created automatically by Flyway on first startup.

2. Configuration

Edit src/main/resources/application.yml (or use environment variables):

Property	Description
spring.datasource.url / username / password	PostgreSQL connection
jwt.secret	JWT signing secret (≥ 256 bits, change it in production)
GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET	GitHub OAuth (optional)

The Groq API key for the AI generator is sent from the frontend with each request and is not stored on the server.

3. Run the backend
bash
mvn spring-boot:run

The API will be available at http://localhost:8080/api.

4. Run the frontend
bash
cd frontend
npm install
npm run dev
REST API

Base path: /api. All endpoints except /auth/** require the Authorization: Bearer <token> header.

Method	Path	Description
POST	/auth/register	Register
POST	/auth/login	Log in, returns JWT
GET	/projects	List projects
GET	/projects/{id}	Project details
POST	/projects/import	Import a project
DELETE	/projects/{id}	Delete a project
POST	/analysis/{projectId}/analyse	Run Git history analysis
GET	/analysis/{projectId}/commits	Commit history
GET	/analysis/{projectId}/risks	File risks
GET	/ai/ping	AI module health check
POST	/ai/{projectId}/generate	Generate test scenarios
GET	/ai/{projectId}/scenarios	Generated scenarios
GET / POST	/workspace/{projectId}/scenarios	Test scenarios
PUT / DELETE	/workspace/{projectId}/scenarios/{scenarioId}	Update / delete a scenario
GET / POST	/workspace/{projectId}/bugs	Bug reports
PUT / DELETE	/workspace/{projectId}/bugs/{bugId}	Update / delete a bug report
Database

Main tables: users, projects, commit_history, file_risks, test_scenarios, bug_reports.

Author

Vengar-V
