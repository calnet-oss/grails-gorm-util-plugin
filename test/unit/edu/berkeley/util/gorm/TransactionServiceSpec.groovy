package edu.berkeley.util.gorm

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