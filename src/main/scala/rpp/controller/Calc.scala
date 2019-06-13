package rpp.controller

import java.util.Date

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import rpp.common.JsonUtils
import rpp.common.MysqlIOUtil
import rpp.common.PartitionUtil
import rpp.data.TXTFiles1
import rpp.common.SysConfigHbase
import java.security.MessageDigest

import com.alibaba.fastjson.JSONArray
import rpp.common.HbaseIOUtil

import scala.collection.JavaConversions._
import scala.collection.mutable

object Calc extends Serializable {
  private val serialVersionUID =1L
  val DIR ="dir"
  val DATALENGTH = "fieldNum"
  val SOURCE_FILE_TYPE="sourceFileType"
  val LZO_SEQUENCE_FILE = "LZODEFLATESEQUENCE"
  val TXT_FILE = "TXT"
  val TABLE_SCHEMA = "tableSchema"
  val HIVE_TABLE = "hiveTable"
  val SPECIAL_FILE = "specialFile"

  /**
	 * 默认使用 | 分隔符 ascII码
	 */
  var SplitChar='|'
  val CALC = "calc"
  var inputPath="0"

  /**
    * 计算的入口函数
    * @param args 有三个参数，第一个为执行程序的时间标记，第二个参为mysql配置文件路径
    *            ,每三个参数是存放数据持久化hbase的配置信息文件
    * @throws Exception
    */
  @throws[Exception]
  def main(args: Array[String]): Unit = {
    val inDate = args(0)
    val hbaseConfig = args(1)
    val mysqlConfPath = args(2)
    //yarn队列 内存总数除以vcore的数量取整数 以G为单位
    val memoryDivideVcore = args(3).toInt
    val jsons = JsonUtils.getRule(inDate, mysqlConfPath)
    val logLevel = args(4)
    var ind = 0
    while ( {
      ind < jsons.size
    }) {
      val logId = System.currentTimeMillis
      val inven_id = jsons.getJSONObject(ind).getString("inven_id")
      val ruleString = jsons.getJSONObject(ind).getString("inven_json")
      try { // 向mysql中的TS_INVEN_LOG_T表插入程序开始执行时间，标记程序开始执行
        MysqlIOUtil.insertLog(inDate, logId, inven_id, new Date, mysqlConfPath)
        run(inDate, hbaseConfig, ruleString, memoryDivideVcore, logLevel)
      } catch {
        case e: Exception =>
          e.printStackTrace()
          MysqlIOUtil.updateLog(logId, new Date, "否", "FAILED", mysqlConfPath)
      }
      MysqlIOUtil.updateLog(logId, new Date, "是", "SUCCEEDED", mysqlConfPath)

      {
        ind += 1; ind - 1
      }
    }
  }
  def run(inDate:String,hbaseConfig:String,ruleString:String,memoryDivideVcore:Int,logLevel:String){
   //将string转换成json对象并返回
   val json=JsonUtils.getTest(ruleString)
   SplitChar=JsonUtils.getChar(json)
   inputPath = JsonUtils.getDir(json.getString(DIR), inDate)
   val dataLength =Integer.parseInt(json.getString(DATALENGTH))
   //输入数据的文件类型，不同的类型需要不同的处理方式
   val sourceType = json.getString(SOURCE_FILE_TYPE)
   // viewName spark SQL 中的表名
   val sparkTableName = "TABLE"+ System.currentTimeMillis
   val executeSQLMap =JsonUtils.getSQLMap(json,sparkTableName)
   val timeFormat=json.getString("timeFormat")
   val timeFieldIndex=json.getIntValue("timeFieldIndex")
   //根据输入路径的数据规模，自动选择对应区间的partition数量
   val statisticMetadata: mutable.Map[String, String] = FileMetadata.statisticMetadata(json, inputPath, sourceType)
   val minPartitions =PartitionUtil.adjustPartitions(statisticMetadata,memoryDivideVcore)
   
   val sparkConf =new SparkConf().setAppName("SparkSQL-DataVerification_SCALA").setMaster("local")
   val ctx =new SparkContext(sparkConf)
   val sqlContext = new org.apache.spark.sql.SQLContext(ctx)
   ctx.setLogLevel(logLevel)
   val hadoopConfiguration = ctx.hadoopConfiguration
   //使hadoop可以迭代读取
   hadoopConfiguration.set("mapreduce.input.fileinputformat.input.dir.recursive", "true")
   hadoopConfiguration.set("mapreduce.input.pathFilter.class","rpp.filter.EmptyOrMismatchTypeFileFilter")
   
    sourceType match{
   /* case LZO_SEQUENCE_FILE => LzoDeflateSequenceFiles.handleSourceFile2HBase(ctx, sqlContext,
						inputPath, SplitChar.toChar, dataLength, sparkTableName, executeSQLMap, 
						hbaseConfig, statisticMetadata, timeFormat, timeFieldIndex);*/
    case TXT_FILE  => TXTFiles1.handleSourceFile2Hbase(ctx, sqlContext,
						inputPath, SplitChar.toChar, dataLength, sparkTableName, executeSQLMap, 
						hbaseConfig, minPartitions, statisticMetadata, timeFormat, timeFieldIndex)
    case _ =>  TXTFiles1.handleSourceFile2Hbase(ctx, sqlContext,
						inputPath, SplitChar.toChar, dataLength, sparkTableName, executeSQLMap, 
						hbaseConfig, minPartitions, statisticMetadata, timeFormat, timeFieldIndex)
   }
    println("计算完毕")
  }

  /**
	 * 计算结果持久化，目前有hbase的kerberos认证不通过的问题,待解决
	 *
	 */
  def save(uri:String,hbaseData:scala.collection.mutable.Map[String,String]):Unit={
    println("=== get ready to save data into hbase ===")
    for(name <- hbaseData){
      printf("===%s:%s===",name._1,name._2)
    }
    val tableName = SysConfigHbase.getValue(uri, "tablename").trim()
		val familyName = SysConfigHbase.getValue(uri, "familyName").trim()
		val rowkey = MD5(inputPath + System.currentTimeMillis())
		HbaseIOUtil.init(uri)
		for(data <- hbaseData){
		  HbaseIOUtil.putData(tableName, rowkey, familyName, data._1, data._2)
		}
		HbaseIOUtil.close()
  }
  
  /**
	 * 对给定的字段串生成32位的MD5加密字符串
	 * 
	 *
	 *
	 */
  private def MD5(sourceStr:String):String={
  var result = ""
  try{
    val md =MessageDigest.getInstance("MD5")
    md.update(sourceStr.getBytes)
    val b: Array[Byte] =md.digest()
    val buf = new StringBuffer("")
    for (offset <- 0 to b.length-1){
      var i:Int = b(offset)
      if(i<0) i += 256
      if(i<16) buf.append("0")
      buf.append(i.toHexString)
    }
    result = buf.toString()
  }catch{
    case e:Exception => println(e)
  }
  result
  }
}