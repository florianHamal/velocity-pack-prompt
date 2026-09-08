## Velocity Pack Prompt

A simple plugin that sends a resource-pack prompt to players on their first proxy join.

Background: I discovered a bug where, with ViaVersion and Velocity, pack prompts from 1.8 servers went missing. This plugin sends the prompt on first proxy join so that subsequent packs from the different backend servers are accepted without an additional prompt. For this, the pack prompt doesn't even need to point to a valid pack URL — it just needs to be sent.
