package rpp.test

import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.SQLContext
import org.apache.spark.{Accumulator, SparkConf, SparkContext}
import org.apache.spark.sql.types._
import org.apache.spark.sql.Row
object test {
  def main(args: Array[String]): Unit = {
    val conf =new SparkConf().setAppName("test").setMaster("local")
    val sc=new SparkContext(conf)
    val sqlContext= new SQLContext(sc)
   // val s = "1|||"

    // s.split("\\|", -1).foreach(str => println(str))

    // StringUtils.split(s, '|').foreach( x => println(x))

//    println(StringUtils.split(s, '|').length)
    //val rdd=sc.wholeTextFiles("/testData/2000.txt",1)
    val validAccum: Accumulator[Long] = sc.accumulator(0L, "My Accumulator")
    val invalidAccum: Accumulator[Long] = sc.accumulator(0L, "My Accumulator")
    val SplitChar='|'
    /*val rdd=sc.wholeTextFiles("/testData/2000.txt", 1).map(_._2).flatMap(x =>x.split("\\n")).map(_.split("\\t")(1)).
      foreach(x => println(x.split("\\|",-1).length))*/

    val rdd=sc.wholeTextFiles("/testData/2000.txt", 1).map(_._2).flatMap(x =>x.split("\\n")).map(_.split("\\t")(1)).filter(x => {
      val predicate =(x.split("\\"+SplitChar,-1).length==41)
      if(predicate) validAccum+=1L else invalidAccum+=1L
      predicate
    })
    val schema = StructType(
      for(i <- 1 to 41) yield StructField("col_"+i,StringType,true)
    )
    val rowRDD =rdd.map(_.split(SplitChar)).map(x =>{
      Row(x:_*)
    })
    val dataFrame =sqlContext.createDataFrame(rowRDD, schema)
    dataFrame.registerTempTable("file")
    val rows=sqlContext.sql("select sum(case when col_1 IS NULL or col_1='' then 1 else 0 end) imsi from file").head(2)
    rows.foreach(x => println(x.get(0)))
    //rdd.map(_._2).flatMap(x =>x.split("\\n")).foreach(x => println(x.split("\\|").length))

  }

}
