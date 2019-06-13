package rpp.controller

import rpp.common.HdfsIOUtil
import org.apache.hadoop.fs.Path
import com.alibaba.fastjson.JSONObject
import scala.collection.mutable.HashMap
import org.apache.hadoop.fs.FileStatus
import scala.collection.JavaConversions._ 
import rpp.filter.EmptyOrMismatchTypeFileFilter



object FileMetadata {
  val FILE_NAME_REGEX = "fileRegex";
  val LZO_DEFLATE_SUFFIX = ".lzo_deflate";
  val TXT_SUFFIX = ".txt";
  /**
	 * 通过HDFS client获取空文件数量和不符合文件名正则的文件数量及其他文件元数据信息
	 * @param json
	 * @param inputPath
	 * @param sourceType
	 * @return
	 * @throws Exception
	 */
  def statisticMetadata( json:JSONObject,inputPath:String,sourceType:String ):scala.collection.mutable.Map[String,String] = {
  val allFileStatus = HdfsIOUtil.getAllFileStatus(new Path(inputPath))
  val jsonPatterns=json.getJSONArray(FILE_NAME_REGEX)
  val  fileNameRegex = new HashMap[String,String]()
  val matchNum = new HashMap[String,Long]()
  for(i <- 0 to jsonPatterns.size()-1){
    println(i)
    val patternObject=jsonPatterns.getJSONObject(i)
    val regexName = patternObject.getString("name")
    val regexContent = patternObject.getString("value")
    fileNameRegex.put(regexName, regexContent)
    matchNum.put(regexName, 0L)
  }
  val inputFileNum =allFileStatus.size().toLong
  //统计空文件个数
  var emptyFileNum =0L
  //统计不匹配文件个数
  var mismatchTypeFileNum =0L
  //统计输入的大小
  var inputSize =0L
  //统计实际处理文件的大小
  var handleFileSize=0L
  val fileSuffix= sourceType match{
    case Calc.LZO_SEQUENCE_FILE => LZO_DEFLATE_SUFFIX 
    case Calc.TXT_FILE => TXT_SUFFIX
     //默认文件后缀为空串，这样无论是什么文件名，在判断是否以空串结尾时都返回true，也就是不管文件名以何结尾均为符合要求的文件类型
    case _ => ""
  }
  for( stat <- allFileStatus){
   val fileSize = stat.getLen
   val filePath = stat.getPath
   val fileName = filePath.getName;
   if(fileSize==0){
     emptyFileNum +=1L
     EmptyOrMismatchTypeFileFilter.emptyFiles.add(filePath.toString())
     printf("=== file: %s ==is empty file===",filePath.toString())
   }else{
     inputSize+=fileSize
   }
   if(fileName.endsWith(fileSuffix)){
     handleFileSize +=fileSize;
   }else{
     mismatchTypeFileNum +=1L
     EmptyOrMismatchTypeFileFilter.mismatchTypeFiles.add(filePath.toString());
     printf("=== file :%s == is not match type: %s ===",filePath.toString(),fileSuffix)
   }
   for(regex <-fileNameRegex.entrySet()){
     val regexName = regex.getKey
     val regexCont =regex.getValue.r
     val matcher=regexCont.pattern.matcher(fileName)
     if(matcher.matches()){
       matchNum.put(regexName, matchNum.get(regexName).get+1L)
     }else{
       printf("=== file : %s == mismatch fileRegex : %s ===",filePath.toString(),regexCont.pattern)
     }
   }
  }
  HdfsIOUtil.closeHDFSFileSystem();
  val statisticResult =new HashMap[String,String]()
	statisticResult.put("InputFileNumber", inputFileNum+"");
	statisticResult.put("EmptyFileNumber", emptyFileNum+"");
	statisticResult.put("MismatchTypeFileNumber", mismatchTypeFileNum+"");
	statisticResult.put("TotalInputFileSize", inputSize+"");
	statisticResult.put("HandledFileSize", handleFileSize+"");
	for(regexName <- matchNum.keySet){
	  statisticResult.put(regexName+"_MatchNumber", matchNum.get(regexName).get+"")
	}
	for(name <- statisticResult.keySet){
	  println("=== "+name+" : "+statisticResult.get(name)+" ===")
	}
  statisticResult
  }
  
}