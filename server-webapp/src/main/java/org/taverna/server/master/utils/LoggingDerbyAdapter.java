package org.taverna.server.master.utils;

import static org.apache.commons.logging.LogFactory.getLog;

import java.sql.DatabaseMetaData;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.datanucleus.store.rdbms.adapter.DerbyAdapter;
import org.datanucleus.store.rdbms.identifier.DatastoreIdentifier;
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
		log.info("Generated table definition\n" + statement);
		return statement;
	}

	@Override
	public String getCreateIndexStatement(Index index, IdentifierFactory factory) {
		String statement = super.getCreateIndexStatement(index, factory);
		log.info("Generated table index\n" + statement);
		return statement;
	}

	@Override
	public String getAddCandidateKeyStatement(CandidateKey ck,
			IdentifierFactory factory) {
		String statement = super.getAddCandidateKeyStatement(ck, factory);
		log.info("Generated candidate key\n" + statement);
		return statement;
	}

	@Override
	public String getAddPrimaryKeyStatement(PrimaryKey pk,
			IdentifierFactory factory) {
		String statement = super.getAddPrimaryKeyStatement(pk, factory);
		log.info("Generated primary key\n" + statement);
		return statement;
	}

	@Override
	public String getAddColumnStatement(Table table,
			Column col) {
		String statement = super.getAddColumnStatement(table, col);
		log.info("Generated extra column\n" + statement);
		return statement;
	}

	@Override
	public String getAddForeignKeyStatement(ForeignKey fk,
			IdentifierFactory factory) {
		String statement = super.getAddForeignKeyStatement(fk, factory);
		log.info("Generated foreign key\n" + statement);
		return statement;
	}

	@Override
	public String getCheckConstraintForValues(DatastoreIdentifier identifier,
			Object[] values, boolean nullable) {
		String constraint = super.getCheckConstraintForValues(identifier,
				values, nullable);
		log.info("Generated check constraint\n" + constraint);
		return constraint;
	}

	@Override
	public String getDeleteTableStatement(SQLTable tbl) {
		String statement = super.getDeleteTableStatement(tbl);
		log.info("Generated delete table\n" + statement);
		return statement;
	}

	@Override
	public String getDropTableStatement(Table table) {
		String statement = super.getDropTableStatement(table);
		log.info("Generated drop table\n" + statement);
		return statement;
	}

	@Override
	public String getDropViewStatement(ViewImpl view) {
		String statement = super.getDropViewStatement(view);
		log.info("Generated drop view\n" + statement);
		return statement;
	}
}
