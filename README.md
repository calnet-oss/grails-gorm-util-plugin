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

Also supports `DomainClassUnitTestMixin` and
'getSimpleDatastore().currentSession' in unit tests.  See
[TransactionServiceSpec](test/unit/edu/berkeley/util/gorm/TransactionServiceSpec.groovy).

```
import edu.berkeley.util.gorm.test.TestPerson
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import spock.lang.Specification

@TestFor(TransactionService)
@TestMixin([DomainClassUnitTestMixin])
@Mock(TestPerson)
class TransactionServiceSpec extends Specification {
    void "test SimpleMapDatastore session and withClearingTransaction"() {
        given:
        assert service
        assert simpleDatastore.currentSession
        service.setSession(simpleDatastore.currentSession)
        assert service.currentSession
        service.withClearingTransaction {
            TestPerson person = new TestPerson(id: "1")
            person.save()
        }

        expect:
        TestPerson.list().size() > 0
    }
}
```
