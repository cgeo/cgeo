# Contributing to c:geo

Thank you for your interest in contributing! Here is everything you need to get started.

## Before you start: open an issue first

Please **tell us in the [issue tracker](https://github.com/cgeo/cgeo/issues) before you start hacking** on a new feature or bug fix. This avoids wasted effort if a change doesn't fit the existing architecture.

If a [good first issue](https://github.com/cgeo/cgeo/contribute) already covers what you want to do, comment on it to let the team know you are working on it.

## Setting up your development environment

1. **Fork** this repository — do not clone `cgeo/cgeo` directly, or you won't be able to open a pull request later.
2. Clone your fork locally and set `cgeo/cgeo` as the upstream remote.
3. Follow the [IDE setup guide](https://github.com/cgeo/cgeo/wiki/IDE) for Android Studio configuration.
4. New to git or GitHub? See the [git/GitHub beginners guide](https://github.com/cgeo/cgeo/wiki/Working-on-c%3Ageo-for-git-beginners).

## Branches

| Branch | Purpose |
|---|---|
| `master` | New features — nightly builds are made from here |
| `release` | Bug fixes for already-released features |

Target `release` for bug fixes; target `master` for new features. See the [merge procedure wiki page](https://github.com/cgeo/cgeo/wiki/Merge-procedure) for details.

## Translations

Translations are managed via [Crowdin](https://crowdin.com/project/cgeo). **Do not edit the string resource XML files directly** — your changes will be overwritten on the next Crowdin sync. Submit translation changes through Crowdin instead.

## AI-assisted contributions

You are welcome to use AI coding tools (Copilot, ChatGPT, Claude, etc.) when contributing, subject to these expectations:

- **Disclose it**: mention AI assistance in your pull request description.
- **Own the output**: you are responsible for reviewing, testing, and standing behind everything you submit, regardless of how it was generated.
- **No bulk generation**: do not use AI to mass-create issues, comments, or pull requests. Contributions should reflect genuine effort and understanding.

## Pull request checklist

- [ ] An issue exists and you have commented on it (or you opened one)
- [ ] Your branch targets the correct base (`master` or `release`)
- [ ] The build passes locally
- [ ] Translation changes were made via Crowdin, not by editing XML directly
- [ ] AI assistance (if any) is disclosed in the PR description
