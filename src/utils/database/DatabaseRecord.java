package utils.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * This class represents the record of a database table.
 * @author nilsw
 *
 */

public class DatabaseRecord {
	/**
	 * Stores the data values of the record.
	 */
	private HashMap<ColumnType, Object> values;
	
	/**
	 * Stores the type of the table from which the record was retrieved.
	 */
	private EvaluationTable evaluationTable;
	
	/**
	 * @param tableSchema Array of ColumnType that specifies the types of the columns of this record. Thereby, the ColumnType values have to be in the same order as the corresponding values in the second parameter.
	 * @param values Array of Object that contains the data values of the record.
	 */
	public DatabaseRecord(ColumnType[] tableSchema, Object[] values) {
		this.values = new HashMap<ColumnType, Object>();
		for(int i=0; i < tableSchema.length; i++) {
			this.values.put(tableSchema[i], values[i]);
		}
		
		EvaluationTable[] tables = EvaluationTable.values();
		ColumnType[] evaluationTableSchema = null;
		boolean allFound = true;
		boolean found = false;
		for(int i=0; i < tables.length; i++) {
			evaluationTableSchema = EvaluationTable.getTableSchema(tables[i]);
			allFound = true;
			
			for(ColumnType cType : tableSchema) {
				found = false;
				for(ColumnType eCType : evaluationTableSchema) {
					if(cType == eCType) {
						found = true;
						break;
					}
				}
				if(!found) {
					allFound = false;
					break;
				}
			}
			if(allFound) {
				this.evaluationTable = tables[i];
				break;
			}
		}
		
	}
	
	public DatabaseRecord(ColumnType[] tableSchema, Object[] values, EvaluationTable e) {
		this.values = new HashMap<ColumnType, Object>();
		for(int i=0; i < tableSchema.length; i++) {
			this.values.put(tableSchema[i], values[i]);
		}	
		
		this.evaluationTable = e;	
	}

	/**
	 * Can be used to create a record from the result of a database query.
	 * @param databaseResult ResultSet that corresponds to the result of a database query.
	 */
	public DatabaseRecord(ResultSet databaseResult) {
		//Initialize data value map
		this.values = new HashMap<ColumnType, Object>();
		
		//Determine the TableType of the table from which the ResultSet was retrieved
		EvaluationTable[] tables = EvaluationTable.values();
		boolean allFound = true;
		ColumnType[] tableSchema = null;
		for(int i=0; i < tables.length; i++) {
			tableSchema = EvaluationTable.getTableSchema(tables[i]);
			allFound = true;
			try {
				for(ColumnType cType : tableSchema) {
					databaseResult.findColumn(ColumnType.convertToStringName(cType));
				}
			}catch(SQLException e) {
				allFound = false;
			}
			if(allFound) {
				tableSchema = EvaluationTable.getTableSchema(tables[i]);
				this.evaluationTable = tables[i];
				break;
			}
		}
		
		//Retrieve the data values from the ResultSet instance
		try {
			ColumnType cType;
			for (int i = 1; i <= tableSchema.length; i++) {
				cType = tableSchema[i-1];
				switch(ColumnType.getJavaVarType(cType)) {
				case DOUBLE:
					this.values.put(cType, databaseResult.getDouble(i));
					break;
				case INT:
					this.values.put(cType, databaseResult.getInt(i));
					break;
				case LONG:
					this.values.put(cType, databaseResult.getLong(i));
					break;
				case TEXT:
					this.values.put(cType, databaseResult.getString(i));
					break;
				default:
					break;
				
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns a data value of the record as a long value.
	 * @param type Value of ColumnType that corresponds to the record column that is requested.
	 * @return long value of the data value that corresponds to the requested column.
	 */
	public long getLong(ColumnType type) {
		return ((Long)this.values.get(type)).longValue();
	}
	
	/**
	 * Returns a data value of the record as a double value.
	 * @param type Value of ColumnType that corresponds to the record column that is requested.
	 * @return double value of the data value that corresponds to the requested column.
	 */
	public double getDouble(ColumnType type) {
		return ((Double)this.values.get(type)).doubleValue();
	}
	
	/**
	 * Returns a data value of the record as an int value.
	 * @param type Value of ColumnType that corresponds to the record column that is requested.
	 * @return int value of the data value that corresponds to the requested column.
	 */
	public int getInt(ColumnType type) {
		return ((Integer)this.values.get(type)).intValue();
	}
	
	public String getString(ColumnType type) {
		return ((String)this.values.get(type));
	}

	/**
	 * Converts the record into a string representation. 3
	 * Thereby, the data values appear in the same order as they are stored in the table from which they were retrieved.
	 */
	public String toString() {
		ColumnType[] tableSchema = EvaluationTable.getTableSchema(this.evaluationTable);
		String result = "";
		for(int i=0; i < tableSchema.length; i++) {
			switch(ColumnType.getJavaVarType(tableSchema[i])) {
			case DOUBLE:
				result += this.getDouble(tableSchema[i]);
				break;
			case INT:
				result += this.getInt(tableSchema[i]);
				break;
			case LONG:
				result += this.getLong(tableSchema[i]);
				break;
			case TEXT:
				result += this.getString(tableSchema[i]);
			default:
				break;
			
			}
			if(i+1 != tableSchema.length) {
				result += ";";
			}
		}
		
		return result;
	}
	
	/**
	 * Returns the data value of a specific column of the record.
	 * @param type Value of ColumnType that corresponds to the requested column of the record.
	 * @return Instance of Object that represents the data value of the requested column.
	 */
	public Object getValueByColumnType(ColumnType type) {
		return this.values.get(type);
	}
	
	public EvaluationTable getTargetEvaluationTable() {
		return this.evaluationTable;
	}

}
