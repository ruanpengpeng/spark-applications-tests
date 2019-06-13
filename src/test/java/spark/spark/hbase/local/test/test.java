package spark.spark.hbase.local.test;

import java.util.ArrayList;
import java.util.List;

import rpp.common.HdfsIOUtil;

public class test {	
	public static void main(String[] args) throws Exception {
		//List<String> allFileStatus = getAllFileStatus("2");
	}
	public static void getFileStatusRecursively(List<String> result) throws Exception {
				result.add("haha");
	}

	public static List<String> getAllFileStatus(String str) throws Exception{

		List<String> result = new ArrayList<String>();	
		getFileStatusRecursively(result);	
		System.out.println("=== get all file status finished! ===");
		return result;
	}			
	
}
