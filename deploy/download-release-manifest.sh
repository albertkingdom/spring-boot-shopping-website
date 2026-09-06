#!/usr/bin/env bash

# Download the immutable manifest produced by the successful master CI run for
# a given commit. It is used by the tag-triggered production workflow so that
# production never rebuilds or deploys a floating image tag.

set -euo pipefail

fail() {
  echo "Manifest download failed: $*" >&2
  exit 1
}

usage() {
  echo "Usage: $0 <git-sha> <output-path>" >&2
  exit 64
}

[ "$#" -eq 2 ] || usage

git_sha="$1"
output_path="$2"
repository="${GITHUB_REPOSITORY:-}"
github_token="${GITHUB_TOKEN:-}"
api_url="${GITHUB_API_URL:-https://api.github.com}"

[ -n "$repository" ] || fail "GITHUB_REPOSITORY is required"
[ -n "$github_token" ] || fail "GITHUB_TOKEN is required"
command -v curl >/dev/null || fail "curl is required"
command -v jq >/dev/null || fail "jq is required"
command -v unzip >/dev/null || fail "unzip is required"

work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

request_header="Authorization: Bearer ${github_token}"
runs_json="$work_dir/runs.json"
curl --fail --silent --show-error --location \
  --header "$request_header" \
  --header 'Accept: application/vnd.github+json' \
  "${api_url}/repos/${repository}/actions/workflows/ci.yml/runs?branch=master&event=push&status=success&head_sha=${git_sha}&per_page=20" \
  --output "$runs_json"

run_id="$(jq -er '.workflow_runs[0].id' "$runs_json")" || fail "no successful master CI run exists for ${git_sha}"
artifacts_json="$work_dir/artifacts.json"
curl --fail --silent --show-error --location \
  --header "$request_header" \
  --header 'Accept: application/vnd.github+json' \
  "${api_url}/repos/${repository}/actions/runs/${run_id}/artifacts?per_page=100" \
  --output "$artifacts_json"

artifact_url="$(jq -er --arg name "release-manifest-${git_sha}" \
  '.artifacts[] | select(.name == $name and .expired == false) | .archive_download_url' \
  "$artifacts_json" | head -n 1)" || fail "release manifest artifact is unavailable for ${git_sha}"

archive_path="$work_dir/release-manifest.zip"
curl --fail --silent --show-error --location \
  --header "$request_header" \
  --header 'Accept: application/vnd.github+json' \
  "$artifact_url" \
  --output "$archive_path"

mkdir -p "$(dirname "$output_path")"
unzip -p "$archive_path" release-manifest.json > "$output_path"
jq -e --arg git_sha "$git_sha" \
  '.schemaVersion == "1" and .gitSha == $git_sha and (.springImage | startswith("ghcr.io/")) and (.frontendImage | startswith("ghcr.io/"))' \
  "$output_path" >/dev/null || fail "downloaded manifest is invalid"
