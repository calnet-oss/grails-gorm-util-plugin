package edu.berkeley.util.gorm

import grails.transaction.Transactional
import org.hibernate.SessionFactory
import org.springframework.transaction.annotation.Propagation
import org.hibernate.Transaction

@Transactional
class TransactionService {
    // manage transaction at the method level
    static transactional = false

    // injected
    SessionFactory sessionFactory

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
        if (!sessionFactory)
            throw new RuntimeException("sessionFactory cannot be null")
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
        if (!sessionFactory)
            throw new RuntimeException("sessionFactory cannot be null")
        Object result = withTransaction(closure)
        if (!sessionFactory.currentSession) {
            throw new RuntimeException("Transaction has been committed but was unable to clear the Hibernate session because sessionFactory.getCurrentSession() returned null")
        }
        try {
            // clear the Hibernate session cache
            sessionFactory.currentSession.clear()
        }
        catch (Exception e) {
            throw new RuntimeException("Transaction has been committed but there was an exception clearing the Hibernate session", e)
        }
        return result
    }

    private static class DoTransactionResult {
        Transaction closureTransaction
        Object closureResult
    }

    /**
     * Execute the closure within a new transaction.  Rollback for any
     * Exception.
     */
    protected Object doTransaction(Closure closure) {
        Transaction originalTransaction = sessionFactory.currentSession.transaction
        DoTransactionResult doTransactionResult = _doTransaction(closure, originalTransaction)
        // This is a "sanity check" to confirm that the transaction was
        // committed upon exit of _doTransaction().
        if (!doTransactionResult.closureTransaction.wasCommitted()) {
            throw new RuntimeException("Something went wrong with @Transactional behavior: transaction was not committed upon _doTransaction() exit")
        }
        return doTransactionResult.closureResult
    }

    /**
     * Execute the closure within a new transaction.  Rollback for any
     * Exception.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception)
    private Object _doTransaction(Closure closure, Transaction originalTransaction) {
        if (!closure)
            throw new RuntimeException("closure cannot be null")
        Transaction currentTransaction = sessionFactory.currentSession.transaction
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
}
