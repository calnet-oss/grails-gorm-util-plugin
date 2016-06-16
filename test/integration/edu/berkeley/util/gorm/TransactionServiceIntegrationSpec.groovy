package edu.berkeley.util.gorm

import edu.berkeley.util.gorm.test.TestPerson
import grails.test.spock.IntegrationSpec
import org.hibernate.SessionFactory

class TransactionServiceIntegrationSpec extends IntegrationSpec {

    static transactional = false

    TransactionService transactionService
    SessionFactory sessionFactory

    void "test withClearingTransaction"() {
        given:
        assert sessionFactory
        transactionService.withClearingTransaction {
            TestPerson person = new TestPerson(id: "1")
            person.save()
        }

        expect:
        TestPerson.list().size() > 0
    }
}
