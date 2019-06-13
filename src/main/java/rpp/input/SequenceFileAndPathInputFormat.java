package rpp.input;

import java.io.IOException;

import org.apache.avro.mapred.AvroWrapper;
import org.apache.avro.mapred.Pair;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

public class SequenceFileAndPathInputFormat<K,V> extends FileInputFormat<AvroWrapper<Pair<K,V>>, NullWritable> {

	@Override
	public RecordReader<AvroWrapper<Pair<K, V>>, NullWritable> getRecordReader(InputSplit split, JobConf job,
			Reporter reporter) throws IOException {
		reporter.setStatus(split.toString());
	    return new SequenceFileAndPathRecordReader<K,V>(job, (FileSplit)split);
	}

	
}
