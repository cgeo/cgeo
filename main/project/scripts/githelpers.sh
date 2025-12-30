#!/usr/bin/env bash
# Git workflow helper functions for local repositories.
# Source this file in your shell (e.g., `. ./githelpers.sh`).

# Fail with a message to stderr
_git_fail() { echo "ERROR: $*" >&2; return 1; }

# Check if current directory is inside a git work tree
_git_assert_repo() {
  git rev-parse --is-inside-work-tree >/dev/null 2>&1 || _git_fail "Not inside a git repository."
}

# Create a safe remote name from owner/repo
_git_remote_name_from_repo_id() {
  # input: $1 = owner/repo
  local id="$1"
  local owner="${id%%/*}"
  local repo="${id##*/}"
  local base="gh-${owner}-${repo}"
  local name="$base"
  local i=1
  # Ensure the name doesn't collide
  while git remote | grep -qx "$name"; do
    i=$((i+1))
    name="${base}-${i}"
  done
  printf "%s" "$name"
}

# Find a remote name that points to a given URL, or empty if none
_git_find_remote_by_url() {
  # input: $1 = url
  local url="$1" r
  for r in $(git remote); do
    if git remote get-url --all "$r" 2>/dev/null | grep -qxF "$url"; then
      printf "%s" "$r"
      return 0
    fi
  done
  return 1
}

# git_checkout_local REMOTE_REPO BRANCH
# - REMOTE_REPO: either a remote name (e.g., origin) or GitHub repo id (owner/repo)
# - BRANCH: branch name to check out locally tracking that remote branch
git_checkout_local() {
  local REMOTE_REPO="$1"
  local BRANCH="$2"

  _git_assert_repo || return 1

  if [ -z "$REMOTE_REPO" ] || [ -z "$BRANCH" ]; then
    _git_fail "Usage: git_checkout_local <remote-name|owner/repo> <branch>"
    return 1
  fi

  local remote_name=""
  local remote_url=""

  if printf "%s" "$REMOTE_REPO" | grep -q "/"; then
    # Treat as GitHub repo identifier owner/repo
    remote_url="git@github.com:${REMOTE_REPO}.git"

    # Verify repository exists (and is accessible)
    if ! git ls-remote --exit-code "$remote_url" >/dev/null 2>&1; then
      _git_fail "GitHub repository '$REMOTE_REPO' not found or inaccessible via SSH ($remote_url)."
      return 1
    fi

    # Find or create a local remote pointing to this URL
    remote_name="$(_git_find_remote_by_url "$remote_url" || true)"
    if [ -z "$remote_name" ]; then
      remote_name="$(_git_remote_name_from_repo_id "$REMOTE_REPO")"
      if ! git remote add "$remote_name" "$remote_url"; then
        _git_fail "Failed to add remote '$remote_name' with URL '$remote_url'."
        return 1
      fi
      echo "Added remote '$remote_name' -> $remote_url"
    fi
  else
    # Treat as remote name
    remote_name="$REMOTE_REPO"
    if ! git remote get-url "$remote_name" >/dev/null 2>&1; then
      _git_fail "Remote '$remote_name' does not exist in this repository."
      return 1
    fi
    remote_url="$(git remote get-url "$remote_name" 2>/dev/null)"
  fi

  # Ensure the branch exists on the remote
  if ! git ls-remote --exit-code --heads "$remote_name" "$BRANCH" >/dev/null 2>&1; then
    _git_fail "Branch '$BRANCH' does not exist on remote '$remote_name'."
    return 1
  fi

  # Fetch the branch ref
  if ! git fetch --no-tags --prune "$remote_name" "$BRANCH"; then
    _git_fail "Failed to fetch '$BRANCH' from '$remote_name'."
    return 1
  fi

  # Check if local branch exists
  if git show-ref --verify --quiet "refs/heads/$BRANCH"; then
    # Existing local branch: check it out and set upstream to the specified remote branch
    if ! git checkout "$BRANCH"; then
      _git_fail "Failed to checkout local branch '$BRANCH'."
      return 1
    fi
    if ! git branch --set-upstream-to="$remote_name/$BRANCH" "$BRANCH" >/dev/null 2>&1; then
      _git_fail "Failed to set upstream for '$BRANCH' to '$remote_name/$BRANCH'."
      return 1
    fi
  else
    # Create new local branch tracking the remote branch
    if ! git checkout -b "$BRANCH" --track "$remote_name/$BRANCH"; then
      _git_fail "Failed to create and checkout local branch '$BRANCH' tracking '$remote_name/$BRANCH'."
      return 1
    fi
  fi

  echo "Checked out local branch '$BRANCH' tracking '$remote_name/$BRANCH'."
}

# git_squash MESSAGE BASE_BRANCH_OR_COMMIT_COUNT
# Squash commits on the current branch:
# - If BASE_BRANCH_OR_COMMIT_COUNT is a branch name, squash all commits since the fork point from that base branch.
# - If BASE_BRANCH_OR_COMMIT_COUNT is a positive integer N, squash the last N commits on the current branch.
git_squash() {
  local MESSAGE="$1"
  local BASE_BRANCH_OR_COMMIT_COUNT="$2"

  _git_assert_repo || return 1

  if [ -z "$MESSAGE" ] || [ -z "$BASE_BRANCH_OR_COMMIT_COUNT" ]; then
    _git_fail "Usage: git_squash <commit-message> <base-branch|commit-count>"
    return 1
  fi

  # Ensure we are on a branch (not detached HEAD)
  local current_branch
  current_branch="$(git symbolic-ref --quiet --short HEAD)" || true
  if [ -z "$current_branch" ]; then
    _git_fail "Detached HEAD state. Please checkout a branch before squashing."
    return 1
  fi

  # Ensure working tree is clean to avoid mixing uncommitted changes into the squash
  if ! git diff --quiet || ! git diff --cached --quiet; then
    _git_fail "Working tree has uncommitted changes. Please commit or stash them before squashing."
    return 1
  fi

  local commit_count
  local start_point
  local start_point_desc

  if [[ "$BASE_BRANCH_OR_COMMIT_COUNT" =~ ^[0-9]+$ ]]; then
    # Numeric mode: squash last N commits
    commit_count="$BASE_BRANCH_OR_COMMIT_COUNT"

    if [ "$commit_count" -le 0 ]; then
      _git_fail "Commit count must be a positive integer."
      return 1
    fi

    # Verify we have at least N commits to squash
    if ! git rev-parse --verify "HEAD~$commit_count" >/dev/null 2>&1; then
      _git_fail "Not enough commits on '$current_branch' to squash the last $commit_count commit(s)."
      return 1
    fi

    start_point="$(git rev-parse "HEAD~$commit_count")"
    start_point_desc="$(git show -s --pretty=format:'%h %s' "$start_point")"
    echo "Starting point for squash (HEAD~$commit_count): $start_point_desc"
    echo "Commits to squash ($commit_count):"
    git log --reverse --pretty=format:'- %h %s' "HEAD~${commit_count}..${current_branch}"

    # Confirm
    printf "Proceed to squash these %s commit(s) into one with your message? [y/N]: " "$commit_count"
    local ans
    read -r ans
    case "$ans" in
      y|Y|yes|YES)
        ;;
      *)
        echo "Aborted."
        return 1
        ;;
    esac

    # Soft reset to HEAD~N, then create single commit
    if ! git reset --soft "HEAD~${commit_count}"; then
      _git_fail "Failed to reset to HEAD~${commit_count}."
      return 1
    fi
    if ! git commit -m "$MESSAGE"; then
      _git_fail "Failed to create the squashed commit."
      return 1
    fi
  else
    # Branch mode: squash commits since fork point from base branch
    local base_branch="$BASE_BRANCH_OR_COMMIT_COUNT"

    # Ensure base branch exists locally
    if ! git show-ref --verify --quiet "refs/heads/$base_branch"; then
      _git_fail "Base branch '$base_branch' does not exist locally."
      return 1
    fi

    # Determine fork point (try --fork-point first, fall back to merge-base)
    local fork_point
    fork_point="$(git merge-base --fork-point "$base_branch" "$current_branch" 2>/dev/null)" || true
    if [ -z "$fork_point" ]; then
      fork_point="$(git merge-base "$base_branch" "$current_branch" 2>/dev/null)" || true
    fi
    if [ -z "$fork_point" ]; then
      _git_fail "Could not determine fork point between '$base_branch' and '$current_branch'."
      return 1
    fi

    # Determine commits to squash
    commit_count="$(git rev-list --count "${fork_point}..${current_branch}")"
    if [ "$commit_count" -eq 0 ]; then
      _git_fail "No commits to squash; '$current_branch' is at the fork point of '$base_branch'."
      return 1
    fi

    local fork_point_desc
    fork_point_desc="$(git show -s --pretty=format:'%h %s' "$fork_point")"
    echo "Fork point from '$base_branch': $fork_point_desc"
    echo "Commits to squash ($commit_count):"
    git log --reverse --pretty=format:'- %h %s' "${fork_point}..${current_branch}"

    # Confirm
    printf "Proceed to squash these %s commit(s) into one with your message? [y/N]: " "$commit_count"
    local ans
    read -r ans
    case "$ans" in
      y|Y|yes|YES)
        ;;
      *)
        echo "Aborted."
        return 1
        ;;
    esac

    # Soft reset to fork point, then create single commit
    if ! git reset --soft "$fork_point"; then
      _git_fail "Failed to reset to fork point $fork_point."
      return 1
    fi
    if ! git commit -m "$MESSAGE"; then
      _git_fail "Failed to create the squashed commit."
      return 1
    fi
  fi

  echo "Squash completed on branch '$current_branch'. Created one commit with your message."
  echo "Note: If this branch was previously pushed, you will need to force-push to update the remote (not done automatically)."
}