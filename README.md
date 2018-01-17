CloseIT SAS reader - PDI plugin
=====

## Description
CloseIT SAS reader is a PDI input step plugin for reading data from SAS files (sas7bdat) based on the [Parso library](https://github.com/epam/parso). It was created as a feature rich substitution for default SAS reader step.

### Features
* <b>Supports compressed SAS data sets</b> - default SAS reader step is unable to read compressed SAS data sets. CloseIT SAS reader is able to read them thanks to underlying Parso library.
* <b>Column order independence</b> - plugin uses defined SAS column names to identify fields in input rows, so any column order change on input won't have impact on your transformation.
* <b>Optional columns (1.2.0-SNAPSHOT)</b> - missing columns in SAS data sets are reported so you know the input isn't correct. Columns can be marked as optional so they skip the presence check.
* <b>Column renaming</b> - the definition of SAS data set column names and output column names is separate so you can rename then just in one step.
* <b>Output data type change</b> - the step can convert SAS datatypes to desired output format in best possible way.
* <b>Intensive step checks</b> - PDI transformation verification function will execute several step checks and you will be informed about possible problems.

* <b>Your own cool feature</b> - do you need a feature which is missing now? Get in touch with us. Implement a feature, do a pull request. We would like to make the best ETL SAS reader together with FOSS community.

## Installation

1. use [Pentaho Marketplace](http://www.pentaho.com/marketplace/)
2. manually:
* compile project with <i>mvn package</i>
* extract the <i>target/pdi-sas-reader.zip</i> into your <i>.../data-integration/plugins/</i> , make sure that both jars was extracted (sas plugin, parso library)

## Support

Start new ticket on GitHub or write an email to our [address](mailto:contact@closeit.cz).
