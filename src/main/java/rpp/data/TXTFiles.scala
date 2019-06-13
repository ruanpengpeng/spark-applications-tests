package rpp.data

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.spark.{Accumulator, SparkContext}
import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types._
import rpp.controller.Calc
import org.apache.spark.storage.StorageLevel


object TXTFiles {
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
  val dataLength= dataLengthOld+1
  println("===input inputPath is : "+inputPath+" ===")
  val invalidAccum: Accumulator[Long] = ctx.accumulator(0L, "invalidRecords")
  val validAccum = ctx.accumulator(0L, "validRecords")
  val invalidTimeAccum =ctx.accumulator(0L, "invalidTimeFieldRecords")
  //将wholeTextFile生成的PairRDD操作返回Tuple，key是文件的修改时间，value是文件内容，最后生成RDD
  val timeAndContentRDD=ctx.wholeTextFiles(inputPath, minPartitions).mapPartitions(x =>
    {
      val hdfsFileSystem = FileSystem.get(new Configuration())
      val timeAndContent:scala.collection.mutable.Map[String,String] = scala.collection.mutable.Map()
      while(x.hasNext){
        var element=x.next()
        var fileStatus=hdfsFileSystem.listStatus(new Path(element._1))
        var modification=0L
        for(status <- fileStatus){
          modification=status.getModificationTime
        }
        timeAndContent+=(modification+"" ->element._2)
      }
      timeAndContent.iterator
    })
   //对上一次RDD进行操作,并将tuple的内容进行按行切割，并在每条数据的后面加上时间字段
   val rdd=timeAndContentRDD.flatMap(x =>{
    val time=x._1;
    val content=x._2.split(dataSeperator.toChar)
    var contentNew:scala.collection.mutable.ListBuffer[String] =new scala.collection.mutable.ListBuffer()
    for(i <-content){
      contentNew.+=(SplitChar+i)
    }
    contentNew
    })
  	//------------------------2019-05-29-------------------------------------
   //对上面的RDD进行操作，对数据进行按字段切割，不满足指定要求的个数的数据进行过滤。
   val validLine=rdd.filter(x =>{
   val predicate:Boolean =(x.split(SplitChar).length==dataLength)
   if(predicate){
     validAccum+=1L
   }else{
     invalidAccum+=1L
   }
   predicate
   })
   //----------------------------------------2019-05-29---------------------------------------------------------
   //RDD进行操作
   
    val attributeRDD=validLine.map(x =>{  
     val dateFormat =new SimpleDateFormat(timeFormat,Locale.ENGLISH)
     val attributes:Array[String]=x.split(SplitChar)
     val objects:Array[Any]=Array[Any]()
     for(i <- 0 to x.length()){
       //如果到了日期这个字段
       if(i==(timeFieldIndex-1)){
         try{
           objects(i)=dateFormat.parse(attributes(i).trim()).getTime
         }catch{
           case ex:ParseException => objects(i) =0L;invalidTimeAccum.add(1L)
         }
         //如果到了末尾,末尾是文件的时间字段
       }else if(i==(dataLength-1)){
          objects(i) = attributes(i).trim().toLong
       }else{
         objects(i) = attributes(i).trim()
       }
     }
     objects.mkString("|")
   })
   //生成表的元数据信息,Specifying the Schema
   val schema = StructType(
       for(i <- 1 to dataLength)
         yield{
          if(i==timeFieldIndex||i==dataLength){
            StructField("col_"+i,LongType,true)
          }else {
            StructField("col_"+i,StringType,true)
          }
         }
       )
   val rowRDD =attributeRDD.map(_.split(SplitChar)).map(x =>{
     Row(x:_*)
   })
   val dataFrame =sqlContext.createDataFrame(rowRDD, schema)
   //如果需要执行的SQL超过一条，那么为了提升性能需要缓存DataFrame
   if(executeSQLMap.size>1) dataFrame.persist(StorageLevel.MEMORY_AND_DISK_SER)
   dataFrame.registerTempTable(sparkTableName)
   println("===生成表完毕！！替换表名生成实际执行的SQL语句===")
   println("=== 开始执行SQL语句  ===")
    dataFrame.foreach(x => println(x.get(1)))
   for(x <- executeSQLMap){
    System.out.println("== 目前执行的SQL语句为:" + x._2);
    val rows=sqlContext.sql(x._2).head(2)
    // 统计指标的类型名，分割后为一条SQL语句对应多个统计指标
    val statisticsNames=x._1.split(",")
    for(row <-rows){
      println("=== 统计指标的类型名的数量 : " + statisticsNames.length + " === SQL查询结果列的数量 : " + row.length + " ===")
      if(statisticsNames.length == row.length){
          statisticMetadata.++:(for(i <- 0 to row.length-1)
          yield (statisticsNames(i) -> row.get(i).toString()))          
      }else{
        throw new Exception("=== 统计指标的类型名的数量与SQL查询结果列的数量不一致！===")
      } 
    }
   }
  val totalRows= invalidAccum.value+validAccum.value
  printf("==原始数据共有   %ld   行！！！字段数与预期不符的数据有    %ld   行",totalRows,invalidAccum.value)
  statisticMetadata.+(("NumberOfInvalidRows", invalidAccum.value+""))
  statisticMetadata.+(("NumberOfInvalidRows", invalidAccum.value+""))
  statisticMetadata.+(("NumberOfInvalidTimeFieldRows", invalidTimeAccum+""))
  
  
	Calc.save(hbaseConfig, statisticMetadata);
	ctx.stop();

  }
}