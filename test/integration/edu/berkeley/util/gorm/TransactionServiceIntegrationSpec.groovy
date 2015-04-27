package edu.berkeley.util.gorm

import grails.test.spock.IntegrationSpec
import edu.berkeley.util.gorm.test.TestPerson

class TransactionServiceIntegrationSpec extends IntegrationSpec {

    static transactional = false

    TransactionService transactionService

    void "test withClearingTransaction"() {
        given:
            transactionService.withClearingTransaction {
                TestPerson person = new TestPerson(id: "1")
                person.save()
            }

        expect:
            TestPerson.list().size() > 0
    }
}
