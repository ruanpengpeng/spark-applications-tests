package rpp.test

import org.apache.spark.sql.{Row, RowFactory, SQLContext}
import org.apache.spark.sql.types.{LongType, StringType, StructField, StructType}
import org.apache.spark.{SparkConf, SparkContext}

object sparksqlTest {
  def main(args: Array[String]): Unit = {
    val conf =new SparkConf().setMaster("local").setAppName("testSparkSQl")
    val sc=new SparkContext(conf)
    val sqlContext=new SQLContext(sc)
    val data= sc.textFile("/testData/2000.txt")
    println(data.first())
    val schema: StructType = StructType(for(i <- 1 to 37)
        yield{
            StructField("col_"+i,StringType,true)
        }
    )
    val rowRDD =data.map(_.split("\\|")).filter(x => x.length==37).map(x =>{
      Row(x:_*)
    })
    val dataFrame =sqlContext.createDataFrame(rowRDD, schema)
    dataFrame.registerTempTable("dayFirst")
    val rows=sqlContext.sql("select sum(case when col_1 IS NULL or col_1='' then 1 else 0 end) imsi from dayFirst").head(2)
    val statisticMetadata:scala.collection.mutable.Map[String,String]=scala.collection.mutable.Map[String,String]("1"->"hao","2"->"不好")
    val statisticMetadata1:scala.collection.mutable.Map[String,String]=scala.collection.mutable.Map[String,String]("1"->"hao","2"->"不好")
    val statisticsNames: Array[String] =Array("imsi")
    for(row <-rows){
      for(i <- 0 to row.length-1)
        statisticMetadata.put(statisticsNames(i) , row.get(i).toString())
    }
    statisticMetadata.foreach(println)



  }
}
