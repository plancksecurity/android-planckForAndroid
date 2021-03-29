package com.fsck.k9.search

import com.fsck.k9.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object SqlQueryBuilderInvoker {

    @JvmStatic
    fun buildWhereClause(account: Account,
                         node: ConditionsTreeNode?,
                         query: StringBuilder,
                         selectionArgs: MutableList<String>) {
        runBlocking {
            buildWhereClauseInBackground(account, node, query, selectionArgs)
        }
    }

    private suspend fun buildWhereClauseInBackground(
            account: Account,
            node: ConditionsTreeNode?,
            query: StringBuilder,
            selectionArgs: MutableList<String>) = withContext(Dispatchers.IO){
        SqlQueryBuilder.buildWhereClause(account, node, query, selectionArgs)
    }

    @JvmStatic
    fun addPrefixToSelection(columnNames: Array<String>, prefix: String, selection: String): String? {
        return SqlQueryBuilder.addPrefixToSelection(columnNames, prefix, selection)
    }
}