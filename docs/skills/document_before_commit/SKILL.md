---
name: document-before-commit
description: Triggers before every commit and push to ensure documentation is updated and explicit user permission is granted.
---

# Document Before Commit Skill

**CRITICAL RULE:** Do NOT commit or push code to GitHub without EXPLICIT permission from the user. 
Even if you are confident the code works, you must stop, summarize what you've done, and ask the user if they would like you to commit and push the changes.

## Workflow

1. **Feature Completion**: Once you complete a feature or bug fix and verify it builds successfully, you must immediately update the documentation in the `docs/` folder (such as `docs/architecture.md` or a new feature-specific markdown file).
2. **Seek Permission**: After documentation is generated/updated, tell the user that the feature is complete and the documentation is updated. Explicitly ask: "Would you like me to commit and push these changes?"
3. **Execution**: ONLY after the user responds affirmatively (e.g., "yes", "proceed", "commit it"), you may use the `run_command` tool to execute `git add`, `git commit`, and `git push`.
