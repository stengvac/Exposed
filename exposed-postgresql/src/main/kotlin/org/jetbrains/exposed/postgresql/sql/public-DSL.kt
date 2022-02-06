package org.jetbrains.exposed.postgresql.sql

import org.jetbrains.exposed.sql.FieldSet
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.DeleteStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

fun <T : Table> T.insert(body: PostgresqlInsertDSL<T>.(T) -> Unit): Int {
    val insertStatement = InsertStatement<Number>(this)

    val dsl = PostgresqlInsertDSL(this, insertStatement)
    body(dsl, this)

    insertStatement.registerPrepareSQLCallback(dsl.createOnConflictPrepareSQL())

    return insertStatement.execute(TransactionManager.current())!!
}

fun <T : Table> T.insertReturning(body: PostgresqlInsertReturningDSL<T>.(T) -> Unit): ResultRow {
    val insertStatement = InsertStatement<List<ResultRow>>(this)

    val dsl = PostgresqlInsertReturningDSL(this, insertStatement)
    body(dsl, this)

    insertStatement.registerPrepareSQLCallback(dsl.createOnConflictPrepareSQL())
    insertStatement.registerPrepareSQLCallback(dsl.createReturningPrepareSQLCustomizer())

    insertStatement.execute(TransactionManager.current())!!

    return insertStatement.resultedValues!!.single()
}


fun FieldSet.select(where: SqlExpressionBuilder.() -> Op<Boolean>): Query = select(where)
fun FieldSet.selectAll(): Query = selectAll()

fun <T : Table> T.update(body: PostgresqlUpdateWhereDSL<T, UpdateStatement>.() -> Unit): Int {
    val updateDsl = PostgresqlUpdateWhereDSL(this, UpdateStatement(this, where = null))
    body(updateDsl)

    if (updateDsl.updateStatement.where == null) {
        throw IllegalStateException("""
            Calling update without where clause. This exception try to avoid unwanted update of whole table.
            "In case of update all call updateAll.""".trimIndent()
        )
    }

    return updateDsl.updateStatement.execute(TransactionManager.current())!!
}

fun <T : Table> T.updateAll(body: PostgresqlUpdateDSL<T, UpdateStatement>.() -> Unit): Int {
    val updateDsl = PostgresqlUpdateDSL(this, UpdateStatement(this, where = null))
    body(updateDsl)

    return updateDsl.updateStatement.execute(TransactionManager.current())!!
}

fun <T: Table> T.updateReturning(body: PostgresqlUpdateReturningDSL<T>.() -> Unit): Iterator<ResultRow> {
    val updateDsl = PostgresqlUpdateReturningDSL(this)
    body(updateDsl)

    if (updateDsl.updateStatement.where == null) {
        throw IllegalStateException("""
            Calling updateReturning without where clause. This exception try to avoid unwanted update of whole table.
            "In case of update all call updateAllReturning.""".trimIndent()
        )
    }

    return updateDsl.updateStatement.execute(TransactionManager.current())!!
}

fun <T: Table> T.updateAllReturning(body: PostgresqlUpdateAllReturningDSL<T>.() -> Unit): Iterator<ResultRow> {
    val updateDsl = PostgresqlUpdateAllReturningDSL(this)
    body(updateDsl)

    return updateDsl.updateStatement.execute(TransactionManager.current())!!
}

fun Table.delete(ignoreErrors: Boolean = false, body: SqlExpressionBuilder.() -> Op<Boolean>): Int {
    return DeleteStatement.where(TransactionManager.current(), this, SqlExpressionBuilder.body(), ignoreErrors)
}

//fun Table.deleteReturning(ignoreErrors: Boolean = false, body: PostgresqlDeleteReturningDSL.() -> Unit): Iterator<ResultRow> {
//    val dsl = PostgresqlDeleteReturningDSL(this)
//    dsl.body()
//
//    val where = dsl.where ?: throw IllegalStateException("Where function has to be called or use deleteAll()")
//    val deleteStatement = DeleteStatement(this, where, ignoreErrors)
//
//    val exec = deleteStatement.execute(TransactionManager.current())
//
//    return deleteStatement
//}

fun Table.deleteAll() = DeleteStatement.all(TransactionManager.current(), this)
//fun Table.deleteAllReturning(): Iterator<ResultRow> = DeleteStatement.all(TransactionManager.current(), this)