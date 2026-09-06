#!/usr/bin/env bash

# Deploy a verified pair of GHCR images to one Mac mini Compose stack.
# This script is only for the protected shopping-deploy GitHub Actions runner.

set -euo pipefail

fail() {
  echo "Deployment failed: $*" >&2
  exit 1
}

usage() {
  echo "Usage: $0 <staging|prod> <release-manifest.json>" >&2
  exit 64
}

[ "$#" -eq 2 ] || usage

environment="$1"
manifest_path="$2"

case "$environment" in
  staging)
    env_file="/Users/yklin/services/shopping-website/staging/staging.env"
    compose_override="docker-compose.staging.yml"
    compose_project="shopping-staging"
    frontend_port="8081"
    ;;
  prod)
    env_file="/Users/yklin/services/shopping-website/prod/prod.env"
    compose_override="docker-compose.prod.yml"
    compose_project="shopping-prod"
    frontend_port="8082"
    ;;
  *)
    usage
    ;;
esac

[ -f "$manifest_path" ] || fail "release manifest does not exist"
[ -f "$env_file" ] || fail "environment file does not exist: $env_file"
command -v jq >/dev/null || fail "jq is required"
command -v podman >/dev/null || fail "podman is required"
command -v curl >/dev/null || fail "curl is required"

repository="${GITHUB_REPOSITORY:-}"
git_sha="${GITHUB_SHA:-}"
github_token="${GITHUB_TOKEN:-}"
github_actor="${GITHUB_ACTOR:-}"

[ -n "$repository" ] || fail "GITHUB_REPOSITORY is required"
[ -n "$git_sha" ] || fail "GITHUB_SHA is required"
[ -n "$github_token" ] || fail "GITHUB_TOKEN is required"
[ -n "$github_actor" ] || fail "GITHUB_ACTOR is required"

manifest_schema="$(jq -er '.schemaVersion' "$manifest_path")" || fail "manifest schemaVersion is missing"
manifest_sha="$(jq -er '.gitSha' "$manifest_path")" || fail "manifest gitSha is missing"
spring_image="$(jq -er '.springImage' "$manifest_path")" || fail "manifest springImage is missing"
frontend_image="$(jq -er '.frontendImage' "$manifest_path")" || fail "manifest frontendImage is missing"

[ "$manifest_schema" = "1" ] || fail "unsupported manifest schemaVersion: $manifest_schema"
[ "$manifest_sha" = "$git_sha" ] || fail "manifest gitSha does not match the workflow commit"

registry_repository="$(printf '%s' "$repository" | tr '[:upper:]' '[:lower:]')"
expected_spring_prefix="ghcr.io/${registry_repository}-spring@sha256:"
expected_frontend_prefix="ghcr.io/${registry_repository}-frontend@sha256:"

case "$spring_image" in
  "$expected_spring_prefix"*) ;;
  *) fail "manifest spring image is not from this repository" ;;
esac

case "$frontend_image" in
  "$expected_frontend_prefix"*) ;;
  *) fail "manifest frontend image is not from this repository" ;;
esac

spring_digest="${spring_image#"$expected_spring_prefix"}"
frontend_digest="${frontend_image#"$expected_frontend_prefix"}"
[[ "$spring_digest" =~ ^[0-9a-f]{64}$ ]] || fail "spring image digest is malformed"
[[ "$frontend_digest" =~ ^[0-9a-f]{64}$ ]] || fail "frontend image digest is malformed"

printf '%s' "$github_token" | podman login ghcr.io --username "$github_actor" --password-stdin

export SPRING_IMAGE="$spring_image"
export FRONTEND_IMAGE="$frontend_image"

podman compose \
  --env-file "$env_file" \
  --project-name "$compose_project" \
  --file docker-compose.yml \
  --file "$compose_override" \
  pull

podman compose \
  --env-file "$env_file" \
  --project-name "$compose_project" \
  --file docker-compose.yml \
  --file "$compose_override" \
  up --detach --remove-orphans

curl --fail --silent --show-error --retry 12 --retry-connrefused --retry-delay 5 \
  "http://127.0.0.1:${frontend_port}/" >/dev/null
curl --fail --silent --show-error --retry 6 --retry-connrefused --retry-delay 5 \
  "http://127.0.0.1:${frontend_port}/api/products" >/dev/null

echo "${environment} deployment and loopback smoke test succeeded for ${git_sha}"
