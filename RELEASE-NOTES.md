## 0.19

* #35 Upgrade to bootique 0.22, other fresh dependencies, create extender
* #36 Upgrade to Cayenne 4.0.M5

## 0.18

* #15 Pure Bootique bootstrap of Cayenne projects
* #23 Contributing Cayenne projects via DI
* #25 Upgrade for Cayenne 4.0.M4
* #26 Bad DataSource mapping results in an obscure Cayenne DI NPE
* #27 Unit tests: add a builder of Table objects based on DbEntity
* #28 Extend TestDataManager for Cayenne
* #30 Add a way to register a listener of schema creation event
* #31 New module: bootique-cayenne-jcache
* #32 bootique-cayenne-jcache: enable cache refresh on commit
* #33 Remove 'cayenne.config' configuration deprecated in 0.14 
* #34 Annotate module and factories with documentation

## 0.17

* #19 Upgrade to Bootique 0.20
* #20 bootique-cayenne-test

## 0.16

* #16 Support for contributing Cayenne modules
* #18 Move to io.bootique namespace 

## 0.15

* #12 Make project-less ServerRuntime possible
* #13 Use single bootique-jdbc datasource as the default for Cayenne
* #14 Upgrade to Bootique 0.18 

## 0.14

* #11 Exception when referencing CayenneModule by class

## 0.13

* #7 Support for listeners/filters contribution
* #8 Make Cayenne config "dataSourceName" property optional - let Cayenne config its own DataSource
* #9 CayenneModule should be immutable ; configuration should happen via a builder

## 0.12:

* #5 Upgrade Bootique to 0.12 and bootique-jdbc - to 0.9
* #6 Upgrade Cayenne to official 4.0.M3
 
## 0.11:

* #4 ServerRuntimeFactory: 'createSchema' - a config flag to enable schema auto-creation

## 0.10:

* #3 Adding Shutdown hook

## 0.9:

* #1 Support for named Cayenne stacks
* #2 Upgrade to Cayenne 4.0.M3.fb19854
