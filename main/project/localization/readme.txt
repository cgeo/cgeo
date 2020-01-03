Checks for installed languages
==============================

These scripts are only relevant if you are not sure whether the crowdin translations are up-to-date.

* findmissingtranslations.sh

Search of missing translation strings in the language files other than English.
Creates a file <lang>.missing for all checked languages in this directory.

Parameter:
  * all     check all languages
  * <lang>  check a dedicated language


* findextratranslations.sh

Search of redundant strings in

Parameter:
  * -n      only display
  * -f:     force unused strings removal

Known false positives:
* auth_connected:
* settings_reauthorize:
Both defined in SettingsActivity.java, but not covered by the regex because of ternary operator (?:) before
Sample:
   .setSummary(getString(hasToken
       ? R.string.auth_connected
       : R.string.auth_unconnected));
