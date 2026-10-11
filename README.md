# Better Gameplay Notices

## Scope and authority

This repository owns its mod-specific behavior and authoring inputs. Read [local instructions](AGENTS.md)
and the [shared documentation/policy index](../../better-content-modpack/docs/README.md).


A small Forge 1.20.1 mod providing one queued, input-transparent gameplay notice surface.
Feature mods publish localized notices through `GameplayNotices.send` and choose the Threads
or Combat theme. The queue pauses while another screen is open and clears on logout.
