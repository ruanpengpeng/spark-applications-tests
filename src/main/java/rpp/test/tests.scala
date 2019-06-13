package rpp.test
import java.security.MessageDigest

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
//import spark.spark.hbase.local.test.io.MysqlIOUtil
//import spark.spark.hbase.local.test.io.HbaseIOUtil
import org.apache.hadoop.io.compress

object tests {
  def main(args:Array[String]){
    val conf =new SparkConf().setAppName("test").setMaster("local")
    val sc=new SparkContext(conf)
    val rdd: RDD[String] = sc.textFile("/testData/2000.txt")
    rdd.map(x => {
      val y= x.split("\\t")
      (y(0).toLong,y(1))
    }).saveAsSequenceFile("/testData/yasuo3.txt",codec = Some(classOf[com.hadoop.compression.lzo.LzopCodec]))
  /*val logFile = "test.txt"
  val conf =new SparkConf().setAppName("Simple Application").setMaster("local[2]")
  val sc = new SparkContext(conf)
/*  val map=Map(1->"1\n3\n",2 -> "2\n4\n")
  map.foreach(println)*/
  val numberAndContent=sc.makeRDD(Seq(1,2)).map(x => {(x,"1:2:3")})
  numberAndContent.flatMap(x => {
  x._2.split(':')
  })*/
  /*numberAndContent.foreach(println)
  numberAndContent.flatMapValues(x=>{
    x.split('\n')
  }).foreach(println(_))*/
  
  //val sqlContext =  org.apache.spark.sql.SQLContext(sc)
  //val rddFile=sc.textFile(logFile, 1)
  //rddFile.map(x =>x.split("\t")).map(x => x(1)).map(_.split('|')).foreach(x=>println(x.length))
  //将数据写入到Hbase和Mysql中
  //val date =new Date();
  //MysqlIOUtil.insertLog("1558403120854",1,"20190521",date,"D:\\Users\\apple\\Documents\\workspace-sts-3.9.2.RELEASE\\spark.hbase.local.test\\src\\main\\resources\\db.properties")
  //连接hbase
  //HbaseIOUtil.init("D:\\Users\\apple\\Documents\\workspace-sts-3.9.2.RELEASE\\spark.hbase.local.test\\src\\main\\resources\\hbase.config")
  //HbaseIOUtil.putData("t1", date.toString(), "f1", "a", "334455")
  //HbaseIOUtil.close()
  /*  val logData =sc.textFile(logFile, 2).cache()
  val numAs = logData.filter(line => line.contains("a")).count();
  val numBs = logData.filter(line =>line.contains("b")).count();
  printf("Lines with a: %s,Lines with b: %s",numAs,numBs)*/
  }
}