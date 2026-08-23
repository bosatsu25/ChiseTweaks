# PIT scope

PIT is kept at the project 96% mutation and test-strength threshold, but it targets semantic policy/state-transition code rather than UI coordinate arithmetic.

`ChiseTweaksSettingsLayout` remains inside the JaCoCo 96% coverage scope and is protected by explicit boundary-value/overlap JUnit tests. `PreReleaseUiPolicy` and `WorksiteHighlightProfilePolicy` remain mutation-tested because their mutations represent user-visible availability and profile behavior.
