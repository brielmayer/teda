# Changelog

## [Unreleased]

## [1.0.0-RC2]

### Added

- `[NULL]` in a cell stands for a SQL `NULL`, when loading as well as when comparing. An empty cell
  keeps meaning the empty String, so the two cases are no longer indistinguishable. On Oracle they
  still are, because Oracle stores the empty String as `NULL`
- Tables may be addressed as `SCHEMA.TABLE`, and as `CATALOG.SCHEMA.TABLE` on SQL Server. Every part
  is validated on its own, so a qualified name cannot smuggle anything into the query
- A `#Teda` cockpit without a single action, and a `#Table` without a header row, are reported
  instead of letting the run pass without doing anything

### Changed

- Excel documents are read through Apache POI instead of fastexcel. This also reads the legacy BIFF
  format, which Excel on macOS writes even when the file carries an `.xlsx` extension. The parser
  classes moved from `com.brielmayer.teda.parser.xlsx` to `com.brielmayer.teda.parser.excel`
- Error messages name the sheet and the table an error occurred in, for example
  `Error comparing table "STUDENT" (sheet "TestData") in row 1`. They also no longer speak of a
  *bean* where a table is meant
- An error that wraps another exception now carries its reason in the message, not only in the
  stack trace. A failing statement reports the database's own error text
- A missing value is shown as `[NULL]` rather than as `(null) "null"`, matching what is written in
  the spreadsheet
- `SortComparator` takes a description of what is being sorted as its second constructor argument,
  so a sorting error can name the sheet, the table, and whether the expected or the actual data is
  at fault. The single-argument constructor is gone
- Dependency upgrades, among them JUnit 6, Testcontainers 2, MySQL Connector/J 26 and Logback 1.6

### Fixed

- On PostgreSQL, a row from a CSV document no longer fails against a non-text column. CSV cells
  reach the driver as Strings, and PostgreSQL is the only supported database that refuses to cast
  `varchar` to `integer` on insert. Such parameters are now sent with an unspecified type, so the
  server derives the type from the target column

## [1.0.0-RC1]

First release candidate.
