# Teda Framework

**Data-driven database testing, described entirely in a spreadsheet.**

![Maven Workflow](https://github.com/brielmayer/teda/actions/workflows/maven.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/com.brielmayer.teda/teda?label=Maven%20Central)](https://central.sonatype.com/artifact/com.brielmayer.teda/teda)

Teda lets you write database and data-pipeline tests as an Excel or OpenDocument
spreadsheet instead of Java code. You describe **what data to load**, **what
process to trigger**, and **what the result should look like**. Teda inserts the
data, runs the process, reads the actual rows back, and compares them against your
expectations. If anything differs, the test fails with a precise diff.

It is particularly well suited for testing **ETL jobs, migrations, stored
procedures, and reports**, where you often load into one database and verify the
outcome in another.

![A Cockpit sheet in Excel with the #Teda marker, a scenario name, and one row of TRUNCATE/LOAD/EXECUTE/TEST actions.](docs/images/cockpit.png)

---

## Table of Contents

- [Why Teda?](#why-teda)
- [Supported Databases](#supported-databases)
- [Supported File Formats](#supported-file-formats)
- [Getting Started](#getting-started)
- [How It Works](#how-it-works)
- [The Spreadsheet Layout](#the-spreadsheet-layout)
- [Actions Reference](#actions-reference)
- [Type Handling](#type-handling)
- [Load vs. Test Database](#load-vs-test-database)
- [Custom Handlers](#custom-handlers)
- [FAQ](#faq)
- [Building from Source](#building-from-source)

---

## Why Teda?

- **No boilerplate.** A tester or business analyst can express a full test case in
  a spreadsheet, no JDBC, no assertion code.
- **Readable test data.** Input and expected output live side by side in tabular
  form, exactly how humans think about data.
- **Database-agnostic comparisons.** Teda normalizes types (numbers, dates, UUIDs,
  booleans) so the same test passes across MySQL, PostgreSQL, Oracle, and the rest,
  even when the JDBC drivers return different Java types.
- **Load here, verify there.** Use separate source and target databases to test
  real ETL / migration flows in a single test file.

---

## Supported Databases

| Database             | Auto-detected via JDBC |
|----------------------|:----------------------:|
| MySQL                |           ✅           |
| PostgreSQL           |           ✅           |
| Microsoft SQL Server |           ✅           |
| Oracle               |           ✅           |
| MariaDB              |           ✅           |
| H2                   |           ✅           |
| SQLite               |           ✅           |

The correct database implementation is resolved automatically from the
`DataSource` using Java's `ServiceLoader` mechanism. You only provide a
`javax.sql.DataSource`.

## Supported File Formats

| Format                   | Extension          |
|--------------------------|--------------------|
| Excel (OOXML)            | `.xlsx`            |
| Excel (legacy BIFF)      | `.xls`             |
| OpenDocument Spreadsheet | `.ods`             |
| CSV (directory)          | `.csv` files       |

Both Excel formats are handled transparently under `DocumentType.EXCEL`; the
parser detects the file signature and picks the right reader. This matters on
macOS in particular, where Excel and Numbers sometimes persist a workbook in
the legacy BIFF format even when the file carries an `.xlsx` extension.

For CSV, point Teda at a **directory** that contains one `.csv` file per sheet
(file name without the `.csv` becomes the sheet name). Useful when your test
data lives in git and should diff cleanly, no Excel required to edit it.

## Getting Started

Teda is a Java 11 library built with Maven. Add it as a test dependency:

```xml
<dependency>
    <groupId>com.brielmayer.teda</groupId>
    <artifactId>teda</artifactId>
    <version>1.0.0-RC2</version>
    <scope>test</scope>
</dependency>
```

Point Teda at a `DataSource` and a spreadsheet via the fluent
`TedaConfiguration` builder:

```java
import com.brielmayer.teda.Teda;
import com.brielmayer.teda.configuration.TedaConfiguration;
import com.brielmayer.teda.model.DocumentType;

TedaConfiguration configuration = TedaConfiguration.builder()
        .withDatabase(dataSource)
        .build();

new Teda(configuration)
        .execute("teda/STUDENT_TEST.xlsx", DocumentType.EXCEL);
```

A typical JUnit 5 test looks like this:

```java
class StudentTest {

    private DataSource dataSource; // e.g. an H2 in-memory DataSource

    @Test
    void loadAndVerifyStudents() {
        TedaConfiguration configuration = TedaConfiguration.builder()
                .withDatabase(dataSource)
                .build();

        new Teda(configuration)
                .execute("teda/STUDENT_TEST.xlsx", DocumentType.EXCEL);
    }
}
```

The call throws a `TedaException` with a detailed message if the actual data does
not match the expected data. Plug it straight into any test framework.

## How It Works

Every Teda run is driven by a spreadsheet with a **Cockpit** sheet that orchestrates
the run, plus additional sheets holding the actual data.

```
        ┌──────────────────────────────────────────────┐
        │  Spreadsheet (.xlsx / .ods)                  │
        │                                              │
        │  ┌──────────────┐   reads / orchestrates     │
        │  │  Cockpit     │───────────┐                │
        │  │  #Teda table │           │                │
        │  └──────────────┘           ▼                │
        │       │            ┌──────────────┐          │
        │       │            │ Data sheets  │          │
        │       │            │ #Table ...   │          │
        │       │            └──────────────┘          │
        └───────┼──────────────────────────────────────┘
                │
                ▼   for each Cockpit row, in order:
      ┌───────────────────────────────────────────────┐
      │  TRUNCATE  → empty a table                    │
      │  LOAD      → insert the sheet's rows          │
      │  EXECUTE   → trigger your process (ETL, proc) │
      │  TEST      → compare DB rows to expected rows │
      └───────────────────────────────────────────────┘
```

1. **Parse.** The spreadsheet is read into an in-memory model of sheets and tables.
2. **Cockpit.** The first table of the first sheet (`Cockpit`) is the control
   panel. Each of its rows lists actions to perform.
3. **Execute.** For each row, Teda runs the actions in the fixed order
   `TRUNCATE → LOAD → EXECUTE → TEST`, using the cell value as the argument
   (a table name, a sheet name, or a value passed to your handler).
4. **Compare.** `TEST` reads the real rows from the database and compares them,
   row by row, against the expected rows, after sorting both by their primary-key
   columns and normalizing types.

## The Spreadsheet Layout

### 1. The Cockpit sheet

The first sheet **must be named `Cockpit`** and contain a `#Teda` table. Its column
headers are the [actions](#actions-reference); each row's cells reference the sheet
or table the action applies to. Empty cells are skipped.

| #Teda    | *(scenario)* |           |                 |
|----------|--------------|-----------|-----------------|
| TRUNCATE | LOAD         | EXECUTE   | TEST            |
| STUDENT  | StudentInput | importJob | StudentExpected |

> The `#Teda` marker cell is followed by a free-text name in the cell to its right.
> The header row below defines which actions run; each data row is one scenario.

### 2. Data sheets

Any other sheet can hold one or more `#Table` blocks. A `#Table` marker is followed
by the **target database table name**, then a header row of column names, then the
data rows.

![A data sheet in Excel with a #Table STUDENT marker, header row of #ID/NAME/AGE/AVERAGE, and four data rows.](docs/images/data-sheet.png)

Sheet `StudentInput` (data to load):

| #Table | STUDENT |     |         |
|--------|---------|-----|---------|
| #id    | name    | age | average |
| 1      | Alice   | 21  | 1.75    |
| 2      | Bob     | 23  | 2.30    |

Sheet `StudentExpected` (expected result):

| #Table | STUDENT |     |         |
|--------|---------|-----|---------|
| #id    | name    | age | average |
| 1      | Alice   | 21  | 1.75    |
| 2      | Bob     | 23  | 2.30    |

**Primary keys** are marked by prefixing the column name with `#` (e.g. `#id`).
Teda sorts both expected and actual rows by these columns before comparing, so row
order in the spreadsheet and in the database does not matter.

**Schema-qualified tables** are supported: the table name after `#Table` (and the
one in the cockpit's `TRUNCATE` column) may be written as `SCHEMA.TABLE`, or as
`CATALOG.SCHEMA.TABLE` on SQL Server.

| #Table | REPORTING.STUDENT |     |         |
|--------|-------------------|-----|---------|
| #id    | name              | age | average |
| 1      | Alice             | 21  | 1.75    |

### 3. CSV directories

For CSV, each sheet lives in its own `.csv` file inside a directory. The file
name (without extension) becomes the sheet name, so `Cockpit.csv` is treated
as the sheet named `Cockpit`.

```
tests/loadStudents/
├── Cockpit.csv
├── StudentInput.csv
└── StudentExpected.csv
```

The `#Teda` / `#Table` marker sits in the first cell of the first row, with
the scenario or table name in the cell next to it. The header row follows
directly below, then the data rows, each aligned to the marker column.

`Cockpit.csv`:

```csv
#Teda,LoadTest
TRUNCATE,LOAD,EXECUTE,TEST
STUDENT,StudentInput,importJob,StudentExpected
```

`StudentInput.csv`:

```csv
#Table,STUDENT
#id,name,age,average
1,Alice,21,1.75
2,Bob,23,2.30
```

Point Teda at the directory:

```java
new Teda(configuration).execute("tests/loadStudents", DocumentType.CSV);
```

## Actions Reference

Each column header in the Cockpit's `#Teda` table is one of the following actions.
Within a row they always execute in this order:

| Action     | Cell value refers to  | Effect                                                                                                                        |
|------------|-----------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `TRUNCATE` | a database table name | Empties the table in the **test** database before loading.                                                                    |
| `LOAD`     | a sheet name          | Inserts every `#Table` block on that sheet into the **load** database.                                                        |
| `EXECUTE`  | any value             | Passes the value to your [`IExecutionHandler`](#custom-handlers), which typically triggers an ETL job, stored procedure, etc. |
| `TEST`     | a sheet name          | Reads each `#Table` block's table from the **test** database and asserts it equals the expected rows.                         |

## Type Handling

Different JDBC drivers return different Java types for the same SQL value. Teda
normalizes both the expected (spreadsheet) and actual (database) values before
comparing, so tests stay portable across databases:

- All integer and floating-point types (`Byte`, `Short`, `Integer`, `Long`,
  `Float`, `Double`, `BigInteger`) are compared as `BigDecimal`.
- `java.sql.Date` / `Time` / `Timestamp` become `LocalDate` / `LocalTime` /
  `LocalDateTime`.
- `Boolean` stays `Boolean`; `UUID` is compared as its `String` form.
- Spreadsheet text is auto-detected: `true`/`false` → boolean, numeric strings →
  `BigDecimal`, ISO date/time strings → the matching `java.time` type.

### NULL values

A cell containing `[NULL]` stands for a SQL `NULL`. It works in both directions:
in a `LOAD` sheet it inserts `NULL`, in a `TEST` sheet it asserts that the column
is `NULL`. The spelling is case-insensitive (`[null]`, `[Null]`), and surrounding
whitespace is ignored.

| #Table | STUDENT |        |         |
|--------|---------|--------|---------|
| #id    | name    | age    | average |
| 1      | Alice   | 21     | 1.75    |
| 2      | Bob     | [NULL] | [NULL]  |

An **empty cell** is *not* `NULL` — it means the empty string, and is inserted and
compared as such. The two stay distinguishable:

| Cell content | Loaded as      | Matches in a TEST sheet |
|--------------|----------------|-------------------------|
| `[NULL]`     | SQL `NULL`     | only `NULL`             |
| *(empty)*    | `''`           | only `''`               |

Two things to keep in mind:

- **Oracle stores the empty string as `NULL`.** There an empty cell and `[NULL]`
  end up identical in the database, so an expected `''` can never match. Use
  `[NULL]` on Oracle.
- **Primary key columns must not be `NULL`.** Teda sorts both sides by the `#`
  columns to line rows up, and a `NULL` key makes that impossible — it fails with
  `Primary key "..." contains null`.

A row whose cells are all empty still marks the end of a table. A row of `[NULL]`
cells does not — it is data.

## Load vs. Test Database

For ETL and migration testing you can load into one database and verify in another.
Provide two `DataSource`s plus an execution handler that triggers the process
between them:

```java
TedaConfiguration configuration = TedaConfiguration.builder()
        .withLoadDatabase(sourceDataSource)
        .withTestDatabase(targetDataSource)
        .withExecutionHandler(new MyEtlExecutionHandler())
        .build();

new Teda(configuration)
        .execute("teda/MIGRATION_TEST.xlsx", DocumentType.EXCEL);
```

Here `LOAD` writes to the **source** database, `EXECUTE` runs your migration, and
`TEST` verifies the rows that arrived in the **target** database. With
`.withDatabase(ds)`, load and test databases are the same.

## Custom Handlers

`TedaConfiguration` accepts custom implementations for all four action handlers.
Only override what you need; the defaults handle standard cases.

| Handler              | Default              | Override when                                        |
|----------------------|----------------------|------------------------------------------------------|
| `ITruncateHandler`   | `TruncateHandler`    | you need something other than `TRUNCATE TABLE ...`.  |
| `ILoadHandler`       | `LoadHandler`        | you want a custom insert strategy.                   |
| `IExecutionHandler`  | `LogExecutionHandler`| you want `EXECUTE` to actually trigger a process.    |
| `ITestHandler`       | `TestHandler`        | you want a different comparison strategy.            |

The most common override is `IExecutionHandler`. The default just logs the value;
implement your own to kick off the real work (an ETL job, a stored procedure, a
REST call, and so on):

```java
public class MyEtlExecutionHandler implements IExecutionHandler {
    @Override
    public void execute(String value) {
        // 'value' is the cell content from the EXECUTE column,
        // e.g. the name of the job to run
        myEtlEngine.run(value);
    }
}
```

Wire it up via the builder:

```java
TedaConfiguration configuration = TedaConfiguration.builder()
        .withDatabase(dataSource)
        .withExecutionHandler(new MyEtlExecutionHandler())
        .build();
```

## FAQ

**How are Excel formulas handled?**
Teda reads the **cached result** of the formula, not the formula text. This is what
Excel writes to the file the last time it saved. If a spreadsheet is generated
programmatically without ever being opened by Excel or LibreOffice, formula cells
may not have cached values; in that case Teda returns an empty string.

**Can I compare a subset of columns?**
Only columns that appear as headers in the expected `#Table` block are compared.
Columns that exist in the database but are not listed in the expectation are
ignored.

## Building from Source

Requirements: **JDK 11 or newer**.

```bash
# Compile and run the full test suite
mvn verify

# Install to your local Maven repository
mvn install
```

The test suite uses [Testcontainers](https://www.testcontainers.org/) to spin up
real MySQL, PostgreSQL, Oracle, SQL Server, and MariaDB instances, so a running
**Docker** daemon is required to execute the database integration tests. The H2
tests run in-memory without Docker.

---

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
