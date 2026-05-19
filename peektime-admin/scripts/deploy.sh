#!/bin/bash
set -e

UPSTREAM_CONF="nginx/upstream.conf"
ECR_REGISTRY="${ECR_REGISTRY}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

get_current() {
    if grep -q "admin-blue" "$UPSTREAM_CONF" 2>/dev/null; then
        echo "blue"
    else
        echo "green"
    fi
}

wait_for_health() {
    local container=$1
    local port=$2
    local max_attempts=40
    local attempt=0

    echo "⏳ $container 헬스체크 대기... (최대 120초)"
    while [ $attempt -lt $max_attempts ]; do
        # actuator가 없으면 루트 경로로 대체 (302 redirect도 성공으로 처리)
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "http://localhost:$port/" || echo "000")
        if [[ "$HTTP_CODE" =~ ^(200|302|301)$ ]]; then
            echo "$container 정상 (HTTP $HTTP_CODE)"
            return 0
        fi
        attempt=$((attempt + 1))
        echo "  시도 $attempt/$max_attempts... (HTTP $HTTP_CODE)"
        sleep 3
    done
    echo "$container 헬스체크 실패"
    return 1
}

ensure_nginx() {
    if ! docker compose ps nginx --status running 2>/dev/null | grep -q "nginx"; then
        echo " nginx 컨테이너 없음. 시작합니다..."
        docker compose up -d nginx
        sleep 3
    fi
}

main() {
    CURRENT=$(get_current)
    if [ "$CURRENT" = "blue" ]; then
        TARGET="green"
        TARGET_PORT="9083"
    else
        TARGET="blue"
        TARGET_PORT="9082"
    fi

    echo "========================================"
    echo "  admin 배포 시작"
    echo "  현재: $CURRENT"
    echo "  타겟: $TARGET ($TARGET_PORT)"
    echo "  이미지: $ECR_REGISTRY/peektime-admin:$IMAGE_TAG"
    echo "========================================"

    echo "이미지 Pull 중"
    docker compose pull admin-$TARGET

    echo "$TARGET 컨테이너 시작..."
    docker compose up -d admin-$TARGET

    echo "컨테이너 준비 대기 중... (최대 30초)"
    for i in {1..6}; do
        sleep 5
        if docker ps --filter "name=peektime-admin-$TARGET" --filter "status=running" | grep -q "peektime-admin-$TARGET"; then
            echo "컨테이너 running 확인"
            break
        fi
        echo "  대기 중... ($((i*5))초)"
        if [ $i -eq 6 ]; then
            echo "컨테이너 시작 실패"
            docker logs peektime-admin-$TARGET 2>&1 || true
            exit 1
        fi
    done

    if ! wait_for_health "admin-$TARGET" "$TARGET_PORT"; then
        echo "배포 실패, 롤백"
        docker logs peektime-admin-$TARGET --tail 50 2>&1 || true
        docker compose stop admin-$TARGET
        exit 1
    fi

    echo "트래픽 전환: $CURRENT → $TARGET"
    cat > "$UPSTREAM_CONF" << EOF
upstream peektime_admin {
    server admin-$TARGET:8081;
}
EOF

    ensure_nginx
    docker compose exec -T nginx nginx -s reload
    echo " Nginx 리로드 완료"

    sleep 5
    docker compose stop admin-$CURRENT

    echo "========================================"
    echo " 배포 완료 (활성: $TARGET)"
    echo "========================================"
}

main "$@"