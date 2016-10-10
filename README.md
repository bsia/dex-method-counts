# dex-method-counts

Simple tool to output per-package method counts in an Android DEX executable grouped by package, to aid in getting under the 65,536 referenced method limit. More details are [in this blog post](http://blog.persistent.info/2014/05/per-package-method-counts-for-androids.html).

It can also be used programmatically and is available from maven central

groupId `com.github.spyhunter99`
artifactId `dex-method-counts-lib`
version `2.1`
type `jar`

# Deltas from original source https://github.com/mihaip/dex-method-counts

* experimental support for plain old Jar files
* business logic is seperate from the output formats
* HTML and formatted text file outputs (plus the existing standard out)
* Field counts and method counts are included all the time
* programmatic access to enable you to handle the output anyway you want

# Run it from the command line

To run it with Ant:

    $ ant jar
    $ ./dex-method-counts path/to/App.apk # or .zip or .dex or directory

or with Gradle:

    $ ./gradlew assemble
    $ ./dex-method-counts path/to/App.apk # or .zip or .dex or directory

on Windows:

    $ gradlew assemble
    $ dex-method-counts.bat path\to\App.apk

You'll see output of the form:

    Read in 65490 method IDs.
    <root>: 65490
        : 3
        android: 6837
            accessibilityservice: 6
            bluetooth: 2
            content: 248
                pm: 22
                res: 45
            ...
        com: 53881
            adjust: 283
                sdk: 283
            codebutler: 65
                android_websockets: 65
            ...
        Overall method count: 65490

Supported options are:

* `--disableStdout`: Prevents the text version from printing to standard out.
* `--enableFileOut`: Outputs both textual and html reports to disk along with a aggregate report
* `--include-classes`: Treat classes as packages and provide per-class method counts. One use-case is for protocol buffers where all generated code in a package ends up in a single class.
* `--package-filter=...`: Only consider methods whose fully qualified name starts with this prefix.
* `--max-depth=...`: Limit how far into package paths (or inner classes, with `--include-classes`) counts should be reported for.
* `--filter=[all|defined_only|referenced_only]`: Whether to count all methods (the default), just those defined in the input file, or just those that are referenced in it. Note that referenced methods count against the 64K method limit too.

The DEX file parsing is based on the `dexdeps` tool from
[the Android source tree](https://android.googlesource.com/platform/dalvik.git/+/master/tools/dexdeps/).


# Programmatic usage

Use cases:

* custom outputs
* want to run it as a build plugin (maven, gradle, etc)

Example

````
   com.github.spyhunter99.dex.Main dexcount = new com.github.spyhunter99.dex.Main();
    dexcount.setOutputDirectory(new File("."));
    dexcount.enableStdOut(false);
    dexcount.enableFileOutput(true);
    int status = dexcount.run(new String[]{"path/to/my.apk"});
    //at this point, your reports should be generated
    //if status is non-zero, there was some kind of error
    
    List<CountData> results = dexcount.getData();
    //do extra processing here on 'results'
    
````

# To prepare a release

## Update the version

Edit `gradle.properties` and set the new `pom.version` number.

## Set your credentials in `local.properties`

## Build and publish
Run the following

````
./gradlew install -Pprofile=javadoc,sources
./gradlew publishArtifacts
````

If successful, commit the version changes and tag. Then update the version number and commit.
