package org.taverna.server.master.utils;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static org.apache.commons.logging.LogFactory.getLog;

import java.sql.DatabaseMetaData;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.datanucleus.store.rdbms.adapter.DerbyAdapter;
import org.datanucleus.store.rdbms.identifier.IdentifierFactory;
import org.datanucleus.store.rdbms.key.CandidateKey;
import org.datanucleus.store.rdbms.key.ForeignKey;
import org.datanucleus.store.rdbms.key.Index;
import org.datanucleus.store.rdbms.key.PrimaryKey;
import org.datanucleus.store.rdbms.sql.SQLTable;
import org.datanucleus.store.rdbms.table.Column;
import org.datanucleus.store.rdbms.table.Table;
import org.datanucleus.store.rdbms.table.TableImpl;
import org.datanucleus.store.rdbms.table.ViewImpl;

/**
 * Evil hack to allow logging of the DDL spat out to Derby.
 * 
 * @author Donal Fellows
 */
public class LoggingDerbyAdapter extends DerbyAdapter {
	Log log = getLog("Taverna.Server.SQL");

	private StringBuilder ddl = new StringBuilder();
	private volatile long timeout;
	private Thread timer;

	private synchronized void logDDL() {
		if (ddl.length() > 0) {
			log.info("Data definition language:\n" + ddl);
			ddl.setLength(0);
		}
		timer = null;
	}

	private synchronized void doLog(String item) {
		ddl.append(item);
		if (!item.endsWith("\n"))
			ddl.append('\n');
		timeout = currentTimeMillis() + 5000;
		if (timer == null)
			timer = new OneShotThread("DDL logger timeout", new Runnable() {
				@Override
				public void run() {
					try {
						while (timeout > currentTimeMillis())
							sleep(1000);
					} catch (InterruptedException e) {
						// Ignore
					}
					logDDL();
				}
			});
	}

	/**
	 * Creates an Apache Derby adapter based on the given metadata which logs
	 * the DDL it creates.
	 */
	public LoggingDerbyAdapter(DatabaseMetaData metadata) {
		super(metadata);
	}

	@Override
	public String getCreateTableStatement(TableImpl table, Column[] columns,
			Properties props, IdentifierFactory factory) {
		String statement = super.getCreateTableStatement(table, columns, props,
				factory);
		doLog(statement);
		return statement;
	}

	@Override
	public String getCreateIndexStatement(Index index, IdentifierFactory factory) {
		String statement = super.getCreateIndexStatement(index, factory);
		doLog(statement);
		return statement;
	}

	@Override
	public String getAddCandidateKeyStatement(CandidateKey ck,
			IdentifierFactory factory) {
		String statement = super.getAddCandidateKeyStatement(ck, factory);
		doLog(statement);
		return statement;
	}

	@Override
	public String getAddPrimaryKeyStatement(PrimaryKey pk,
			IdentifierFactory factory) {
		String statement = super.getAddPrimaryKeyStatement(pk, factory);
		doLog(statement);
		return statement;
	}

	@Override
	public String getAddColumnStatement(Table table, Column col) {
		String statement = super.getAddColumnStatement(table, col);
		doLog(statement);
		return statement;
	}

	@Override
	public String getAddForeignKeyStatement(ForeignKey fk,
			IdentifierFactory factory) {
		String statement = super.getAddForeignKeyStatement(fk, factory);
		doLog(statement);
		return statement;
	}

	@Override
	public String getDeleteTableStatement(SQLTable tbl) {
		String statement = super.getDeleteTableStatement(tbl);
		doLog(statement);
		return statement;
	}

	@Override
	public String getDropTableStatement(Table table) {
		String statement = super.getDropTableStatement(table);
		doLog(statement);
		return statement;
	}

	@Override
	public String getDropViewStatement(ViewImpl view) {
		String statement = super.getDropViewStatement(view);
		doLog(statement);
		return statement;
	}
}
