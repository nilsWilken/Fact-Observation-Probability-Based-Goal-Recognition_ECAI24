package utils.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;


/**
 * This class provides an interface that can be used to store relevant simulation data in a SQLite database.
 * @author nilsw
 *
 */
public class SQLiteDBHandler {

	/**
	 * Connection to the SQLite database.
	 */
	private Connection dbConnection;
	
	/**
	 * Map of insert statements for all tables in the database.
	 */
	private HashMap<String, PreparedStatement> insertStatements;
	
	/**
	 * Map of select statements for all tables in the database.
	 */
	private HashMap<String, PreparedStatement> selectAllStatements;
	
	/**
	 * Specifies the format in which dates are formatted for the output from and input to the database.
	 */
	private SimpleDateFormat format;
	
	private static Map<String, SQLiteDBHandler> instances = new HashMap<String, SQLiteDBHandler>();
	
	
	public static SQLiteDBHandler getInstance(String databaseName) {
		if(SQLiteDBHandler.instances.get(databaseName) == null) {
			SQLiteDBHandler.instances.put(databaseName, new SQLiteDBHandler(databaseName));
		}
		return SQLiteDBHandler.instances.get(databaseName);
	}

	/**
	 * Connects to an existing database or creates a new database.
	 * @param databaseName Name of the database to which the handler should connect or, in the case that no database with this name exists, the name of
	 * the newly created database.
	 */
	private SQLiteDBHandler(String databaseName) {
		String url = "jdbc:sqlite:" + databaseName;
		this.format = new SimpleDateFormat("dd_MM_yyyy");
		this.format.setTimeZone(TimeZone.getTimeZone("GMT"));
//		this.createTablesCalled = false;
		
		try {
			//Establish connection to the database.
			this.dbConnection = DriverManager.getConnection(url);
			if (this.dbConnection != null) {
				DatabaseMetaData meta = this.dbConnection.getMetaData();
				System.out.println("The driver name is " + meta.getDriverName());
				System.out.println("A new database has been created.");

				//Initialize statement lists
				this.insertStatements = new HashMap<String, PreparedStatement>();
				this.selectAllStatements = new HashMap<String, PreparedStatement>();
				
				//Create prepared statements for all tables that already exist in the database
				List<String> tableList = new ArrayList<String>();;
				for(String name : this.getTableCatalog()) {
					tableList.add(name);
				}
				
				for (String key : tableList) {
					this.updatePreparedStatements(key);
				}
				
				//Turn autocommit function off
				this.dbConnection.setAutoCommit(false);

			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public synchronized void createRecordTable(String name, EvaluationTable evaluationTable) {
		name = SQLiteDBHandler.convertTableName(name, evaluationTable);
		System.out.println("Insert table " + name);
		try {
			//Create SQL queries
			Statement st = dbConnection.createStatement();
			String sql1 = "drop table if exists " + name + ";";
			String sql2 = "create table " + name + SQLiteDBHandler.createTableSchema(evaluationTable) + ";";
			
			//Execute queries on the database
			st.executeUpdate(sql1);
			st.executeUpdate(sql2);

			st.close();

			//If the createRecordTables() method was called, the prepared statements are updated within that method
			this.updatePreparedStatements(name);

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	

	/**
	 * Inserts a record into a table of the database.
	 * @param tableName Name of the table in which the record should be inserted.
	 * @param record Instance of DatabaseRecord that corresponds to the record that should be inserted.
	 */
	public synchronized void insertRecord(String tableName, DatabaseRecord record) {
		
		EvaluationTable targetEvaluationTable = record.getTargetEvaluationTable();
		
		tableName = SQLiteDBHandler.convertTableName(tableName, targetEvaluationTable);

		PreparedStatement insertStatement = insertStatements.get(tableName);
				
		ColumnType[] tableSchema = EvaluationTable.getTableSchema(targetEvaluationTable);
		
		if (insertStatement != null) {
			try {
				ColumnType cType;
				for(int i=1; i <= tableSchema.length; i++) {
					cType = tableSchema[i-1];
					this.setValueInPreparedStatement(insertStatement, cType, record.getValueByColumnType(cType), i);
				}
				insertStatement.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
	
	}
	
	/**
	 * Perform commit on the database.
	 */
	public synchronized void commit() {
		try {
			dbConnection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Perform roll back on the database.
	 */
	public void rollBack() {
		try {
			dbConnection.rollback();
		}catch(SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Retrieves a list of names of all currently existing tables in the database.
	 * @return Returns an instance of ArrayList that contains the names of all tables that currently exist in the database.
	 */
	public synchronized ArrayList<String> getTableCatalog() {
		ArrayList<String> tableCatalog = new ArrayList<String>();

		try {
			ResultSet objects = this.executeQuery("select * from sqlite_master;");

			while (objects.next()) {
				tableCatalog.add(objects.getString((2)));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return tableCatalog;
	}

	public synchronized ArrayList<DatabaseRecord> getResultTable(EvaluationTable evaluationTable) {
		PreparedStatement selectAll = selectAllStatements.get(EvaluationTable.getTableName(evaluationTable));

		if (selectAll == null) {
			return null;
		}

		try {
			ResultSet allEntries = selectAll.executeQuery();

			return this.convertResultSet(allEntries);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public synchronized List<DatabaseRecord> getResultTable(String tableName, EvaluationTable evaluationTable) {
		tableName = SQLiteDBHandler.convertTableName(tableName, evaluationTable);
		PreparedStatement selectAll = this.selectAllStatements.get(tableName);
		
		if(selectAll != null) {
			try {
				ResultSet allEntries = selectAll.executeQuery();
				
				return this.convertResultSet(allEntries);
			}catch(SQLException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}

	public synchronized ArrayList<DatabaseRecord> getPartialResultTable(EvaluationTable evaluationTable, ArrayList<ColumnType> columns) {
		String cols = "(";
		for (int i = 0; i < columns.size(); i++) {
			cols += ColumnType.convertToStringName(columns.get(i));
			if (i + 1 != columns.size()) {
				cols += ",";
			}
		}
		cols += ")";

		ResultSet entries = this.executeQuery("select " + cols + " from " + this.createUnionTableQuery(EvaluationTable.getTableName(evaluationTable)) + ";");

		return this.convertResultSet(entries);
	}
	
	
	public synchronized List<DatabaseRecord> getPartialResultTable(String tableName, EvaluationTable evaluationTable, List<ColumnType> columns) {
		tableName = SQLiteDBHandler.convertTableName(tableName, evaluationTable);
		String cols = "(";
		for(int i=0; i < columns.size(); i++) {
			cols += ColumnType.convertToStringName(columns.get(i));
			if(i + 1 != columns.size()) {
				cols += ",";
			}
		}
		cols += ")";
		
		ResultSet entries = this.executeQuery("select " + cols + " from " + tableName + ";");
		
		if(entries != null) {
			return this.convertResultSet(entries);
		}
		
		return null;
	}
	
	
	public double getStatistic(EvaluationTable evaluationTable, ColumnType column, StatisticType statistic) {
		ResultSet value = this.executeQuery("select " + StatisticType.getSqLiteName(statistic) + "("
				+ ColumnType.convertToStringName(column) + ") from " + this.createUnionTableQuery(EvaluationTable.getTableName(evaluationTable)) + ";");
		double result = 0.0;

		try {
			value.next();
			result = value.getDouble(1);
			value.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return result;
	}
	
	public int getRowCount(EvaluationTable evaluationTable) {
		ResultSet rowCount = this.executeQuery("select count(*) from " + this.createUnionTableQuery(EvaluationTable.getTableName(evaluationTable)) + ";");
		int count = 0;
		
		try {
			rowCount.next();
			count = rowCount.getInt(1);
			rowCount.close();
		}catch(SQLException e) {
			e.printStackTrace();
		}
		
		return count;
	}
	
	/**
	 * Executes a custom sql query on the database.
	 * @param query String that contains the custom sql query.
	 * @return List of records that the database result of the query contains.
	 */
	public List<DatabaseRecord> executeCustomQuery(String query) {
		return this.convertResultSet(this.executeQuery(query));
	}
	
	
	/**
	 * Creates a union of all tables that exist for an external table name.
	 * @param tablePrefix External table name.
	 * @return Union of all tables that exist for the external table name as sql query.
	 */
	private String createUnionTableQuery(String tablePrefix) {
		ArrayList<String> tableNames = this.getTableCatalog();
		StringBuffer result = new StringBuffer();
		result.append("(");
		
		String cName;
		int counter = 0;
		for(int i=0; i < tableNames.size(); i++) {
			cName = tableNames.get(i);
			if(cName.contains(tablePrefix)) {
				if(counter != 0) {
					result.append(" union ");
				}
				result.append("select * from " + cName);
				counter++;
			}
			
		}
		result.append(")");
		
		return result.toString();
	}

	/**
	 * Executes a sql query on the database.
	 * @param sqlQuery Sql query that is executed as string representation.
	 * @return Database result for the executed query.
	 */
	private ResultSet executeQuery(String sqlQuery) {
		try {
			Statement stm = dbConnection.createStatement();
			ResultSet result = stm.executeQuery(sqlQuery);
			return result;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Updates the maps of prepared statements for a specified database table.
	 * @param tableName Name of the table for which the prepared statements are updated.
	 */
	private void updatePreparedStatements(String tableName) {
		EvaluationTable evaluationTable = EvaluationTable.parseFromString(tableName);
		
		if (insertStatements.get(tableName) == null) {
			this.prepareInsertStatement(tableName, evaluationTable);
		}
		if (selectAllStatements.get(tableName) == null) {
			this.prepareSelectAllStatement(tableName);
		}
	}
	
	private void updateInsertStatements() {		
		for(String name : this.getTableCatalog()) {
			this.prepareInsertStatement(name, EvaluationTable.parseFromString(name));
		}
	}

	private void prepareInsertStatement(String tableName, EvaluationTable evaluationTable) {
		ArrayList<String> tableNames = new ArrayList<String>();
		ColumnType[] tableSchema = EvaluationTable.getTableSchema(evaluationTable);
		
		tableNames.add(tableName);
		
		for (String name : tableNames) {
			String first = " (";
			String second = " (";

			for (int i = 0; i < tableSchema.length; i++) {
				first += ColumnType.convertToStringName(tableSchema[i]);
				second += "?";

				if (i + 1 != tableSchema.length) {
					first += ",";
					second += ",";
				}
			}
			first += ") ";
			second += ")";

			String sql = "insert into " + name + first + "values" + second + ";";

			try {
				insertStatements.put(name, dbConnection.prepareStatement(sql));
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void prepareSelectAllStatement(String tableName) {
		String sql = "select * from " + tableName + " ;";
		try {
			selectAllStatements.put(tableName, dbConnection.prepareStatement(sql));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Converts a database result into a list of DatabaseRecord instances.
	 * @param entries Database result that is converted.
	 * @return List of DatabaseRecord instances that represent all records that are contained in the passed database result.
	 */
	private ArrayList<DatabaseRecord> convertResultSet(ResultSet entries) {
		ArrayList<DatabaseRecord> result = new ArrayList<DatabaseRecord>();
		try {
			while (entries.next()) {
				result.add(new DatabaseRecord(entries));
			}
			entries.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Sets the variable values in an instance of PreparedStatement.
	 * @param statement Prepared statement for which the variable values are set.
	 * @param cType Value of ColumnType that indicates the column type of the variable value that is set.
	 * @param value Actual value that is set in the prepared statement.
	 * @param index Indicates the index of the variable value that is set in the prepared statement.
	 */
	private void setValueInPreparedStatement(PreparedStatement statement, ColumnType cType, Object value, int index) {
		if(statement != null) {
			try {
				switch(ColumnType.getJavaVarType(cType)) {
				case DOUBLE:
					statement.setDouble(index, (Double)value);
					break;
				case INT:
					statement.setInt(index, (Integer)value);
					break;
				case LONG:
					statement.setLong(index, (Long)value);
					break;
				case TEXT:
					statement.setString(index, (String)value);
					break;
				default:
					break;
				
				}
			}catch(SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Creates the table schema as sql representation for a value of TableType.
	 * @param evaluationTable Value of TableType that indicates the type schema that is created.
	 * @return Database table schema in sql respresentation.
	 */
	private static String createTableSchema(EvaluationTable evaluationTable) {
		String schema = " (";
		
		ColumnType[] tableSchema = EvaluationTable.getTableSchema(evaluationTable);

		for (int i = 0; i < tableSchema.length; i++) {
			schema += ColumnType.convertToStringName(tableSchema[i]) + " "
					+ ColumnType.getDatabaseVarType(tableSchema[i]);
			if (i + 1 != tableSchema.length) {
				schema += ",";
			}
		}
		schema += ")";

		return schema;
	}
	
	private static String convertTableName(String tableName, EvaluationTable evaluationTable) {
		return tableName + "_" + EvaluationTable.getTableName(evaluationTable);
	}
	

}
