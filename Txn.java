package com.aginity.amp.catalogservice.application.transaction;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.PersistenceException;
import java.sql.SQLException;

/**
 * User: rnestertsov
 * Date: 4/30/18 10:30 AM
 */
@Slf4j
public class Txn {

    private static final String SERIALIZATION_FAILURE = "40001";

    /**
     * execTx runs fn inside a transaction and retries it as needed.
     * On non-retryable failures, the transaction is aborted and rolled
     * back; on success, the transaction is committed.
     * There are cases where the state of a transaction is inherently ambiguous: if
     * we err on RELEASE with a communication error it's unclear if the transaction
     * has been committed or not (similar to erroring on COMMIT in other databases).
     * In that case, we return AmbiguousCommitException.
     * There are cases when restarting a transaction fails: we err on ROLLBACK
     * to the SAVEPOINT. In that case, we return a TxnRestartException.
     * <p>
     * For more information about CockroachDB's transaction model see
     * https://cockroachlabs.com/docs/stable/transactions.html.
     * <p>
     * NOTE: the supplied fn closure should not have external side
     * effects beyond changes to the database.
     */
    public static Object execTx(Session session, TxnFn<Object> fn) throws Throwable {
        Object ret;

        Transaction tx = session.beginTransaction();
        try {
            ret = execInTx(session, tx, fn);
            tx.commit();
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        return ret;
    }

    /**
     * execInTx runs fn inside tx which should already have begun.
     * *WARNING*: Do not execute any statements on the supplied tx before calling this function.
     * execInTx will only retry statements that are performed within the supplied
     * closure (fn). Any statements performed on the tx before execInTx is invoked will *not*
     * be re-run if the transaction needs to be retried.
     * <p>
     * fn is subject to the same restrictions as the fn passed to ExecuteTx.
     */
    private static Object execInTx(Session session, Transaction tx, TxnFn<Object> fn) throws Throwable {
        Object ret;

        // Specify that we intend to retry this txn in case of CockroachDB retryable
        // errors.
        try {
            session.createNativeQuery("SAVEPOINT cockroach_restart").executeUpdate();
        } catch (Exception e) {
            log.error("Error while executing savepoint.", e);
            throw e;
        }

        while (true) {
            boolean released = false;

            try {
                Object result = fn.call();

                // RELEASE acts like COMMIT in CockroachDB. We use it since it gives us an
                // opportunity to react to retryable errors, whereas tx.commit() doesn't.
                released = true;
                session.createNativeQuery("RELEASE SAVEPOINT cockroach_restart").executeUpdate();

                ret = result;
                break;
            } catch (PersistenceException e) {
                // We got an error; let's see if it's a retryable one and, if so, restart. We look
                // for the standard PG errcode SerializationFailureError:40001.
                Throwable cause = e.getCause();
                final boolean serializationFailureException = cause instanceof SQLException
                        && ((SQLException) cause).getSQLState().equals(SERIALIZATION_FAILURE);
                if (!serializationFailureException) {
                    log.info("------------------------------------------------------");
                    log.info("Non serialization exception", e);
                    log.info("Cause", cause);
                    log.info("Transaction status: {}", tx.getStatus());
                    log.info("Transaction getRollbackonly: {}", tx.getRollbackOnly());
                    log.info("------------------------------------------------------");
                }
                if (serializationFailureException) {
                    log.info("Serialization failure. Retrying transaction...");

                    try {
                        session.createNativeQuery("ROLLBACK TO SAVEPOINT cockroach_restart").executeUpdate();
                    } catch (Exception ex) {
                        throw new TxnRestartException(ex);
                    }
                } else if (released) {
                    throw new AmbiguousCommitException(e);
                } else {
                    throw e;
                }
            } catch (Exception e) {
                log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                log.info("Exception caught. Non hibernate", e);
                log.info("Transaction status: {}", tx.getStatus());
                log.info("Transaction getRollbackonly: {}", tx.getRollbackOnly());
                log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                throw e;
            }
        }

        return ret;
    }
}
