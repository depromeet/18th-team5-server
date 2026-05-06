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

    if [ -n "$PREVIOUS_TAG" ]; then
        export IMAGE_TAG="$PREVIOUS_TAG"
        docker compose pull api-$TARGET
    fi

    docker compose up -d api-$TARGET

    echo "⏳ 헬스체크 대기..."
    sleep 15

    cat > "$UPSTREAM_CONF" << EOF
upstream peektime_api {
    server api-$TARGET:8080;
}
EOF

    docker compose exec -T nginx nginx -s reload

    sleep 5
    docker compose stop api-$CURRENT

    echo ""
    echo "========================================"
    echo "✅ 롤백 완료: $TARGET 활성"
    echo "========================================"
}

main "$@"