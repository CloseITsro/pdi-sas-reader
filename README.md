CloseIT SAS reader - PDI plugin
=====

## Description
CloseIT SAS reader is a PDI step plugin for reading data from SAS files (sas7bdat), based on the [Parso library](https://github.com/epam/parso) .

### Features
* <b>support compressed SAS files</b>
* <b>column order independence</b> - plugin use inserted SAS column names to identify fields in input rows, so any column order change on input won't have impact on your transformation
* <b>column renaming</b> - you can rename original SAS column names just in one step, so you do not have to use other step for this
* <b>output data type change</b> - the step can convert SAS datatypes to desired output format in best possible way
* <b>intensive step checks</b> - PDI transformation verification function will execute several step checks, you will be informed about possible problems and about defined SAS file.

## Installation

1. use [Pentaho Marketplace](http://www.pentaho.com/marketplace/)
2. manually:
* compile project with <i>mvn package</i>
* extract the <i>target/pdi-sas-reader.zip</i> into your <i>.../data-integration/plugins/</i> , make sure that both jars was extracted (sas plugin, parso library)

## Support

Start new ticket on GitHub or write an email to our [address](mailto:contact@closeit.cz).