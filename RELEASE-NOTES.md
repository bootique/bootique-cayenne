## 3.0-M6

* #112 Ensure Bootique-provided version of JCache is used
* #113 Upgrade Cayenne to 4.2.1
* #114 Support for Cayenne 5.0-M1

## 3.0-M3

* #110 Deprecate Cayenne 4.1 integration modules
* #111 Optionally recognize `@CommitLog` annotations

## 3.0.M2

* #108 4.2 Extender methods for adding ValueObjectTypes and ExtendedTypes
* #109 Upgrade Cayenne to 4.2 GA

## 3.0.M1

* #93 JUnit 4 support deprecation
* #96 Stop supporting Cayenne 4.0
* #98 CayenneTester: allow to operate unattached 
* #99 Add a hook to run custom actions after Cayenne startup
* #100 CayenneTester: eager initialization breaks other test tools
* #101 Upgrade Cayenne to 4.2.RC1
* #104 Integrate "cayenne-commitlog" in 4.2 Bootique Cayenne module
* #105 API to control whether DataChannelSyncFilter is included or excluded from transactions
* #106 Upgrade 4.2 to 4.2.RC2

## 2.0

* #97 Task 68 ("maps" config must be a dictionary) - was not ported to Cayenne 4.1 and 4.2

## 2.0.RC1

* #94 Upgrade to Cayenne 4.2.B1
* #95 Upgrade Cayenne "4.1" modules to v4.1.1 of Cayenne

## 2.0.B1

* #84 CayenneTester.assertQueryCount(int)
* #85 Upgrade to Cayenne 4.2.M2
* #86 NPE: ObjectContext access causes premature faulting of JUnit 5 DataSource
* #88 Upgrade Cayenne 4.2 to M3
* #89 CayenneTester.allTables(..)
* #90 CayenneTester must init schema before scope start
* #91 Add "onInit" callback to CayenneTester 

## 2.0.M1

* #68 "maps" config must be a dictionary, not a list for extensibility
* #78 Support for JUnit 5
* #79 Support for Cayenne 4.2
* #80 Redesign JUnit 5 tests - CayenneTester
* #81 CayenneTester - support for commit counter
* #82 Upgrade to the final version of Cayenne 4.1

## 1.1

* #64 ServerRuntimeFactory hardcoded inside CayenneModule
* #65 Upgrade Cayenne 4.1 to 4.1.B2
* #72 DataMaps from YAML should be loaded together with other DataMaps 
* #73 Cayenne 4.1: contribute DataChannelSyncFilter, DataChannelQueryFilter
* #74 Upgrade Cayenne 4.1 integration to 4.1.RC1 
* #75 Upgrading Cayenne 4.0.x to 4.0.2
* #76 Upgrade Cayenne 4.1 integration to 4.1.RC2  

## 1.0

* #62 Upgrade to Cayenne 4.0.1
* #63 Upgrade bootique-cayenne41 to 4.1.B1

## 1.0.RC1

* #44 bootique-cayenne-test: test tables are not committed with rollbackOnReturn=true and defaultAutoCommit=false
* #52 Cleaning up APIs deprecated since <= 0.25 
* #53 Upgrade to Cayenne 4.0.RC1
* #54 Implicit config should not be loaded when there is an explicit Bootique config of maps
* #55 Support for Cayenne 4.1
* #59 CayenneTestDataManager - support for loading all entities as Tables 
* #60 Provide access to Cayenne ServerRuntime in CayenneTestDataManager
* #61 Upgrade to Cayenne 4.0 final

## 0.25

* #50 Java 9 support
* #51 Upgrade to bootique-modules-parent 0.8

## 0.24

* #45 Support for join tables in CayenneTestDataManager
* #46 Test API: smart DB cleanup - handle dependent tables
* #47 Test API: an option to clear Cayenne caches after deleting test data
* #48 CayenneTestModule - replace 'contributeSchemaListeners' with an extender
* #49 Upgrade to Cayenne 4.0.B2

## 0.20

* #39 Upgrade to BQ 0.23 and update test API to match the new Bootique test API
* #40 Upgrade to Cayenne 4.0.B1

## 0.19

* #29 bootique-cayenne-test: Derby schema generation requires quotationst com
* #35 Upgrade to bootique 0.22, other fresh dependencies, create extender
* #36 Upgrade to Cayenne 4.0.M5
* #37 Temp solution for the inability of Cayenne to invalidate non-Cayenne JCache caches

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
