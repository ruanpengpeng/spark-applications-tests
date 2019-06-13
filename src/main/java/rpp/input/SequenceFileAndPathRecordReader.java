package rpp.input;

import java.io.IOException;

import org.apache.avro.mapred.AvroRecordReader;
import org.apache.avro.mapred.Pair;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;

public class SequenceFileAndPathRecordReader<K, V> extends AvroRecordReader<Pair<K, V>> {
	public SequenceFileAndPathRecordReader(JobConf job, FileSplit split) throws IOException {
		super(new SequenceFileAndPathReader<K, V>(split.getPath().toUri(), job), split);
	}
}
