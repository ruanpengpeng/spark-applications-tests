package rpp.common;


import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

public class MysqlIOUtil {
	/**
	 * 向mysql中的TS_INVEN_LOG_T表插入程序开始执行时间，标记程序开始执行
	 * @param inDate  执行程序的时间标记
	 * @param log_id  程序代码执行到此方法的当前时间为毫秒数
	 * @param inven_id  规则库的规则ID
	 * @param start  为程序行到此方法时的日期时间如：2018-07-06 11:18
	 * @throws Exception
	 */
	public static final String JOB_TALBE_NAME = "jobTableName";
	public static final String LOG_TALBE_NAME = "logTableName";
	
	public static void insertLog(String inDate, Long log_id, String inven_id, Date start,String mysqlConfPath) throws Exception {
		Connection con = getMySQLConnection(mysqlConfPath);
		String logTableName =  MysqlIOUtil.getMySQLTableName(mysqlConfPath,LOG_TALBE_NAME);
		String sql = "insert into "+logTableName+"(log_id,inven_id,inven_param,start_time) values(?,?,?,?)";
		PreparedStatement pstm = null;
		try {
			pstm = con.prepareStatement(sql);
			pstm.setLong(1, log_id);
			pstm.setInt(2, Integer.parseInt(inven_id));
			pstm.setString(3, inDate);
			pstm.setTimestamp(4, new Timestamp(start.getTime()));
			pstm.executeUpdate();
			con.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			pstm.close();
			con.close();
		}
	}

	public static void updateLog(Long log_id, Date end, String invenRes, String errInfo,String mysqlConfPath) throws Exception {
		Connection con = getMySQLConnection(mysqlConfPath);
		String logTableName =  MysqlIOUtil.getMySQLTableName(mysqlConfPath,LOG_TALBE_NAME);
		String sql = "update "+logTableName+" " + "set end_time = ?," + "inven_res = ?," + "fail_info = ? "
				+ "where log_id = ?";

		PreparedStatement pstm = null;
		try {
			pstm = con.prepareStatement(sql);
			pstm.setTimestamp(1, new Timestamp(end.getTime()));
			pstm.setString(2, invenRes);
			pstm.setString(3, errInfo);
			pstm.setLong(4, log_id);
			pstm.executeUpdate();
			con.commit();
			System.out.println("更新成功：" + log_id + ":" + invenRes);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			pstm.close();
			con.close();
		}

	}
	public static String getMySQLTableName(String mysqlConfPath,String tableName) throws Exception{
		Properties properties = new Properties();
		try {
			// 加载mysql配置文件
			properties.load(new FileInputStream(mysqlConfPath));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("mysql规则库配置文件读取失败");
		}
		String name = properties.getProperty(tableName);
		return name;
	}
	public static Connection getMySQLConnection(String mysqlConfPath) throws Exception {
		Properties properties = new Properties();
		try {
			// 加载mysql配置文件
			properties.load(new FileInputStream(mysqlConfPath));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("mysql规则库配置文件读取失败");
		}
		String driver = "com.mysql.jdbc.Driver"; // 驱动标识符
		String url = properties.getProperty("uri");
		String user = properties.getProperty("user"); // 数据库的用户名
		String password = properties.getProperty("password"); // 数据库的密码
		Connection con = null;
		try {
			Class.forName(driver);
			con = DriverManager.getConnection(url, user, password);
			con.setAutoCommit(false);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("mysql规则配置数据库连接失败:" + url + ":" + user);
		}
		return con;
	}
}

