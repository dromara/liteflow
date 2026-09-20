#!/usr/bin/env bash
set -euo pipefail

# All test code and infrastructure remain under liteflow-testcase-el.
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$repo_root"
mode="${1:-unit}"
maven="${MVN:-mvn}"
maven_args=(-B -ntp -DskipTests=false)
if [[ "${LITEFLOW_MAVEN_OFFLINE:-false}" == true ]]; then maven_args+=(-o); fi
modules=liteflow-testcase-el/liteflow-testcase-el-agent-core,liteflow-testcase-el/liteflow-testcase-el-agent-harness,liteflow-testcase-el/liteflow-testcase-el-agent

case "$mode" in
  unit)
    "$maven" "${maven_args[@]}" -pl "$modules" -am clean test
    "$maven" "${maven_args[@]}" \
      -pl liteflow-testcase-el/liteflow-testcase-el-springboot,liteflow-testcase-el/liteflow-testcase-el-springboot4 \
      -am -Dtest=AgentPropertyBindingTest,AgentChainGuideTest -Dsurefire.failIfNoSpecifiedTests=false test
    ;;
  integration)
    docker info >/dev/null
    redis_image="${LITEFLOW_TEST_REDIS_IMAGE:-redis:7.4-alpine}"
    mysql_image="${LITEFLOW_TEST_MYSQL_IMAGE:-mysql:8.4}"
    export LITEFLOW_SANDBOX_TEST_IMAGE="${LITEFLOW_SANDBOX_TEST_IMAGE:-liteflow-agent-sandbox:node22}"
    for image in "$redis_image" "$mysql_image" "$LITEFLOW_SANDBOX_TEST_IMAGE"; do
      docker image inspect "$image" >/dev/null
    done
    run_id="lf-guide-$$-${RANDOM}"
    mysql_name="${run_id}-mysql"
    redis_name="${run_id}-redis"
    cluster_name="${run_id}-cluster"
    containers=()
    cleanup() {
      for container in "${containers[@]-}"; do
        [[ -z "$container" ]] && continue
        docker rm -f "$container" >/dev/null 2>&1 || true
      done
    }
    trap cleanup EXIT
    trap 'exit 130' INT
    trap 'exit 143' TERM
    docker run -d --rm --name "$mysql_name" -p 127.0.0.1::3306 \
      -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -e MYSQL_DATABASE=liteflow_storage_test "$mysql_image" >/dev/null
    containers+=("$mysql_name")
    docker run -d --rm --name "$redis_name" -p 127.0.0.1::6379 "$redis_image" >/dev/null
    containers+=("$redis_name")
    docker run -d --rm --name "$cluster_name" -p 127.0.0.1::6379 "$redis_image" \
      redis-server --cluster-enabled yes --cluster-config-file nodes.conf --appendonly no >/dev/null
    containers+=("$cluster_name")
    mysql_address="$(docker port "$mysql_name" 3306/tcp)"
    redis_address="$(docker port "$redis_name" 6379/tcp)"
    cluster_address="$(docker port "$cluster_name" 6379/tcp)"
    mysql_port="${mysql_address##*:}"
    redis_port="${redis_address##*:}"
    cluster_port="${cluster_address##*:}"
    for attempt in {1..60}; do
      if docker exec "$mysql_name" mysql --protocol=TCP -h127.0.0.1 -uroot -e 'SELECT 1' >/dev/null 2>&1 \
        && docker exec "$redis_name" redis-cli ping >/dev/null 2>&1 \
        && docker exec "$cluster_name" redis-cli ping >/dev/null 2>&1; then break; fi
      if [[ "$attempt" == 60 ]]; then echo 'Test databases failed to start' >&2; exit 1; fi
      sleep 1
    done
    docker exec "$cluster_name" redis-cli CONFIG SET cluster-announce-ip 127.0.0.1 >/dev/null
    docker exec "$cluster_name" redis-cli CONFIG SET cluster-announce-port "$cluster_port" >/dev/null
    docker exec "$cluster_name" redis-cli CLUSTER ADDSLOTSRANGE 0 16383 >/dev/null
    for attempt in {1..30}; do
      cluster_state="$(docker exec "$cluster_name" redis-cli CLUSTER INFO)"
      if [[ "$cluster_state" == *'cluster_state:ok'* ]]; then break; fi
      if [[ "$attempt" == 30 ]]; then echo 'Test Redis Cluster failed to start' >&2; exit 1; fi
      sleep 1
    done
    export LITEFLOW_TEST_DOCKER=true LITEFLOW_TEST_SHARED_STORAGE=true
    export LITEFLOW_TEST_MYSQL_URL="jdbc:mysql://127.0.0.1:${mysql_port}/liteflow_storage_test?allowPublicKeyRetrieval=true&useSSL=false"
    export LITEFLOW_TEST_REDIS_URI="redis://127.0.0.1:${redis_port}"
    export LITEFLOW_TEST_REDIS_CLUSTER_URI="redis://127.0.0.1:${cluster_port}"
    "$maven" "${maven_args[@]}" -pl liteflow-testcase-el/liteflow-testcase-el-agent-harness -am \
      -Dtest=StorageBackendGuideLiveTest,UnifiedStorageLiveTest,SessionSandboxDockerTest,AgentConversationProcessTest \
      -Dsurefire.failIfNoSpecifiedTests=false test
    "$maven" "${maven_args[@]}" -pl liteflow-testcase-el/liteflow-testcase-el-agent -am \
      -Pagent-docker-it -Dtest=AgentTestLayoutContractTest -Dsurefire.failIfNoSpecifiedTests=false \
      "-Dliteflow.agent.test.docker-image=$LITEFLOW_SANDBOX_TEST_IMAGE" verify
    ;;
  live)
    "$maven" "${maven_args[@]}" -pl liteflow-testcase-el/liteflow-testcase-el-agent -am -Pagent-live test
    ;;
  *) echo 'Usage: bash liteflow-testcase-el/scripts/run-agent-guide-tests.sh [unit|integration|live]' >&2; exit 2 ;;
esac
