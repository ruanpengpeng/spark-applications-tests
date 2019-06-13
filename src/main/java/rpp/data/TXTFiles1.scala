package rpp.data

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types._
import rpp.controller.Calc
import org.apache.spark.storage.StorageLevel


object TXTFiles1 {
  val dataSeperator =0X0A
  def handleSourceFile2Hbase(ctx:SparkContext,
                             sqlContext:SQLContext,
                             inputPath:String,
                             SplitChar:Char,
                             dataLengthOld:Int,
                             sparkTableName:String,
                             executeSQLMap:scala.collection.mutable.Map[String,String],
                             hbaseConfig:String,
                             minPartitions:Int,
                             statisticMetadata:scala.collection.mutable.Map[String,String],
                             timeFormat:String,
                             timeFieldIndex:Int):Unit={
    val dataLength=dataLengthOld
    println("===input inputPath is : "+inputPath+" ===")
    val invalidAccum = ctx.accumulator(0L, "invalidRecords")
    val validAccum = ctx.accumulator(0L, "validRecords")
    val invalidTimeAccum =ctx.accumulator(0L, "invalidTimeFieldRecords")
    //将wholeTextFile生成的PairRDD操作返回Tuple，key是文件的修改时间，value是文件内容，最后生成RDD
    val dataSeperator = 0x0A

   val rdd=ctx.wholeTextFiles(inputPath, minPartitions).map(_._2).flatMap(x =>x.split(dataSeperator.toChar)).map(_.split("\\t")(1)).filter(x => {
      val predicate =(x.split("\\"+SplitChar,-1).length==dataLength)
      if(predicate) validAccum+=1L else invalidAccum+=1L
      predicate
      })
    //生成表的元数据信息,Specifying the Schema
    val schema = StructType(
      for(i <- 1 to dataLength) yield StructField("col_"+i,StringType,true)
    )
    val rowRDD =rdd.map(_.split(SplitChar)).map(x =>{
      Row(x:_*)
    })
    val dataFrame =sqlContext.createDataFrame(rowRDD, schema)
    //如果需要执行的SQL超过一条，那么为了提升性能需要缓存DataFrame
    if(executeSQLMap.size>1) dataFrame.persist(StorageLevel.MEMORY_AND_DISK_SER)
    dataFrame.registerTempTable(sparkTableName)
    println("===生成表完毕！！替换表名生成实际执行的SQL语句===")
    println("=== 开始执行SQL语句  ===")
    for(x <- executeSQLMap){
      System.out.println("== 目前执行的SQL语句为:" + x._2);
      val rows=sqlContext.sql(x._2).head(2)
      // 统计指标的类型名，分割后为一条SQL语句对应多个统计指标
      val statisticsNames: Array[String] =x._1.split(",")
      for(row <-rows){
        println("=== 统计指标的类型名的数量 : " + statisticsNames.length + " === SQL查询结果列的数量 : " + row.length + " ===")
        if(statisticsNames.length == row.length){
          for(i <- 0 to row.length-1)
            statisticMetadata.put(statisticsNames(i) , row.get(i).toString())
        }else{
          throw new Exception("=== 统计指标的类型名的数量与SQL查询结果列的数量不一致！===")
        }
      }
    }
    val totalRows= invalidAccum.value+validAccum.value
    println(s"==原始数据共有   ${totalRows}  行！！！字段数与预期不符的数据有    ${invalidAccum.value}   行")
    statisticMetadata.put("NumberOfvalidRows", validAccum.value+"")
    statisticMetadata.put("NumberOfInvalidRows", invalidAccum.value+"")
    statisticMetadata.put("NumberOfInvalidTimeFieldRows", invalidTimeAccum+"")


    Calc.save(hbaseConfig, statisticMetadata);
    ctx.stop();

  }
}
