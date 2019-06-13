package rpp.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;


public class JsonUtils {
	/**
	 * @param inDate  执行程序的时间，是执行程序时输入的参数
	 * @return 返回JSON集合，该集合是由JSON表和程序执行日志表关联查询出来的结果存储的
	 * @throws Exception
	 */
	public static JSONArray getRule(String inDate,String mysqlConfPath) throws Exception {
		String jobTableName = MysqlIOUtil.getMySQLTableName(mysqlConfPath,MysqlIOUtil.JOB_TALBE_NAME);
		String logTableName =  MysqlIOUtil.getMySQLTableName(mysqlConfPath,MysqlIOUtil.LOG_TALBE_NAME);
		String sql = "select a.inven_id,a.inven_json from "+jobTableName+" a left join (select * from "+logTableName+" where inven_param = ?) b on a.inven_id = b.inven_id where (b.inven_res is null or b.inven_res = '否')";
		Connection con = MysqlIOUtil.getMySQLConnection(mysqlConfPath);
		PreparedStatement pstm = null;
		ResultSet rs = null;
		JSONArray jsons = new JSONArray();
		try {
			pstm = con.prepareStatement(sql);
			pstm.setString(1, inDate);
			rs = pstm.executeQuery();
			while (rs.next()) {
				Integer inven_id = rs.getInt("inven_id");
				String inven_json = rs.getString("inven_json");
				JSONObject json = new JSONObject();
				json.put("inven_id", inven_id);
				json.put("inven_json", inven_json);
				jsons.add(json);
			}
		} catch (SQLException e) {
			System.out.println("===Get Json From MySQL table "+jobTableName+" ERROR! ===");
			e.printStackTrace();
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			// 关闭执行通道
			if (pstm != null) {
				try {
					pstm.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			// 关闭连接通道
			try {
				if (con != null && (!con.isClosed())) {
					try {
						con.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return jsons;
	}
	public static JSONObject getTest(String jsonString) throws Exception {
		return JSONObject.parseObject(jsonString);
	}
	/**
	 * 将16进制转换成char类型
	 * @param json
	 * @return
	 * @throws Exception
	 */
	public static char getChar(JSONObject json) throws Exception{
		return (char) Integer.parseInt(json.getString("splitChar").replaceAll("^0[x|X]", ""), 16);
	}
	/**
	 * 根据配置信息获取文件的路径，路径中可以带有一个宏，为${INDATE}
	 *
	 * @param path
	 * @return
	 */
	public static String getDir(String path,String inDate) {
		String path_ =StringUtils.replace(path, "${INDATE}", inDate);
		return  path_;
	}
	/**
	 * 生成每个指标对应的SQL语句列表
	 * 
	 * @param json
	 *
	 *            SparkSQL中通过DataFrame建立的表名
	 * @return 指标类型名字与SQL语句的集合
	 */
	public static Map<String,String> getSQLMap(JSONObject json,String sparkTableName){
	
	Map<String,String> map =new HashMap<String,String>();
	JSONArray jsonArray = json.getJSONObject("calc").getJSONArray("target");
	for(int i=0;i<jsonArray.size();i++) {
		JSONObject sqlJson=jsonArray.getJSONObject(i);
		String sqlName = sqlJson.getString("name");
		String sqlContent =sqlJson.getString("value");
		String newSQLContent =sqlContent.replace("SQL-TABLE-NAME", sparkTableName);
		System.out.println("=== old SQL Changed ===");
		map.put(sqlName, newSQLContent);
	}
	return map;
		
	}
}
