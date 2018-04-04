/*
 * Copyright (c) 2016, Regents of the University of California and
 * contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.berkeley.util.gorm

import edu.berkeley.util.gorm.test.TestPerson
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
@Rollback
class TransactionServiceIntegrationSpec extends Specification {

    def cleanup() {
        TestPerson.withNewTransaction {
            TestPerson.list()*.delete()
        }
    }

    @Autowired
    TransactionService transactionService
    @Autowired
    SessionFactory sessionFactory

    void "test withClearingTransaction"() {
        expect: "to have a sessionFactory"
        sessionFactory

        and: "there should be no persons"
        TestPerson.count() == 0

        when:
        transactionService.withClearingTransaction {
            TestPerson person = new TestPerson(id: "1")
            person.save()
        }

        then:
        TestPerson.count == 1

    }

    void "test withClearingTransaction throwning an exception"() {
        expect: "to have a sessionFactory"
        sessionFactory

        and: "there should be no persons"
        TestPerson.count() == 0

        when: "the closure throws an exception"
        transactionService.withClearingTransaction {
            TestPerson person = new TestPerson(id: "1")
            person.save()
            throw new RuntimeException("Failed")
        }

        then: "the exception is thrown"
        def e = thrown(RuntimeException)
        e.message == "Failed"

        and: "There is still no TestPerson"
        TestPerson.count == 0
    }


}
