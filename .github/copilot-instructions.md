Whenever copilot creates code for this repository:
* make sure to follow all rules defined in ruleset.xml
* Especially: make sure no unused import statements are left in source files and import statements have correct order
* Especially: make sure all defined variables and parameters are declared final if possible
* Create Pull Requests always towards github repository cgeo/cgeo, where the branch to create the PR against should match the branch where copilot was working on in the original repository
* Prefix each commit message and each PR title with "fix #issue: " where "issue" is the issue number this PR was created for
* Do NOT create an initial commit "Initial Commit" when creating a new PR
