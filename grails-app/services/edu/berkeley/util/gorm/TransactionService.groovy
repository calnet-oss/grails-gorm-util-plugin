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

import grails.transaction.Transactional
import org.grails.datastore.mapping.core.Session
import org.hibernate.SessionFactory
import org.springframework.transaction.annotation.Propagation

import javax.annotation.PostConstruct

@Transactional
class TransactionService {
    // manage transaction at the method level
    static transactional = false

    // injected if not a unit test
    SessionFactory sessionFactory

    SessionHolder sessionHolder

    /**
     * Optionally provided session.  If not set, sessionFactory must be.
     * This may be useful with DomainClassUnitTestMixin and
     * the simpleDatastore.currentSession that it provides.
     */
    TransactionService(Session session = null) {
        this.sessionHolder = new SessionHolder(gormSession: session)
    }

    @PostConstruct
    void init() {
        this.sessionHolder.hibernateSessionFactory = sessionFactory
    }

    void setSession(Session session) {
        sessionHolder.gormSession = session
    }

    /**
     * Execute the closure using a new transaction.  The transaction will be
     * committed upon succesful closure completion.  The transaction will be
     * rolled back on any Exception thrown and the exception will propagate.
     *
     * @param closure The closure to execute within a new transaction.
     *
     * @return The optional return value from the closure.
     */
    Object withTransaction(Closure closure) {
        assertCurrentSession()
        return doTransaction(closure)
    }

    /**
     * Execute the closure using a new transaction and clear the Hibernate
     * session after a successful commit of the transaction.  The
     * transaction will be committed upon succesful closure completion.  The
     * transaction will be rolled back on any Exception thrown and the
     * exception will propagate.  The Hibernate session is not cleared if
     * the closure threw an exception.
     *
     * @param closure The closure to execute within a new transaction.
     *
     * @return The optional return value from the closure.
     */
    Object withClearingTransaction(Closure closure) {
        def session = assertCurrentSession()
        Object result = withTransaction(closure)
        if (!currentSession) {
            throw new RuntimeException("Transaction has been committed but was unable to clear the session because getCurrentSession() returned null")
        }
        session = currentSession
        try {
            // clear the Hibernate session cache
            session.clear()
        }
        catch (Exception e) {
            throw new RuntimeException("Transaction has been committed but there was an exception clearing the session", e)
        }
        return result
    }

    private static class DoTransactionResult {
        def closureTransaction
        def closureResult
    }

    /**
     * Execute the closure within a new transaction.  Rollback for any
     * Exception.
     */
    protected Object doTransaction(Closure closure) {
        def originalTransaction = currentSession.transaction
        DoTransactionResult doTransactionResult = _doTransaction(closure, originalTransaction)
        // GORM Session doesn't support wasCommitted() on transaction object
        if (sessionHolder.gormSession == null) {
            // This is a "sanity check" to confirm that the transaction was
            // committed upon exit of _doTransaction().
            if (!doTransactionResult.closureTransaction.wasCommitted()) {
                throw new RuntimeException("Something went wrong with @Transactional behavior: transaction was not committed upon _doTransaction() exit")
            }
        }
        return doTransactionResult.closureResult
    }

    /**
     * Execute the closure within a new transaction.  Rollback for any
     * Exception.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception)
    private Object _doTransaction(Closure closure, def originalTransaction) {
        if (!closure)
            throw new RuntimeException("closure cannot be null")
        def currentTransaction = currentSession.transaction
        // This is just a "sanity check" to confirm @Transactional
        // annotation is doing what it's supposed to be doing in terms of
        // creating a new transaction.
        if (originalTransaction && originalTransaction.equals(currentTransaction)) {
            throw new RuntimeException("Something went wrong with @Transactional behavior: a new transaction was not established")
        }
        return new DoTransactionResult(
                closureTransaction: currentTransaction,
                closureResult: closure()
        )
    }

    def getCurrentSession() {
        return sessionHolder.currentSession
    }

    private def assertCurrentSession() {
        def session = currentSession
        if (!session)
            throw new RuntimeException("Injected Hibernate sessionFactory and passed-in GORM session cannot both be null: " + sessionFactory)
        return session
    }

    private static class SessionHolder {
        SessionFactory hibernateSessionFactory
        Session gormSession

        /**
         * @return A Hibernate or GORM Session object.  If a GORM session is set that will trump any Hibernate session from the injected sessionFactory.
         */
        def getCurrentSession() {
            return (gormSession ?: hibernateSessionFactory?.currentSession)
        }

    }
}
