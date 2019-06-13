package rpp.common;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
/**
 * 使用Hadoop的FileSystem把数据写入到HDFS
 */
public class HdfsIOUtil {

    private static Logger logger = LoggerFactory.getLogger(HdfsIOUtil.class);
    private static Configuration conf = new Configuration();
    private static FSDataOutputStream writer = null;
    private static FileSystem fs = null;
    
    public static FileSystem getHDFSFileSystem() throws Exception{
    	if(null==fs) fs = FileSystem.get(conf);
    	return fs;
    }
    public static void closeHDFSFileSystem() throws Exception{
    	if(null != fs){
    		fs.close();
    		fs = null;
    	}
    }
	public static void getFileStatusRecursively(FileSystem fs, Path path, List<FileStatus> result) throws Exception {
		FileStatus[] stats = fs.listStatus(path);
		for (FileStatus stat : stats) {
			if (stat.isDirectory()) {
				getFileStatusRecursively(fs, stat.getPath(), result);
			} else {
				result.add(stat);
			}
		}
	}

    public static List<FileStatus> getAllFileStatus(Path path) throws Exception{
    	FileSystem fSys = getHDFSFileSystem();
    	List<FileStatus> result = new ArrayList<FileStatus>();
    	FileStatus[] listStatus = fSys.listStatus(path);
    	for(FileStatus status : listStatus){
    		if(status.isDirectory()){
    			getFileStatusRecursively(fSys, status.getPath(), result);
    		}else{
    			result.add(status);
    		}
    	}
    	System.out.println("=== get all file status finished! ===");
    	return result;
    }
    
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void copyMerge(String folder, String file) {

        Path src = new Path(folder);
        Path dst = new Path(file);

        try {
            FileUtil.copyMerge(src.getFileSystem(conf), src,dst.getFileSystem(conf), dst, true, conf, null);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //在hdfs的目标位置新建一个文件，得到一个输出流
    public static void mkdir(String path)  {

        FileSystem fs = null;
        try {
            fs = FileSystem.get(conf);
            if(!fs.exists(new Path(path))) {
                fs.mkdirs(new Path(path));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    //在hdfs的目标位置新建一个文件，得到一个输出流
    public static void openHdfsFile(String path) throws Exception {

        FileSystem fs = FileSystem.get(conf);
        Path parentDir = new Path(path.substring(1,path.lastIndexOf("/")));
        if(!fs.exists(parentDir)) {
            fs.mkdirs(parentDir);
        }
        //writer = new BufferedWriter(new OutputStreamWriter(fs.create(new Path(path))));

        //fs.create(new Path(path));
        //writer = fs.append(new Path(path));
        writer = fs.create(new Path(path),true,4096000);

        if(null!=writer){
            logger.info("[HdfsOperate]>> initialize writer succeed!");
        }
    }

    //往hdfs文件中写入数据
    public static void writeString(String line) {
        try {
            if(org.apache.commons.lang.StringUtils.isNotEmpty(line)) {
                writer.writeBytes(line + "\n");
                //writer.writeUTF(line+"\n");
            }
        }catch(Exception e){
            logger.error("[HdfsOperate]>> writer a line error:"  ,  e);
        }
    }

    //关闭hdfs输出流
    public static void closeHdfsFile() {
        try {
            if (null != writer) {
                writer.flush();
                writer.close();
                logger.info("[HdfsOperate]>> closeHdfsFile close writer succeed!");
            }
            else{
                logger.error("[HdfsOperate]>> closeHdfsFile writer is null");
            }
        }catch(Exception e){
            logger.error("[HdfsOperate]>> closeHdfsFile close hdfs error:" + e);
        }
    }
    
}