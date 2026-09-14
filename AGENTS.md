# Lebens Rhythmus (LR) — tlab29.com — Agent Instructions

> This file exists specifically for **Google Antigravity** ("AG"). It stays
> in English for the same reason `CLAUDE.md` does — this is tooling
> configuration an agent compiles into behavior, not documentation for a
> human reader (see `CLAUDE.md`'s own Language conventions section, which
> registers this same exception for both files).
>
> **Do not assume this file supports Claude Code's `@path/to/file.md`
> auto-embed syntax** — that has not been verified to work in Antigravity.
> Everything you need for a first, safe change is inlined directly below,
> not just referenced. For anything beyond that, actually open and read the
> referenced files — don't assume their content from the filename alone.

## Read this before anything else

1. `docs/README.md` — human-facing index of the whole `docs/` folder, what
   each subfolder is for.
2. `docs/context/KNOWN_ISSUES.md` — short, concrete "don't step here again"
   list. Always read in full, it's kept short on purpose.
3. `docs/context/CODING_PROTOCOL.md` — the Architecture Pre-Check every
   code change is expected to go through, plus a forbidden-patterns list
   built from real past incidents in this exact repo, not generic advice.
4. `CLAUDE.md` (repo root) — the equivalent file for Claude Code sessions.
   If anything here conflicts with it, that's a bug — flag it, don't
   silently pick one.

## Non-negotiable discipline: verify, don't infer

This project has been broken in production twice in one day (2026-09-13)
by changes that were textbook-correct **in the abstract** but wrong for
**this specific deployment's actual topology**. The failure pattern both
times: a plausible, generic best-practice was applied without checking
whether it held for the real, current state of this repo's infrastructure.

**Rule: for any factual claim about how this system behaves, either (a)
verify it directly against the actual file content or a live command's
real output, or (b) state explicitly that it's unverified/assumed.** Do
not reason from "how this technology typically works" and present the
conclusion as settled fact for this deployment. If you can build the
project locally to check something (e.g. `npm run build` and read the
real output `index.html`) before proposing a fix, do that rather than
guessing from source alone — see the CSP incident below for exactly why.

## Two concrete gotchas that already caused prod outages — read before touching Ingress/CSP/nginx

**1. This site is served through a Cloudflare Tunnel, not a public IP with
TLS terminated at the origin.** `cloudflared` (running on a separate VM
from the k3s cluster, see `docs/infra/INFRA-LR.md`) forwards traffic to
Traefik's `web` entrypoint as **plain HTTP**, always — regardless of
whether the original browser request was HTTP or HTTPS. Full setup
reference: `devops/cloud_flare/README.md`.

**Consequence: never attach an HTTPS-redirect (`redirectScheme` or
equivalent) to the Ingress/Traefik router for this site.** Traefik cannot
observe the original request's scheme, so it will unconditionally 301
every request back to `https://`, which the tunnel then re-delivers as
plain HTTP again — an infinite redirect loop
(`ERR_TOO_MANY_REDIRECTS`, real incident, see
`docs/context/CHANGELOG.md` 2026-09-13, commit `5354c52`).
HTTPS enforcement for browsers belongs at the Cloudflare edge (SSL/TLS →
Edge Certificates → "Always Use HTTPS"), not on the origin Ingress.

**2. `frontend-svelte` is a SvelteKit `adapter-static` build — its
`index.html` contains inline `<script>` tags whose exact content is NOT
stable across builds or across requests, so a static CSP `sha256-` hash
allowlist cannot correctly cover them:**
- SvelteKit's own bootstrap script embeds a `__sveltekit_<random>` variable
  name that is freshly randomized on **every build**, even with byte-identical
  source.
- Cloudflare's own edge-injected script (Bot Fight Mode /
  `/cdn-cgi/challenge-platform/...`) carries a random token **per request**,
  and isn't part of this project's build at all.

**Consequence: don't hash-pin `script-src` in `frontend-svelte/nginx.conf`'s
CSP.** Use `script-src 'self' 'unsafe-inline'` until a proper fix ships
(SvelteKit's native `kit.csp` hash mode, tracked as ticket `LR-110`; the
Cloudflare-injected script needs a separate decision, tracked as `LR-111`).
Real incident: `docs/context/CHANGELOG.md` 2026-09-13, commit `239ed1b`.

Before proposing anything else that touches `devops/helm/lr-app/**`,
`frontend-svelte/nginx.conf`, or CORS/CSP config: read
`docs/infra/INFRA-LR.md` and `devops/cloud_flare/README.md` in full first.

## Ticket risk tiers (same convention as `CLAUDE.md`)

State the tier explicitly before proposing a change: **LOW** (pure
frontend/CSS/copy) / **MED** (new endpoint, no auth/payment/children's-data
touch) / **HIGH** (auth, children's personal data, payments, or an
ADR-level decision) / **INFRA** (k8s manifests, Cloudflare Tunnel, CI/CD,
VM config). If a change ends up touching `devops/helm/**` or
`frontend-svelte/nginx.conf` even though the ticket wasn't originally
tiered INFRA — stop and re-declare the tier as INFRA before proceeding.
That exact gap (a CORS ticket quietly growing an Ingress-middleware change
never covered by its own scope or re-reviewed for it) is what caused the
first incident above.

## Workflow

1. State risk tier.
2. For MED/HIGH/INFRA: do the Architecture Pre-Check from
   `docs/context/CODING_PROTOCOL.md` — for INFRA-tier or anything touching
   Ingress/nginx/CSP, this now includes the Topology Check in
   `CODING_PROTOCOL.md` §4c.
3. Plan → wait for explicit human approval before writing code.
4. Write code.
5. Independent review before presenting a diff for approval (this project
   uses a dedicated reviewer subagent for Claude Code sessions,
   `.claude/agents/architect-reviewer.md` — if AG has an equivalent
   multi-pass/independent-review mechanism, use it for MED/HIGH/INFRA).
6. Conventional commit message, English, include `LR-XXX` if tied to a
   ticket. **Never add a co-author trailer unless explicitly asked.**
7. Update `docs/context/CHANGELOG.md` (per-file entries) — and if the
   change touches anything in `docs/security/`'s scope, update
   `docs/security/ARCHITECTURE.md` in the **same** diff, not a follow-up.
8. Move the closed ticket from `docs/tickets/tickets.md` to
   `docs/tickets/archive.md`.
9. **Never `git commit`/`git push` without an explicit, fresh instruction
   for that specific action** — a prior approval does not carry over to
   the next commit.

## Language conventions (same as `CLAUDE.md`)

Docs under `docs/`: Russian by default. Code comments: English always.
This file and `CLAUDE.md`: English (tooling config, not human documentation).
Never invent a new English doc without flagging it as a deliberate,
explicit exception — see `docs/README.md`'s own convention on this.
