package rpp.common;

import java.util.Map;

public class PartitionUtil {
		/**
		 * 对于txt这样的小文件，使用wholeTextFile时，需要指定patition，否则每个文件为一个分区，执行效率太低。
		 * @return 经过计算的分区数
		 */
		public static int adjustPartitions(Map<String, String> statisticMetadata,int memoryDivideVcore){
			long inputSize = Long.parseLong(statisticMetadata.get("TotalInputFileSize"));
			int mDV = (memoryDivideVcore*1024)/52;//单个分区的最大数据量，以M为单位取整数
			System.out.println("===单个分区最大数据量为："+mDV+" M ===");
			int minPartitions = (int)(inputSize/(mDV*1024*1024));
			System.out.println("=== txt小文件类处理的最佳分区数为:"+minPartitions+" ===");
			return minPartitions;
		}
}
