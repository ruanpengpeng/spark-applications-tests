package rpp.common;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;


/**
 * HBASE操作工具
 *
 * @author
 */

public class HbaseIOUtil {
    private static final Logger logger = LoggerFactory.getLogger(HbaseIOUtil.class);
    //static ExecutorService pool = Executors.newSingleThreadExecutor();
    //static ExecutorService pool = Executors.newFixedThreadPool(50);
    //初始化连接
    private static Configuration conf;
    private static Connection con;

    public static void init(String uri) {
        try {
            conf = HBaseConfiguration.create();
            //String hostsname = SysConfigHbase.getValue(uri, "username").trim();
            //String keporsPath = SysConfigHbase.getValue(uri, "keporsPath").trim();
            String hbase_zookeeper_quorum = SysConfigHbase.getValue(uri, "hbase.zookeeper.quorum").trim();
            String hbase_zookeeper_clientPort = SysConfigHbase.getValue(uri, "hbase.zookeeper.property.clientPort").trim();

            conf.set("hbase.zookeeper.quorum", hbase_zookeeper_quorum);
            conf.set("hbase.zookeeper.property.clientPort",hbase_zookeeper_clientPort);
            conf.setInt("hbase.client.retries.number", 1);
            conf.set("hbase.client.pause", "100");
            conf.setInt("zookeeper.recovery.retry", 1);
            conf.set("zookeeper.recovery.retry.intervalmill", "200");
            conf.set("hbase.client.ipc.pool.type", "RoundRobin");
            conf.set("hbase.client.ipc.pool.size", "200");
            //conf.set("hbase.security.authentication","kerberos");
            //conf.set("hadoop.security.authentication","Kerberos");

            //UserGroupInformation.setConfiguration(conf);
            //UserGroupInformation.loginUserFromKeytab(hostsname, keporsPath);

            con = ConnectionFactory.createConnection(conf);
        } catch (IOException e) {
            System.out.println("登录HBASE失败1");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("登录HBASE失败2");
            e.printStackTrace();
        }
    }


    /**
     * 获得链接
     *
     * @return
     */
    public static Connection getConnection() {
        try {
            if (con == null || con.isClosed()) {
                con = ConnectionFactory.createConnection(conf);
            }
        } catch (IOException e) {
            logger.error("HBase 建立链接失败 ", e);
        }
        return con;
    }

    /**
     * 关闭连接
     *
     * @throws IOException
     */
    public static void close() {
        if (con != null) {
            try {
                con.close();
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("close failure !", e);
            }
        }
    }

    public static Table getHTable(String namespace, String tablename) throws IOException {
        System.out.println("===getHTable..start===");
        Connection connection = getConnection();
        TableName tableName = TableName.valueOf(namespace, tablename);
        Table table = connection.getTable(tableName);
        System.out.println("===getHTable..end===");
        System.out.println("===tables value :" + table);
        return table;
    }

    //数据存储
    /**
     * 
     * @param tableName "表名"
     * @param rowKey	"行"
     * @param familyName "列簇"
     * @param columnName "列"
     * @param value "值"
     * @throws Exception
     */    
    public static void putData(String tableName, String rowKey, String familyName, String columnName, String value)
            throws Exception {
        Connection connection = getConnection();
        Table table = connection.getTable(TableName.valueOf(tableName));
        // 通过rowkey创建一个put对像
        Put put = new Put(Bytes.toBytes(rowKey));
        // 在put对像中设置列簇，列和值
        put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(columnName), Bytes.toBytes(value));
        table.put(put);
        table.close();
        //close();
    }

    
}
