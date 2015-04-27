Grails GORM Utility Plugin
==========================

```
def transactionService
...
transactionService.withClearingTransaction {
  ... do whatever you want here for a transaction ...
}
```

`withClearingTransaction()` will execute your closure in a new transaction,
commit that transaction and then clear the Hibernate session cache. 
Clearing this cache is useful when saving or updating large datasets.  The
cache can interfere with performance when dealing with large data sets.
