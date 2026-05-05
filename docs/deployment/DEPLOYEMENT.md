```python?code_reference&code_event_index=1
import os

# Create docs/deployment directory if it doesn't exist
os.makedirs('docs/deployment', exist_ok=True)

md_content = """# Deployment Guide - MindBridge

This document outlines the steps to deploy the MindBridge Digital Therapy Assistant full-stack application on an AWS EC2 instance using Docker and Nginx.

## 1. Prerequisites
* AWS EC2 Instance (t3.medium or larger recommended)
* Docker and Docker Compose installed on the instance
* Anthropic API Key for AI features
* JWT Secret for authentication

## 2. Environment Variables
Create a `.env` file in the root directory with the following keys:
```env
ANTHROPIC_API_KEY=your_api_key_here
JWT_SECRET=your_secure_random_secret
```

## 3. Infrastructure Components
* **Nginx (Port 80)**: Reverse proxy for routing traffic.
* **Frontend (Port 3000)**: Next.js web application.
* **Backend (Port 8080)**: Spring Boot REST API and MCP Server.

## 4. Launch Instructions
1. Clone the repository to the EC2 instance.
2. Ensure the `.env` file is populated.
3. Run the following command from the root directory:
   ```bash
   docker-compose up -d --build
   ```

## 5. Verification
* **Frontend**: Access via `http://<ec2-public-ip>/`
* **API Health**: `http://<ec2-public-ip>/api/actuator/health` (Should return UP status)
* **Swagger UI**: `http://<ec2-public-ip>/swagger-ui/index.html`
* **MCP Server**: `http://<ec2-public-ip>/api/mcp/tools`

## 6. Troubleshooting
* Check container logs: `docker-compose logs -f`
* Verify security groups: Ensure port 80 is open to inbound traffic.
* Database: H2 is persisted in the `./data` volume.
"""

with open('docs/deployment/DEPLOYMENT.md', 'w') as f:
    f.write(md_content)


```
Your deployment guide is ready in the required folder.

[file-tag: code-generated-file-0-1778020540787752410]

The file is located at `docs/deployment/DEPLOYMENT.md`. It includes instructions for setting up your environment variables, launching the containers with Docker Compose, and verifying the health of the system. You can now use this to complete your ownership of the infrastructure tasks for Assignment 3.