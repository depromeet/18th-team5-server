#!/bin/bash
set -e

UPSTREAM_CONF="nginx/upstream.conf"
ECR_REGISTRY="${ECR_REGISTRY}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

get_current() {
    if grep -q "api-blue" "$UPSTREAM_CONF" 2>/dev/null; then
        echo "blue"
    else
        echo "green"
    fi
}

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

ensure_nginx() {
    if ! docker compose ps nginx --status running 2>/dev/null | grep -q "nginx"; then
        echo "🔧 nginx 컨테이너가 없습니다. 시작합니다..."
        docker compose up -d nginx
        sleep 3
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
    echo "🚀 배포 시작"
    echo "  현재: $CURRENT"
    echo "  타겟: $TARGET"
    echo "  이미지: $ECR_REGISTRY/peaktime:$IMAGE_TAG"
    echo "========================================"

    echo ""
    echo "📦 이미지 Pull 중..."
    docker compose pull api-$TARGET

    echo ""
    echo "🔄 $TARGET 컨테이너 시작..."
    docker compose up -d api-$TARGET

    echo ""
    if ! wait_for_health "api-$TARGET" "$TARGET_PORT"; then
        echo "❌ 배포 실패, 롤백"
        docker compose stop api-$TARGET
        exit 1
    fi

    echo ""
    echo "🔀 트래픽 전환: $CURRENT → $TARGET"
    cat > "$UPSTREAM_CONF" << EOF
upstream peektime_api {
    server api-$TARGET:8080;
}
EOF

    ensure_nginx
    docker compose exec -T nginx nginx -s reload
    echo "✅ Nginx 리로드 완료"

    echo ""
    echo "🧹 이전 컨테이너 정리: $CURRENT"
    sleep 5
    docker compose stop api-$CURRENT

    echo ""
    echo "========================================"
    echo "✅ 배포 완료"
    echo "  활성: $TARGET"
    echo "  이미지: $IMAGE_TAG"
    echo "========================================"
}

main "$@"