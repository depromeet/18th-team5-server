#!/bin/bash
set -e

UPSTREAM_CONF="nginx/upstream.conf"
PREVIOUS_TAG="${1:-}"

get_current() {
    if grep -q "admin-blue" "$UPSTREAM_CONF" 2>/dev/null; then
        echo "blue"
    else
        echo "green"
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
    echo "admin 롤백 시작"
    echo "  현재: $CURRENT → 타겟: $TARGET"
    [ -n "$PREVIOUS_TAG" ] && echo "  이미지 태그: $PREVIOUS_TAG"
    echo "========================================"

    if [ -n "$PREVIOUS_TAG" ]; then
        export IMAGE_TAG="$PREVIOUS_TAG"
        docker compose pull admin-$TARGET
    fi

    docker compose up -d admin-$TARGET
    echo "기동 대기 (15초)..."
    sleep 15

    cat > "$UPSTREAM_CONF" << EOF
upstream peektime_admin {
    server admin-$TARGET:8081;
}
EOF

    docker compose exec -T nginx nginx -s reload
    sleep 5
    docker compose stop admin-$CURRENT

    echo "롤백 완료 (활성: $TARGET)"
}

main "$@"