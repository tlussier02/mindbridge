# Timmy Deployment And Testing Checklist

Use this checklist from `master`.

## 1. Pull the branch

```bash
git fetch origin
git checkout master
git pull origin master
```

## 2. Confirm required secrets

Create or update the root `.env` file:

```env
ANTHROPIC_API_KEY=your_real_anthropic_key
JWT_SECRET=your_secure_random_secret
```

Do not commit `.env`.

## 3. Local code validation

From the repo root:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -q -DskipTests compile
```

Expected result:

- backend compile succeeds on Java 21

## 4. Local Docker validation

From the repo root:

```bash
docker compose build
docker compose up -d
docker compose ps
```

Expected result:

- `backend`, `frontend`, and `nginx` are up
- no immediate restart loop

## 5. Local endpoint checks

Check the main deployment endpoints:

```bash
curl -sS http://localhost:8080/actuator/health
curl -sS -I http://localhost:8080/swagger-ui
curl -sS -I http://localhost:8080/h2-console
curl -sS -I http://localhost:3000
```

Expected result:

- actuator returns `{"status":"UP",...}`
- Swagger returns `302` to `/swagger-ui/index.html`
- H2 returns `302` to `/h2-console/`
- port `3000` returns `200`

## 6. Frontend behavior check

Open:

- `http://localhost:3000`

Verify:

- app loads
- auth screens load
- session library loads
- starting a real session does not bounce into mock history when ended
- crisis page loads without passing raw `userId` in the frontend

## 7. EC2 deployment update

On the server:

```bash
cd /home/ec2-user/mindbridge
git fetch origin
git checkout master
git pull origin master
sudo docker compose up -d --build --remove-orphans
sudo docker compose ps
```

## 8. EC2 on-box checks

Run these on EC2:

```bash
curl -sS http://localhost:8080/actuator/health
curl -sS -I http://localhost:8080/swagger-ui
curl -sS -I http://localhost:8080/h2-console
curl -sS -I http://localhost:3000
```

Expected result:

- backend health is `UP`
- Swagger and H2 redirect correctly
- nginx responds on `3000`

## 9. Public EC2 checks

From your own machine, replace `<host>` with the EC2 public DNS:

```bash
curl -sS -I http://<host>:3000
curl -sS --max-time 10 http://<host>:3000/actuator/health
curl -sS --max-time 10 -I http://<host>:3000/swagger-ui
curl -sS --max-time 10 -I http://<host>:3000/h2-console/
```

Interpretation:

- `:3000` should return `200`
- backend tools are now expected through nginx on `:3000`
- direct public `:8080` exposure is no longer required for final verification

## 10. What is already proven on this branch

These checks have already been confirmed during recovery work:

- Docker images build successfully for backend and frontend
- backend container starts successfully
- local backend container returns `UP` on actuator
- local backend container redirects Swagger and H2 correctly
- public EC2 frontend on `:3000` returns `200`

## 11. Most likely remaining failure modes

- EC2 `.env` is missing or stale
- security group is not exposing `3000`
- Docker daemon is running but old containers were not rebuilt
- teammate is on an older local branch instead of current `master`

## 12. Done criteria

Timmy's side is done when:

- branch is deployed from `master`
- local Docker stack builds and starts
- EC2 on-box backend checks pass
- public frontend on `:3000` works
- public `:8080` behavior is either confirmed working or clearly identified as a security-group-only issue
