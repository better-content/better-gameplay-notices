# Better Gameplay Notices

## Scope and authority

This repository owns its mod-specific behavior and authoring inputs. Read [local instructions](AGENTS.md)
and the [shared documentation/policy index](../../better-content-modpack/docs/README.md).


A small Forge 1.20.1 mod providing one queued, input-transparent gameplay notice surface.
Feature mods publish localized notices through `GameplayNotices.send` and choose the Threads
or Combat theme. The queue pauses while another screen is open and clears on logout.

## Bounded playtest diagnostics

`bc.playtest.better_gameplay_notices.*` uses loader-provided SLF4J. INFO reports startup;
DEBUG records channel submission, client dispatch, queue admission/duplicate/expiry/clear and
first-frame sound submission. Submission is not proof of visible pixels or audible playback.
Notice titles, hints and arbitrary caller identities are never logged; theme/count-only records
cannot correlate individual notices across processes. Render frames do not produce repeated records.
One process-local mod budget shares 120 detail records/minute with burst 20, 256 aggregate keys,
2048-character records and 30-second coalesced summaries; disconnect/server stop clears aggregate
state without replenishing the allowance. Normal standalone logging stays INFO; the pack enables
scoped DEBUG. No new packet fields, queue limits or persistent diagnostic history are introduced.
