# Git Workflow — fetch, pull, merge, PR, and the branch loop

A reference for the day-to-day cycle used on this project. Written after getting genuinely tangled up
once (local `main` went stale while a PR merged on GitHub without anyone pulling it down) — see
[PROGRESS.md](PROGRESS.md) 2026-08-2x entries for that story.

## Local vs remote — the thing that causes most confusion

You have **local** branches (your machine) and **remote** branches (GitHub, called `origin`). They are
separate copies. Git never auto-syncs them — every sync is something you explicitly run.

```
Your machine                          GitHub ("origin")
─────────────                         ─────────────────
main                                  origin/main
my-feature-branch                     origin/my-feature-branch
```

`origin/main` on your machine is not live GitHub — it's a cached snapshot of what GitHub looked like
the last time you ran `fetch` or `pull`. It can be stale.

## The four operations, precisely

| Command | What it actually does | Touches your files? |
|---|---|---|
| `git fetch` | Downloads new commits from GitHub into your local `origin/*` refs | No — safe to run anytime |
| `git pull` | `fetch`, then immediately `merge origin/<current-branch>` into the branch you're on | Yes |
| `git merge` | Combines one branch's history into another | Yes, when run locally |
| **Pull Request (PR)** | Not a git command — a GitHub feature: "merge branch A into branch B, after review." Merging a PR updates GitHub's copy only; your local machine doesn't know until you `fetch`/`pull`. | No (until you pull) |

## The loop — repeat this for every chunk of work

```
1. Make sure main is up to date locally
        │
        ├─ git switch -c my-branch          (branch off main, short-lived, one topic)
        │
        ├─ edit, git add, git commit        (repeat)
        │
        ├─ git push -u origin my-branch     (first push needs -u; after that just `git push`)
        │
        ├─ open a PR on GitHub: my-branch → main
        │
        ├─ review, click "Merge" on GitHub
        │        (origin/main updates — your LOCAL main does not, yet)
        │
        ├─ git switch main
        ├─ git pull                         (local main catches up)
        │
        ├─ git branch -d my-branch          (delete the finished branch locally)
        │  (optionally delete on GitHub too — the PR page has a "Delete branch" button)
        │
        └─ back to step 1
```

**Rule of thumb:** never commit directly on `main`. Every piece of work gets its own branch, created
fresh off an up-to-date `main`. This single habit is what prevents local `main` from silently going
stale (which is exactly what happened before — a commit sat on local `main` for a week, unpushed and
unrelated to what actually landed on GitHub).

## Quick answers to specific situations

**"There's a change in another branch, how do I get it into `main`?"**
Push that branch → open a PR into `main` → merge the PR on GitHub → `git switch main && git pull` locally.

**"I just pulled changes into `main` — what now?"**
Branch off the fresh `main` for your next piece of work: `git switch -c next-thing`. Don't keep working
on the branch that just got merged — it's done, delete it.

**"How do I know if my local `main` is stale?"**
```bash
git fetch origin
git status
```
After `fetch`, `git status` on `main` will say "ahead by N", "behind by N", or "diverged" relative to
`origin/main` if there's a mismatch. If it says nothing, you're in sync.

**"I have uncommitted changes but need to switch branches / sync main — will I lose them?"**
```bash
git stash push -u -m "description"   # parks changes, including new untracked files
# ...do the branch switch/sync/whatever...
git stash pop                        # brings them back, on whichever branch you're now on
```
