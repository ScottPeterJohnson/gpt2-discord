# GPT-2 Discord Bot

## What is this?
This is just for fun. It's a Discord bot that generates semi-coherent
gibberish based on whatever you prompt it with.

## Add it to your server
[Click here](
https://discordapp.com/api/oauth2/authorize?client_id=574391151583559721&permissions=2048&redirect_uri=https%3A%2F%2Fgithub.com%2FScottPeterJohnson%2Fgpt2-discord&scope=bot), but please be gentle.

## Ask it things
Type `!gpt (your question here)`

## Building
Make sure to run `git submodule update`.
Run `./buildall.sh`, then run the generated docker container with the DISCORD_TOKEN environment
variable set.