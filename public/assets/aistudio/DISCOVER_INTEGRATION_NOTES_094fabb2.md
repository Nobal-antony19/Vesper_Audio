# Weighing licence options and launch terms for the project.

# 

# Your plan is good. Open-source on GitHub, then Reddit and LinkedIn, is the right route for a privacy-first player like this. Here’s how I’d run it.

# 

# Before you post anything

# 

# Fix the basics first. Songs must load fast, search must play, Next at the last track must not loop, and Appearance Studio and the Downloader must be gone. A beta that fails on day one gets one chance with Reddit.

# Add a licence. GPL-3.0 is what Auxio uses and suits a community player. Don’t copy Auxio’s code unless you keep its licence. Take ideas from it, not source.

# Check third-party terms.

# Audius: read their current API terms.

# MusicBrainz: set a real contact in the user-agent.

# Don’t scrape any streaming service.

# Sign the release APK. Make a keystore, back it up, and never lose it, because every future update must use the same one. Don’t commit it to GitHub.

# Check the build. Confirm the release APK size, and run it on at least one other phone and Android version besides your Redmi.

# 

# Repo contents

# 

# README with screenshots, a short feature list, what’s not supported (no Dolby, no downloads) and the privacy promise (no trackers, local library stays on the phone).

# LICENSE, CHANGELOG, PRIVACY.md, a bug-report template that asks for device, Android version and logs, and a CONTRIBUTING file.

# A GitHub Release marked “pre-release” (v0.9.0-beta), with the APK and its SHA-256 checksum.

# Mention Obtainium so users can auto-update from your releases. Consider IzzyOnDroid or F-Droid later.

# 

# Beta stages

# 

# Private (a week): you and family, plus 5–10 friends with different phones.

# Public beta: post on GitHub, Reddit and LinkedIn.

# Stable 1.0 once crash reports slow down.

# 

# Where to post

# 

# Reddit: r/androidapps and r/opensource. Check each subreddit’s self-promotion rules first. Lead with a screenshot and what’s different, and be honest that it’s a beta. Reply to every comment in the first day.

# LinkedIn: tell the build story (solo developer, AI-assisted, privacy-first) with a short screen recording.

# GitHub: pin the repo, add topics like android, music-player, media3, jetpack-compose, open-source, and open “good first issue” tickets.

# 

# What not to claim

# 

# Don’t promise “studio-grade” or “millions of songs”. Say “parametric EQ, spatial modes and legal streaming via Audius”.

# Don’t mention Dolby Atmos at all.

# 

# I can draft the README, the Reddit and LinkedIn posts, and the bug template next. Tell me the repo name and tagline and I’ll start.

