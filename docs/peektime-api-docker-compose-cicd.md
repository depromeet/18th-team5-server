# PeekTime API - Docker Compose 기반 CI/CD 마이그레이션

## 현재 구조

```
GitHub Actions (zero-downtime-deploy.yml)
    ↓ SSH
EC2 서버
    ├── /app/deploy.sh      ← 레포에 없음 (EC2에만 존재)
    ├── /app/switch.sh      ← 레포에 없음 (EC2에만 존재)
    └── /var/run/peaktime-current.env  ← blue/green 상태 저장
```

**문제점:**
- 배포 스크립트가 EC2에만 존재 → 버전 관리 안 됨
- 설정이 분산되어 있음
- 로컬에서 동일 환경 재현 어려움

---

## 목표 구조

```
GitHub Actions
    ↓ SSH
EC2 서버
    └── /app/peektime-api/
        ├── docker-compose.yml      ← 레포에서 관리
        ├── docker-compose.prod.yml ← 레포에서 관리
        ├── nginx/
        │   └── nginx.conf          ← 레포에서 관리
        ├── scripts/
        │   ├── deploy.sh           ← 레포에서 관리
        │   └── healthcheck.sh      ← 레포에서 관리
        └── .env                    ← EC2에서 관리 (secrets)
```

---

## Phase 1: Docker Compose 파일 작성

### 1.1 기본 docker-compose.yml

```yaml
# docker-compose.yml
services:
  api-blue:
    build:
      context: ./peektime-api
      dockerfile: Dockerfile
    container_name: peektime-api-blue
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    env_file:
      - .env
    ports:
      - "8081:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 40s
    restart: unless-stopped

  api-green:
    build:
      context: ./peektime-api
      dockerfile: Dockerfile
    container_name: peektime-api-green
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    env_file:
      - .env
    ports:
      - "8082:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 40s
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    container_name: peektime-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/upstream.conf:/etc/nginx/conf.d/upstream.conf:ro
    depends_on:
      - api-blue
      - api-green
    restart: unless-stopped
```

### 1.2 nginx/nginx.conf

```nginx
events {
    worker_connections 1024;
}

http {
    include /etc/nginx/conf.d/upstream.conf;

    server {
        listen 80;
        server_name _;

        location /health {
            return 200 'OK';
            add_header Content-Type text/plain;
        }

        location / {
            proxy_pass http://peektime_api;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;

            proxy_connect_timeout 5s;
            proxy_read_timeout 60s;
        }
    }
}
```

### 1.3 nginx/upstream.conf (동적으로 교체되는 파일)

```nginx
# Blue가 활성 상태일 때
upstream peektime_api {
    server api-blue:8080;
}
```

---

## Phase 2: 배포 스크립트 작성

### 2.1 scripts/deploy.sh

```bash
#!/bin/bash
set -e

COMPOSE_FILE="docker-compose.yml"
UPSTREAM_CONF="nginx/upstream.conf"

# 현재 활성 컨테이너 확인
get_current() {
    if grep -q "api-blue" "$UPSTREAM_CONF" 2>/dev/null; then
        echo "blue"
    else
        echo "green"
    fi
}

# 헬스체크
wait_for_health() {
    local container=$1
    local port=$2
    local max_attempts=30
    local attempt=0

    echo "⏳ $container 헬스체크 대기..."

    while [ $attempt -lt $max_attempts ]; do
        if curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo "✅ $container 정상"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 2
    done

    echo "❌ $container 헬스체크 실패"
    return 1
}

# 메인 배포 로직
main() {
    CURRENT=$(get_current)

    if [ "$CURRENT" = "blue" ]; then
        TARGET="green"
        TARGET_PORT="8082"
    else
        TARGET="blue"
        TARGET_PORT="8081"
    fi

    echo "🚀 배포 시작: $CURRENT → $TARGET"

    # 1. 새 컨테이너 빌드 & 시작
    echo "📦 $TARGET 컨테이너 빌드 중..."
    docker compose build api-$TARGET
    docker compose up -d api-$TARGET

    # 2. 헬스체크 대기
    if ! wait_for_health "api-$TARGET" "$TARGET_PORT"; then
        echo "❌ 배포 실패, 롤백"
        docker compose stop api-$TARGET
        exit 1
    fi

    # 3. Nginx upstream 전환
    echo "🔄 트래픽 전환: $TARGET"
    cat > "$UPSTREAM_CONF" << EOF
upstream peektime_api {
    server api-$TARGET:8080;
}
EOF

    # 4. Nginx reload (무중단)
    docker compose exec -T nginx nginx -s reload

    # 5. 이전 컨테이너 정리 (선택적)
    echo "🧹 이전 컨테이너 정리: $CURRENT"
    sleep 5  # graceful shutdown 대기
    docker compose stop api-$CURRENT

    echo "✅ 배포 완료: $TARGET 활성"
}

main "$@"
```

### 2.2 scripts/rollback.sh

```bash
#!/bin/bash
set -e

UPSTREAM_CONF="nginx/upstream.conf"

get_current() {
    if grep -q "api-blue" "$UPSTREAM_CONF" 2>/dev/null; then
        echo "blue"
    else
        echo "green"
    fi
}

main() {
    CURRENT=$(get_current)

    if [ "$CURRENT" = "blue" ]; then
        TARGET="green"
    else
        TARGET="blue"
    fi

    echo "⚠️ 롤백: $CURRENT → $TARGET"

    # 이전 컨테이너 시작
    docker compose up -d api-$TARGET

    # 헬스체크
    sleep 10

    # Nginx 전환
    cat > "$UPSTREAM_CONF" << EOF
upstream peektime_api {
    server api-$TARGET:8080;
}
EOF

    docker compose exec -T nginx nginx -s reload

    echo "✅ 롤백 완료: $TARGET 활성"
}

main "$@"
```

---

## Phase 3: GitHub Actions 워크플로우

### 3.1 .github/workflows/deploy-api.yml

```yaml
name: Deploy PeekTime API

on:
  push:
    branches: [main]
    paths:
      - 'peektime-api/**'
      - 'docker-compose.yml'
      - 'nginx/**'
      - 'scripts/**'
  workflow_dispatch:

env:
  EC2_HOST: ${{ secrets.EC2_HOST }}
  EC2_USER: ${{ secrets.EC2_USER }}
  EC2_KEY: ${{ secrets.EC2_KEY }}
  APP_DIR: /app/peektime-api

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Copy files to EC2
        uses: appleboy/scp-action@master
        with:
          host: ${{ env.EC2_HOST }}
          username: ${{ env.EC2_USER }}
          key: ${{ env.EC2_KEY }}
          source: "docker-compose.yml,peektime-api,nginx,scripts"
          target: ${{ env.APP_DIR }}

      - name: Deploy
        uses: appleboy/ssh-action@master
        with:
          host: ${{ env.EC2_HOST }}
          username: ${{ env.EC2_USER }}
          key: ${{ env.EC2_KEY }}
          script: |
            cd ${{ env.APP_DIR }}
            chmod +x scripts/*.sh
            ./scripts/deploy.sh
```

---

## Phase 4: EC2 초기 설정

### 4.1 사전 요구사항

```bash
# Docker 설치
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 4.2 앱 디렉토리 생성 및 환경변수 설정

```bash
sudo mkdir -p /app/peektime-api
sudo chown ec2-user:ec2-user /app/peektime-api

# .env 파일 생성 (secrets)
cat > /app/peektime-api/.env << 'EOF'
DB_URL=jdbc:mysql://your-rds-endpoint:3306/peektime
DB_USERNAME=admin
DB_PASSWORD=your-password
AWS_ACCESS_KEY=xxx
AWS_SECRET_KEY=xxx
AWS_S3_BUCKET=your-bucket
EOF

chmod 600 /app/peektime-api/.env
```

### 4.3 초기 nginx upstream 설정

```bash
mkdir -p /app/peektime-api/nginx
cat > /app/peektime-api/nginx/upstream.conf << 'EOF'
upstream peektime_api {
    server api-blue:8080;
}
EOF
```

---

## Phase 5: 마이그레이션 단계

### Step 1: 파일 준비 (로컬)
- [ ] docker-compose.yml 작성
- [ ] nginx/nginx.conf 작성
- [ ] nginx/upstream.conf 작성
- [ ] scripts/deploy.sh 작성
- [ ] scripts/rollback.sh 작성
- [ ] peektime-api/Dockerfile 수정 (healthcheck용 curl 추가)

### Step 2: EC2 준비
- [ ] Docker & Docker Compose 설치 확인
- [ ] /app/peektime-api 디렉토리 생성
- [ ] .env 파일 생성

### Step 3: 수동 테스트
```bash
# EC2에서 직접 실행
cd /app/peektime-api
docker compose up -d api-blue nginx
# 동작 확인 후
./scripts/deploy.sh  # green으로 전환 테스트
```

### Step 4: GitHub Actions 전환
- [ ] 새 워크플로우 파일 추가
- [ ] 기존 워크플로우 비활성화 또는 삭제

### Step 5: 정리
- [ ] EC2의 기존 /app/deploy.sh, switch.sh 백업 후 삭제
- [ ] 기존 컨테이너 정리

---

## 파일 구조 (최종)

```
peektime-server/
├── docker-compose.yml
├── nginx/
│   ├── nginx.conf
│   └── upstream.conf
├── scripts/
│   ├── deploy.sh
│   └── rollback.sh
├── peektime-api/
│   ├── Dockerfile
│   └── src/...
└── .github/workflows/
    └── deploy-api.yml
```

---

## Actuator 의존성 추가 필요

헬스체크를 위해 `peektime-api/build.gradle`에 추가:

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

`application-prod.yml`에 추가:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
```

---

## 롤백 방법

```bash
# EC2에서 직접 실행
cd /app/peektime-api
./scripts/rollback.sh
```

또는 GitHub Actions에서 이전 커밋으로 재배포.