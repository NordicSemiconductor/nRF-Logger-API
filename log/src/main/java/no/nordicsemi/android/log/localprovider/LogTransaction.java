/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing permissions and limitations under the License
 */

package no.nordicsemi.android.log.localprovider;

import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A transaction for interacting with a articles provider. This is used to pass state around
 * throughout the operations comprising the transaction, including which databases the overall
 * transaction is involved in, and whether the operation being performed is a batch operation.
 */
/* package */class LogTransaction {

	/**
	 * Whether this transaction is encompassing a batch of operations.
	 * If we're in batch mode, transactional operations from non-batch callers are ignored.
	 */
	private final boolean mBatch;

	/**
	 * The list of databases that have been enlisted in this transaction.
	 * <p>
	 * Note, we insert elements to the head of the list, so that we endTransaction() in the
	 * reverse order.
	 */
	private final List<SQLiteDatabase> mDatabasesForTransaction;

	/**
	 * The mapping of tags to databases involved in this transaction.
	 */
	private final Map<String, SQLiteDatabase> mDatabaseTagMap;

	/**
	 * Whether any actual changes have been made successfully in this transaction.
	 */
	private boolean mIsDirty;

	/**
	 * Whether a yield operation failed with an exception. If this occurred, we may not have a
	 * lock on one of the databases that we started the transaction with (the yield code cleans
	 * that up itself), so we should do an extra check before ending transactions.
	 */
	private boolean mYieldFailed;

	/**
	 * Creates a new transaction object, optionally marked as a batch transaction.
	 *
	 * @param batch whether the transaction is in batch mode.
	 */
	LogTransaction(boolean batch) {
		mBatch = batch;
		mDatabasesForTransaction = new ArrayList<>();
		mDatabaseTagMap = new HashMap<>();
		mIsDirty = false;
	}

	boolean isBatch() {
		return mBatch;
	}

	boolean isDirty() {
		return mIsDirty;
	}

	void markDirty() {
		mIsDirty = true;
	}

	void markYieldFailed() {
		mYieldFailed = true;
	}

	/**
	 * If the given database has not already been enlisted in this transaction, adds it to our list
	 * of affected databases and starts a transaction on it. If we already have the given database
	 * in this transaction, this is a no-op.
	 *
	 * @param db  the database to start a transaction on, if necessary.
	 * @param tag a constant that can be used to retrieve the DB instance in this transaction.
	 */
	void startTransactionForDb(SQLiteDatabase db, String tag) {
		if (!hasDbInTransaction(tag)) {
			// Insert a new db into the head of the list, so that we'll endTransaction() in
			// the reverse order.
			mDatabasesForTransaction.add(0, db);
			mDatabaseTagMap.put(tag, db);
			db.beginTransaction();
		}
	}

	/**
	 * Returns whether DB corresponding to the given tag is currently enlisted in this transaction.
	 */
	private boolean hasDbInTransaction(String tag) {
		return mDatabaseTagMap.containsKey(tag);
	}

	/**
	 * Retrieves the database enlisted in the transaction corresponding to the given tag.
	 *
	 * @param tag the tag of the database to look up.
	 * @return The database corresponding to the tag, or null if no database with that tag
	 * has been enlisted in this transaction.
	 */
	@SuppressWarnings("SameParameterValue")
	SQLiteDatabase getDbForTag(String tag) {
		return mDatabaseTagMap.get(tag);
	}

	/**
	 * Removes the database corresponding to the given tag from this transaction. It is now the
	 * caller's responsibility to do whatever needs to happen with this database - it is no longer
	 * a part of this transaction.
	 *
	 * @param tag the tag of the database to remove.
	 * @return The database corresponding to the tag, or null if no database with that tag has
	 * been enlisted in this transaction.
	 */
	@SuppressWarnings("unused")
	public SQLiteDatabase removeDbForTag(String tag) {
		SQLiteDatabase db = mDatabaseTagMap.get(tag);
		mDatabaseTagMap.remove(tag);
		mDatabasesForTransaction.remove(db);
		return db;
	}

	/**
	 * Marks all active DB transactions as successful.
	 *
	 * @param callerIsBatch whether this is being performed in the context of a batch operation.
	 *                      If it is not, and the transaction is marked as batch, this call is
	 *                      a no-op.
	 */
	void markSuccessful(boolean callerIsBatch) {
		if (!mBatch || callerIsBatch) {
			for (SQLiteDatabase db : mDatabasesForTransaction) {
				db.setTransactionSuccessful();
			}
		}
	}

	/**
	 * Completes the transaction, ending the DB transactions for all associated databases.
	 *
	 * @param callerIsBatch whether this is being performed in the context of a batch operation.
	 *                      If it is not, and the transaction is marked as batch, this call is
	 *                      a no-op.
	 */
	void finish(boolean callerIsBatch) {
		if (!mBatch || callerIsBatch) {
			for (SQLiteDatabase db : mDatabasesForTransaction) {
				// If an exception was thrown while yielding, it's possible that we no longer have
				// a lock on this database, so we need to check before attempting to end its
				// transaction. Otherwise, we should always expect to be in a transaction (and will
				// throw an exception if this is not the case).
				if (mYieldFailed && !db.isDbLockedByCurrentThread()) {
					// We no longer hold the lock, so don't do anything with this database.
					continue;
				}
				db.endTransaction();
			}
			mDatabasesForTransaction.clear();
			mDatabaseTagMap.clear();
			mIsDirty = false;
		}
	}
}