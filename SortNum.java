import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import java.io.IOException;

/**
 * Read integers from several input files, sort them in ascending order,
 * and output "rank value" per line.
 * input : one integer per line, from multiple files
 * output: "rank value"  e.g.  1 1
 */
public class SortNum {

    public static class SortMapper extends Mapper<Object, Text, IntWritable, IntWritable> {
        @Override
        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty()) return;
            context.write(new IntWritable(Integer.parseInt(line)), new IntWritable(1));
        }
    }

    public static class SortReducer extends Reducer<IntWritable, IntWritable, IntWritable, IntWritable> {
        private int rank = 1;

        @Override
        public void reduce(IntWritable key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            for (IntWritable v : values) {
                context.write(new IntWritable(rank++), key);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        conf.set("mapreduce.output.textoutputformat.separator", " ");
        Job job = Job.getInstance(conf, "Sort Numbers");
        job.setJarByClass(SortNum.class);
        job.setMapperClass(SortMapper.class);
        job.setReducerClass(SortReducer.class);
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(IntWritable.class);
        job.setNumReduceTasks(1); // single reducer -> global ordering
        for (int i = 0; i < args.length - 1; i++) {
            FileInputFormat.addInputPath(job, new Path(args[i]));
        }
        FileOutputFormat.setOutputPath(job, new Path(args[args.length - 1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
