package rpp.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

public class DataFrameSchema {

	/**
	 * 生成表的基本信息，文件对应的列分别为col_1,col_2，以次类推，在文件内容的最后四列添加文件的基本信息，对应的列名分别为：
	 * 
	 * @param dataLength 源数据字段的个数
	 * @param modifyTimeIndex 文件修改时间字段的位置，通常为最后一个字段，从1开始,如果没有此字段则该值=-1
	 * @param timeFieldIndex 源数据中表示时间的字段的位置，从1开始，如果没有此字段则该值=-1
	 * @return
	 */
	public static StructType getSchema(int dataLength,int modifyTimeIndex, int timeFieldIndex) {
		// Generate the schema based on the string of schema
		List<StructField> fields = new ArrayList<StructField>();
		for (int i = 1; i <= dataLength; i++) {
			if(timeFieldIndex!=-1&&i==timeFieldIndex){
				StructField field = DataTypes.createStructField("col_" + i, DataTypes.LongType, true);
				fields.add(field);
			}else if(modifyTimeIndex!=-1&&i==modifyTimeIndex){
				StructField field = DataTypes.createStructField("col_" + i, DataTypes.LongType, true);
				fields.add(field);
			}else{
				StructField field = DataTypes.createStructField("col_" + i, DataTypes.StringType, true);
				fields.add(field);				
			}
			
		}
		return DataTypes.createStructType(fields);
	}
}
