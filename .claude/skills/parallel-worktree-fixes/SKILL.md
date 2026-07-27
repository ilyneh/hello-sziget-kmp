---
name: parallel-worktree-fixes
description: Work on multiple fixes or issues in parallel using one git worktree per fix, rather than switching branches serially in the main checkout. Use when asked to work on multiple fixes/issues in parallel.
---

# Parallel fixes in worktrees

When asked to work on multiple fixes/issues in parallel, use one git worktree per fix rather than switching branches serially in the main checkout:

1. For each fix, create a branch named to describe the issue (e.g. `fix-schedule-timezone-bug`, not `fix-1` or `wip`), and a worktree for it: `git worktree add ../<branch-name> -b <branch-name>`.
2. Work each fix independently within its own worktree — no shared state or sequencing between them unless the fixes actually depend on each other.
3. When a fix is complete and verified, commit and push its branch to `origin` under the same branch name (`git push -u origin <branch-name>`).
4. Remove the worktree once its branch is pushed: `git worktree remove ../<branch-name>`.
5. Do not open PRs or merge automatically — the parent job's final output should just be the list of pushed branch names, so the user can review/PR each one themselves.
