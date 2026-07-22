#!/usr/bin/env bash
set -Eeuo pipefail

readonly DEPLOY_DIR="/home/ubuntu/gilmoa/deploy/dev"
readonly CONTAINER_NAME="jejugilmoa-dev-app"
readonly HEALTH_URL="http://127.0.0.1:8080/health"
readonly OVERRIDE_FILE="${DEPLOY_DIR}/.deploy-image.override.yml"
readonly HEALTH_TIMEOUT_SECONDS=120
readonly HEALTH_INTERVAL_SECONDS=5

AWS_REGION="${1:-}"
ECR_REGISTRY="${2:-}"
ECR_REPOSITORY="${3:-}"
IMAGE_TAG="${4:-}"

if [[ ! "${AWS_REGION}" =~ ^[a-z]{2}-[a-z]+-[0-9]+$ ]]; then
  echo "오류: AWS 리전 값이 올바르지 않습니다." >&2
  exit 1
fi
if [[ ! "${ECR_REGISTRY}" =~ ^[0-9]+\.dkr\.ecr\.[a-z0-9-]+\.amazonaws\.com$ ]]; then
  echo "오류: ECR registry 값이 올바르지 않습니다." >&2
  exit 1
fi
if [[ ! "${ECR_REPOSITORY}" =~ ^[a-z0-9][a-z0-9._/-]*$ ]]; then
  echo "오류: ECR repository 값이 올바르지 않습니다." >&2
  exit 1
fi
if [[ "${ECR_REPOSITORY}" != "jejugilmoa-backend" ]]; then
  echo "오류: 허용된 ECR repository가 아닙니다." >&2
  exit 1
fi
if [[ "${ECR_REGISTRY}" != *".dkr.ecr.${AWS_REGION}.amazonaws.com" ]]; then
  echo "오류: ECR registry와 AWS 리전이 일치하지 않습니다." >&2
  exit 1
fi
if [[ ! "${IMAGE_TAG}" =~ ^[0-9a-f]{40}$ ]]; then
  echo "오류: 이미지 태그는 전체 Git commit SHA여야 합니다." >&2
  exit 1
fi

readonly NEW_IMAGE="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"

# 기존 Compose가 ${APP_IMAGE}를 사용하더라도 서버 .env를 수정하지 않고 새 이미지를 주입한다.
export APP_IMAGE="${NEW_IMAGE}"

cd "${DEPLOY_DIR}"

if [[ ! -f .env ]]; then
  echo "오류: 서버의 ${DEPLOY_DIR}/.env 파일이 없습니다." >&2
  exit 1
fi

APP_SERVICE="$(docker inspect --format '{{ index .Config.Labels "com.docker.compose.service" }}' "${CONTAINER_NAME}" 2>/dev/null || true)"
COMPOSE_PROJECT="$(docker inspect --format '{{ index .Config.Labels "com.docker.compose.project" }}' "${CONTAINER_NAME}" 2>/dev/null || true)"
if [[ -z "${APP_SERVICE}" || "${APP_SERVICE}" == "<no value>" ]]; then
  echo "오류: ${CONTAINER_NAME} 컨테이너에서 Compose app 서비스명을 확인할 수 없습니다." >&2
  exit 1
fi
if [[ ! "${APP_SERVICE}" =~ ^[a-zA-Z0-9][a-zA-Z0-9._-]*$ ]]; then
  echo "오류: 확인된 Compose app 서비스명이 올바르지 않습니다." >&2
  exit 1
fi
if [[ -z "${COMPOSE_PROJECT}" || "${COMPOSE_PROJECT}" == "<no value>" ]]; then
  echo "오류: ${CONTAINER_NAME} 컨테이너에서 Compose 프로젝트명을 확인할 수 없습니다." >&2
  exit 1
fi
if [[ ! "${COMPOSE_PROJECT}" =~ ^[a-z0-9][a-z0-9_-]*$ ]]; then
  echo "오류: 확인된 Compose 프로젝트명이 올바르지 않습니다." >&2
  exit 1
fi

COMPOSE_FILES=()
for candidate in compose.yml compose.yaml docker-compose.yml docker-compose.yaml; do
  if [[ -f "${candidate}" ]] && docker compose -f "${candidate}" config --services | grep -Fxq "${APP_SERVICE}"; then
    COMPOSE_FILES+=("${candidate}")
  fi
done

if (( ${#COMPOSE_FILES[@]} != 1 )); then
  echo "오류: ${APP_SERVICE} 서비스를 포함한 Compose 파일이 정확히 하나여야 합니다." >&2
  exit 1
fi
readonly COMPOSE_FILE="${COMPOSE_FILES[0]}"

readonly PREVIOUS_IMAGE="$(docker inspect --format '{{.Config.Image}}' "${CONTAINER_NAME}" 2>/dev/null || true)"
if [[ -z "${PREVIOUS_IMAGE}" ]]; then
  echo "오류: 배포 전 기존 이미지 이름을 확인할 수 없습니다." >&2
  exit 1
fi

write_image_override() {
  local image="$1"
  if [[ ! "${image}" =~ ^[a-zA-Z0-9][a-zA-Z0-9._:/@-]+$ ]]; then
    echo "오류: Compose override에 사용할 이미지 이름이 올바르지 않습니다." >&2
    return 1
  fi

  umask 077
  printf 'services:\n  %s:\n    image: %s\n' "${APP_SERVICE}" "${image}" > "${OVERRIDE_FILE}"
}

compose_app_up() {
  docker compose \
    --project-name "${COMPOSE_PROJECT}" \
    --project-directory "${DEPLOY_DIR}" \
    -f "${COMPOSE_FILE}" \
    -f "${OVERRIDE_FILE}" \
    up -d --no-deps --force-recreate "${APP_SERVICE}"
}

health_check() {
  local attempt=1 body remaining curl_timeout sleep_seconds
  local deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))

  while (( SECONDS < deadline )); do
    remaining=$((deadline - SECONDS))
    curl_timeout=$((remaining < 5 ? remaining : 5))
    if body="$(curl --fail --silent --show-error --max-time "${curl_timeout}" "${HEALTH_URL}" 2>/dev/null)" \
      && [[ "${body}" == "ok" ]]; then
      return 0
    fi
    echo "Health check 대기 중 (${attempt}회, 남은 시간 최대 ${remaining}초)..."
    remaining=$((deadline - SECONDS))
    if (( remaining <= 0 )); then
      break
    fi
    sleep_seconds=$((remaining < HEALTH_INTERVAL_SECONDS ? remaining : HEALTH_INTERVAL_SECONDS))
    sleep "${sleep_seconds}"
    ((attempt += 1))
  done
  return 1
}

echo "ECR 로그인 및 이미지 pull을 시작합니다. tag=${IMAGE_TAG}"
aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REGISTRY}" >/dev/null
docker pull "${NEW_IMAGE}"

write_image_override "${NEW_IMAGE}"
echo "${APP_SERVICE} 서비스만 새 이미지로 교체합니다."
compose_app_up

if health_check; then
  echo "배포 완료: health check가 성공했습니다. tag=${IMAGE_TAG}"
  exit 0
fi

echo "새 이미지 health check 실패. ${CONTAINER_NAME} 최근 로그를 출력합니다." >&2
docker logs --tail 100 "${CONTAINER_NAME}" 2>&1 || true

echo "이전 이미지로 ${APP_SERVICE} 서비스만 롤백합니다." >&2
write_image_override "${PREVIOUS_IMAGE}"
compose_app_up

if health_check; then
  echo "롤백 완료: 이전 이미지 health check가 성공했습니다." >&2
  exit 1
fi

echo "최종 실패: 롤백 후에도 health check가 실패했습니다." >&2
docker logs --tail 100 "${CONTAINER_NAME}" 2>&1 || true
exit 1
