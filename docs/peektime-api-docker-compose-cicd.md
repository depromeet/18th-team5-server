# PeekTime API - Docker Compose + ECR 기반 CI/CD 마이그레이션

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
- EC2에서 빌드 → 느림, 리소스 낭비
- 설정이 분산되어 있음
- 롤백 어려움

---

## 목표 구조

```
GitHub Actions
    ↓ Build & Push
ECR (이미지 저장소)
    ↓ Pull
EC2 서버
    └── /app/peektime-api/
        ├── docker-compose.yml      ← 레포에서 관리
        ├── nginx/
        │   ├── nginx.conf          ← 레포에서 관리
        │   └── upstream.conf       ← 동적 교체
        ├── scripts/
        │   ├── deploy.sh           ← 레포에서 관리
        │   └── rollback.sh         ← 레포에서 관리
        └── .env                    ← EC2에서 관리 (secrets)
```

**장점:**
- GitHub Actions에서 빌드 → EC2 리소스 절약
- 이미지 태그로 버전 관리 → 롤백 용이
- 동일 이미지로 여러 환경 배포 가능

---

## Phase 1: ECR 설정

### 1.1 ECR 리포지토리 생성

```bash
# AWS CLI로 생성
aws ecr create-repository \
    --repository-name peektime/api \
    --region ap-northeast-2

# 결과 예시
# URI: 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/peektime/api
```

### 1.2 GitHub Secrets 설정

| Secret 이름 | 값 |
|------------|-----|
| `AWS_ACCOUNT_ID` | `123456789012` |
| `AWS_ACCESS_KEY_ID` | IAM Access Key |
| `AWS_SECRET_ACCESS_KEY` | IAM Secret Key |
| `AWS_REGION` | `ap-northeast-2` |
| `EC2_HOST` | EC2 퍼블릭 IP |
| `EC2_USER` | `ec2-user` |
| `EC2_KEY` | SSH Private Key |

### 1.3 IAM 권한 (GitHub Actions용)

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ecr:GetAuthorizationToken",
                "ecr:BatchCheckLayerAvailability",
                "ecr:GetDownloadUrlForLayer",
                "ecr:BatchGetImage",
                "ecr:PutImage",
                "ecr:InitiateLayerUpload",
                "ecr:UploadLayerPart",
                "ecr:CompleteLayerUpload"
            ],
            "Resource": "*"
        }
    ]
}
```

---

## Phase 2: Docker Compose 파일 작성

### 2.1 docker-compose.yml

```yaml
services:
  api-blue:
    image: ${ECR_REGISTRY}/peektime/api:${IMAGE_TAG:-latest}
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
    networks:
      - peektime-network

  api-green:
    image: ${ECR_REGISTRY}/peektime/api:${IMAGE_TAG:-latest}
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
    networks:
      - peektime-network

  nginx:
    image: nginx:alpine
    container_name: peektime-nginx
    ports:
      - "80:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/upstream.conf:/etc/nginx/conf.d/upstream.conf:ro
    networks:
      - peektime-network
    restart: unless-stopped

networks:
  peektime-network:
    driver: bridge
```

### 2.2 nginx/nginx.conf

```nginx
events {
    worker_connections 1024;
}

http {
    include /etc/nginx/conf.d/upstream.conf;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent"';

    access_log /var/log/nginx/access.log main;

    server {
        listen 80;
        server_name _;

        location /health {
            return 200 'OK';
            add_header Content-Type text/plain;
        }

        location / {
            proxy_pass http://peektime_api;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_set_header Connection "";

            proxy_connect_timeout 5s;
            proxy_read_timeout 60s;
            proxy_send_timeout 60s;
        }
    }
}
```

### 2.3 nginx/upstream.conf

```nginx
upstream peektime_api {
    server api-blue:8080;
}
```

---

## Phase 3: 배포 스크립트

### 3.1 scripts/deploy.sh

```bash
#!/bin/bash
set -e

# 설정
UPSTREAM_CONF="nginx/upstream.conf"
ECR_REGISTRY="${ECR_REGISTRY}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

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
        echo "  시도 $attempt/$max_attempts..."
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

    echo "========================================"
    echo "🚀 배포 시작"
    echo "  현재: $CURRENT"
    echo "  타겟: $TARGET"
    echo "  이미지: $ECR_REGISTRY/peektime/api:$IMAGE_TAG"
    echo "========================================"

    # 1. 새 이미지 pull
    echo ""
    echo "📦 이미지 Pull 중..."
    docker compose pull api-$TARGET

    # 2. 새 컨테이너 시작
    echo ""
    echo "🔄 $TARGET 컨테이너 시작..."
    docker compose up -d api-$TARGET

    # 3. 헬스체크 대기
    echo ""
    if ! wait_for_health "api-$TARGET" "$TARGET_PORT"; then
        echo "❌ 배포 실패, 롤백"
        docker compose stop api-$TARGET
        exit 1
    fi

    # 4. Nginx upstream 전환
    echo ""
    echo "🔀 트래픽 전환: $CURRENT → $TARGET"
    cat > "$UPSTREAM_CONF" << EOF
upstream peektime_api {
    server api-$TARGET:8080;
}
EOF

    # 5. Nginx reload (무중단)
    docker compose exec -T nginx nginx -s reload
    echo "✅ Nginx 리로드 완료"

    # 6. 이전 컨테이너 정리
    echo ""
    echo "🧹 이전 컨테이너 정리: $CURRENT"
    sleep 5  # graceful shutdown 대기
    docker compose stop api-$CURRENT

    echo ""
    echo "========================================"
    echo "✅ 배포 완료"
    echo "  활성: $TARGET"
    echo "  이미지: $IMAGE_TAG"
    echo "========================================"
}

main "$@"
```

### 3.2 scripts/rollback.sh

```bash
#!/bin/bash
set -e

UPSTREAM_CONF="nginx/upstream.conf"
PREVIOUS_TAG="${1:-}"

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
        TARGET_PORT="8082"
    else
        TARGET="blue"
        TARGET_PORT="8081"
    fi

    echo "========================================"
    echo "⚠️  롤백 시작"
    echo "  현재: $CURRENT"
    echo "  타겟: $TARGET"
    if [ -n "$PREVIOUS_TAG" ]; then
        echo "  이미지 태그: $PREVIOUS_TAG"
    fi
    echo "========================================"

    # 이전 태그가 지정된 경우 해당 이미지로 pull
    if [ -n "$PREVIOUS_TAG" ]; then
        export IMAGE_TAG="$PREVIOUS_TAG"
        docker compose pull api-$TARGET
    fi

    # 이전 컨테이너 시작
    docker compose up -d api-$TARGET

    # 헬스체크
    echo "⏳ 헬스체크 대기..."
    sleep 15

    # Nginx 전환
    cat > "$UPSTREAM_CONF" << EOF
upstream peektime_api {
    server api-$TARGET:8080;
}
EOF

    docker compose exec -T nginx nginx -s reload

    # 현재 컨테이너 정리
    sleep 5
    docker compose stop api-$CURRENT

    echo ""
    echo "========================================"
    echo "✅ 롤백 완료: $TARGET 활성"
    echo "========================================"
}

main "$@"
```

---

## Phase 4: GitHub Actions 워크플로우

### 4.1 .github/workflows/deploy-api.yml

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
    inputs:
      image_tag:
        description: '배포할 이미지 태그 (기본: git SHA)'
        required: false

env:
  AWS_REGION: ap-northeast-2
  ECR_REPOSITORY: peektime/api
  APP_DIR: /app/peektime-api

jobs:
  build-and-push:
    name: Build & Push to ECR
    runs-on: ubuntu-latest
    outputs:
      image_tag: ${{ steps.meta.outputs.tags }}

    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Set image tag
        id: meta
        run: |
          if [ -n "${{ inputs.image_tag }}" ]; then
            TAG="${{ inputs.image_tag }}"
          else
            TAG="${{ github.sha }}"
          fi
          echo "tags=$TAG" >> $GITHUB_OUTPUT

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: ./peektime-api
          push: true
          tags: |
            ${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:${{ steps.meta.outputs.tags }}
            ${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:latest
          cache-from: type=registry,ref=${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:latest
          cache-to: type=inline

  deploy:
    name: Deploy to EC2
    needs: build-and-push
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Copy files to EC2
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_KEY }}
          source: "docker-compose.yml,nginx,scripts"
          target: ${{ env.APP_DIR }}
          overwrite: true

      - name: Deploy
        uses: appleboy/ssh-action@v1.0.3
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ needs.build-and-push.outputs.image_tag }}
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_KEY }}
          envs: ECR_REGISTRY,IMAGE_TAG
          script: |
            # ECR 로그인
            aws ecr get-login-password --region ${{ env.AWS_REGION }} | \
              docker login --username AWS --password-stdin $ECR_REGISTRY

            # 배포 실행
            cd ${{ env.APP_DIR }}
            chmod +x scripts/*.sh
            export ECR_REGISTRY=$ECR_REGISTRY
            export IMAGE_TAG=$IMAGE_TAG
            ./scripts/deploy.sh
```

### 4.2 .github/workflows/rollback-api.yml (수동 롤백용)

```yaml
name: Rollback PeekTime API

on:
  workflow_dispatch:
    inputs:
      image_tag:
        description: '롤백할 이미지 태그'
        required: true

env:
  AWS_REGION: ap-northeast-2
  APP_DIR: /app/peektime-api

jobs:
  rollback:
    name: Rollback to previous version
    runs-on: ubuntu-latest

    steps:
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Rollback
        uses: appleboy/ssh-action@v1.0.3
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ inputs.image_tag }}
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_KEY }}
          envs: ECR_REGISTRY,IMAGE_TAG
          script: |
            aws ecr get-login-password --region ${{ env.AWS_REGION }} | \
              docker login --username AWS --password-stdin $ECR_REGISTRY

            cd ${{ env.APP_DIR }}
            export ECR_REGISTRY=$ECR_REGISTRY
            ./scripts/rollback.sh $IMAGE_TAG
```

---

## Phase 5: Dockerfile 수정

### 5.1 peektime-api/Dockerfile

```dockerfile
# Build stage
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Gradle 파일 먼저 복사 (캐시 활용)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 다운로드 (캐시 레이어)
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre

WORKDIR /app

# curl 설치 (헬스체크용)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 빌드된 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Phase 6: EC2 초기 설정

### 6.1 Docker & AWS CLI 설치

```bash
# Docker 설치
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Docker Compose 설치 (v2)
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
    -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# AWS CLI (ECR 로그인용) - Amazon Linux 2023에는 기본 설치됨
aws --version
```

### 6.2 앱 디렉토리 및 환경변수

```bash
# 디렉토리 생성
sudo mkdir -p /app/peektime-api/nginx
sudo chown -R ec2-user:ec2-user /app/peektime-api

# .env 파일 생성
cat > /app/peektime-api/.env << 'EOF'
DB_URL=jdbc:mysql://your-rds-endpoint:3306/peektime
DB_USERNAME=admin
DB_PASSWORD=your-password
AWS_ACCESS_KEY=xxx
AWS_SECRET_KEY=xxx
AWS_S3_BUCKET=your-bucket
EOF

chmod 600 /app/peektime-api/.env

# 초기 upstream.conf
cat > /app/peektime-api/nginx/upstream.conf << 'EOF'
upstream peektime_api {
    server api-blue:8080;
}
EOF
```

### 6.3 EC2 IAM Role 설정 (권장)

EC2에 IAM Role을 할당하면 AWS credentials 설정 없이 ECR 접근 가능:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ecr:GetAuthorizationToken",
                "ecr:BatchCheckLayerAvailability",
                "ecr:GetDownloadUrlForLayer",
                "ecr:BatchGetImage"
            ],
            "Resource": "*"
        }
    ]
}
```

---

## Phase 7: 마이그레이션 체크리스트

### Step 1: AWS 준비
- [ ] ECR 리포지토리 생성
- [ ] GitHub Secrets 설정 (AWS 키, EC2 정보)
- [ ] IAM 권한 설정

### Step 2: 파일 준비 (로컬)
- [ ] docker-compose.yml 작성
- [ ] nginx/nginx.conf 작성
- [ ] scripts/deploy.sh 작성
- [ ] scripts/rollback.sh 작성
- [ ] Dockerfile 수정 (curl 추가)
- [ ] Actuator 의존성 추가

### Step 3: EC2 준비
- [ ] Docker & Docker Compose 설치
- [ ] AWS CLI 설치/확인
- [ ] /app/peektime-api 디렉토리 생성
- [ ] .env 파일 생성
- [ ] nginx/upstream.conf 초기화

### Step 4: 수동 테스트
```bash
# 로컬에서 이미지 빌드 & ECR 푸시 테스트
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin <ECR_REGISTRY>
docker build -t peektime/api:test ./peektime-api
docker tag peektime/api:test <ECR_REGISTRY>/peektime/api:test
docker push <ECR_REGISTRY>/peektime/api:test

# EC2에서 수동 배포 테스트
export ECR_REGISTRY=<ECR_REGISTRY>
export IMAGE_TAG=test
./scripts/deploy.sh
```

### Step 5: GitHub Actions 전환
- [ ] 새 워크플로우 추가 (deploy-api.yml)
- [ ] 롤백 워크플로우 추가 (rollback-api.yml)
- [ ] 기존 zero-downtime-deploy.yml 비활성화

### Step 6: 정리
- [ ] EC2 기존 스크립트 백업 & 삭제
- [ ] 기존 컨테이너 정리
- [ ] 기존 워크플로우 삭제

---

## 파일 구조 (최종)

```
peektime-server/
├── docker-compose.yml              # Blue/Green + Nginx
├── nginx/
│   ├── nginx.conf                  # Nginx 설정
│   └── upstream.conf               # 트래픽 전환용 (동적)
├── scripts/
│   ├── deploy.sh                   # 배포 스크립트
│   └── rollback.sh                 # 롤백 스크립트
├── peektime-api/
│   ├── Dockerfile                  # 수정됨 (curl 추가)
│   └── src/...
└── .github/workflows/
    ├── deploy-api.yml              # 메인 배포
    └── rollback-api.yml            # 수동 롤백
```

---

## 운영 명령어

### 배포 상태 확인
```bash
cd /app/peektime-api
docker compose ps
cat nginx/upstream.conf  # 현재 활성 컨테이너 확인
```

### 로그 확인
```bash
docker compose logs -f api-blue
docker compose logs -f api-green
docker compose logs -f nginx
```

### 수동 롤백
```bash
# 이전 버전으로 롤백
./scripts/rollback.sh <이전-이미지-태그>

# 또는 GitHub Actions에서 rollback-api.yml 워크플로우 실행
```

### ECR 이미지 목록 확인
```bash
aws ecr describe-images \
    --repository-name peektime/api \
    --query 'imageDetails[*].[imageTags,imagePushedAt]' \
    --output table
```

---

## Actuator 설정

### build.gradle
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

### application-prod.yml
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