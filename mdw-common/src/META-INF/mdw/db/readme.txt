META_INF contents to address this issue:
https://github.com/CenturyLinkCloud/mdw/issues/377

Queries in rdbms-specific upgrade json array are executed sequentially.
Each "check" query returns an empty ResultSet if the associated "upgrade" query has not yet been applied.

JSON files here should be cleaned out at the time of each point release (see build.md).
