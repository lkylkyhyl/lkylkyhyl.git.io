import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import java.io.IOException;

/**
 * Merge two input files (A, B) and remove duplicated records.
 * input : each line "date letter"   e.g. 20170101 x
 * output: sorted unique "date letter" records
 */
public class MergeDedup {

    public static class DupMapper extends Mapper<Object, Text, Text, Text> {
        @Override
        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty()) return;
            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                context.write(new Text(parts[0]), new Text(parts[1]));
            } else {
                context.write(new Text(line), new Text(""));
            }
        }
    }

    public static class DupReducer extends Reducer<Text, Text, Text, Text> {
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {
            java.util.TreeSet<String> seen = new java.util.TreeSet<>();
            for (Text v : values) {
                seen.add(v.toString());
            }
            for (String s : seen) {
                context.write(key, new Text(s));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Merge And Dedup");
        job.setJarByClass(MergeDedup.class);
        job.setMapperClass(DupMapper.class);
        job.setReducerClass(DupReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        for (int i = 0; i < args.length - 1; i++) {
            FileInputFormat.addInputPath(job, new Path(args[i]));
        }
        FileOutputFormat.setOutputPath(job, new Path(args[args.length - 1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
