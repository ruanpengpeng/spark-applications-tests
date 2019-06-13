package rpp.filter;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
/**
 * 过滤空文件和不符合指定文件类型的文件
 * @author Administrator
 *
 */
public class EmptyOrMismatchTypeFileFilter implements PathFilter {
	public static List<String> emptyFiles = new ArrayList<String>();
	public static List<String> mismatchTypeFiles = new ArrayList<String>();
	@Override
	public boolean accept(Path path) {
		boolean empty = emptyFile(path);
		boolean mismatchType = mismatchTypeFile(path);
		return !(empty||mismatchType);
	}
	protected boolean emptyFile(Path path){
		boolean empty = emptyFiles.contains(path.toString());
		if(empty) System.out.println("=== filter empty file : "+path.toString()+" ===");
		return empty;
	}
	protected boolean mismatchTypeFile(Path path){
		boolean mismatchType = mismatchTypeFiles.contains(path.toString());
		if(mismatchType) System.out.println("=== filter mismatch type file : "+path.toString()+" ===");
		return mismatchType;
	}
}
