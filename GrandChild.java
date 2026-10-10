import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Single-table self-join: mine grandchild-grandparent relations from a
 * child-parent table.
 * input : "child parent" per line (first line is a header, skipped)
 * output: "grandchild grandparent"
 */
public class GrandChild {

    public static class GC_Mapper extends Mapper<Object, Text, Text, Text> {
        @Override
        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty() || line.startsWith("child")) return; // skip header
            String[] parts = line.split("\\s+");
            if (parts.length < 2) return;
            String child = parts[0];
            String parent = parts[1];
            context.write(new Text(parent), new Text("1:" + child)); // parent -> has child
            context.write(new Text(child), new Text("2:" + parent)); // child  -> has parent
        }
    }

    public static class GC_Reducer extends Reducer<Text, Text, Text, Text> {
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {
            List<String> children = new ArrayList<>();
            List<String> parents = new ArrayList<>();
            for (Text t : values) {
                String s = t.toString();
                if (s.startsWith("1:")) children.add(s.substring(2));
                else if (s.startsWith("2:")) parents.add(s.substring(2));
            }
            // key has parents in `parents` and children in `children`
            // => each child of key is a grandchild of each parent of key
            for (String p : parents) {
                for (String c : children) {
                    context.write(new Text(c), new Text(p));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "GrandChild Mining");
        job.setJarByClass(GrandChild.class);
        job.setMapperClass(GC_Mapper.class);
        job.setReducerClass(GC_Reducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setNumReduceTasks(1);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
