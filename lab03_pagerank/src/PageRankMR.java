import java.io.*;
import java.util.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/** Ten genuine MapReduce jobs. State: page TAB rank TAB comma-separated links. */
public class PageRankMR {
  public static class PRMapper extends Mapper<LongWritable,Text,Text,Text> {
    public void map(LongWritable k, Text v, Context c) throws IOException,InterruptedException {
      String[] p=v.toString().split("\t",-1);
      c.write(new Text(p[0]), new Text("A"+p[2])); // preserve zero-inlink vertices
      if (!p[2].isEmpty()) {
        String[] links=p[2].split(","); double share=Double.parseDouble(p[1])/links.length;
        for(String dest:links) c.write(new Text(dest),new Text("R"+share));
    }
    }
  }
  public static class PRReducer extends Reducer<Text,Text,Text,Text> {
    public void reduce(Text page,Iterable<Text> values,Context c) throws IOException,InterruptedException {
      String links=""; double sum=0;
      for(Text t:values){String s=t.toString();if(s.startsWith("A")) links=s.substring(1);else sum+=Double.parseDouble(s.substring(1));}
      Configuration conf=c.getConfiguration();
      double n=conf.getInt("pr.n",1), d=conf.getDouble("pr.d",0.85), dangling=conf.getDouble("pr.dangling",0);
      c.write(page,new Text(Double.toString(1-d+d*(sum+dangling/n))+"\t"+links));
    }
  }
  static List<String> read(FileSystem fs,Path path) throws IOException {
    List<String> lines=new ArrayList<>();
    if(fs.isFile(path)){try(BufferedReader b=new BufferedReader(new InputStreamReader(fs.open(path),"UTF-8"))){String s;while((s=b.readLine())!=null)lines.add(s);}}
    else for(FileStatus f:fs.listStatus(path))if(!f.getPath().getName().startsWith("_")&&!f.getPath().getName().startsWith("."))lines.addAll(read(fs,f.getPath()));
    return lines;
  }
  public static void main(String[] args) throws Exception {
    if(args.length!=2)throw new IllegalArgumentException("PageRankMR input outputRoot (must not exist)");
    Configuration base=new Configuration(); FileSystem fs=new Path(args[0]).getFileSystem(base);
    Path root=new Path(args[1]); if(fs.exists(root))throw new IOException("Refusing to overwrite "+root);
    // Lab-sized graph: normalize duplicate edges and complete target-only vertices.
    TreeMap<String,TreeSet<String>> graph=new TreeMap<>();
    for(String line:read(fs,new Path(args[0]))){if(line.trim().isEmpty())continue;String[] p=line.split("\t",-1);if(p.length!=2||p[0].trim().isEmpty())throw new IOException("Expected page TAB links: "+line);
      String page=p[0].trim();graph.computeIfAbsent(page,x->new TreeSet<>());
      for(String s:p[1].split(",")){s=s.trim();if(!s.isEmpty()){graph.get(page).add(s);graph.computeIfAbsent(s,x->new TreeSet<>());}}
    }
    if(graph.isEmpty())throw new IOException("Empty graph");
    fs.mkdirs(root);Path state=new Path(root,"iter00");fs.mkdirs(state);
    try(PrintWriter w=new PrintWriter(new OutputStreamWriter(fs.create(new Path(state,"part-00000")),"UTF-8"))){for(String p:graph.keySet())w.println(p+"\t1.0\t"+String.join(",",graph.get(p)));}
    for(int i=1;i<=10;i++){
      double dangling=0;for(String line:read(fs,state)){String[] p=line.split("\t",-1);if(p[2].isEmpty())dangling+=Double.parseDouble(p[1]);}
      Configuration conf=new Configuration(base);conf.setInt("pr.n",graph.size());conf.setDouble("pr.d",0.85);conf.setDouble("pr.dangling",dangling);
      conf.setInt("yarn.app.mapreduce.am.resource.mb",512);conf.set("yarn.app.mapreduce.am.command-opts","-Xmx384m");
      conf.setInt("mapreduce.map.memory.mb",512);conf.set("mapreduce.map.java.opts","-Xmx384m");
      conf.setInt("mapreduce.reduce.memory.mb",512);conf.set("mapreduce.reduce.java.opts","-Xmx384m");
      Job job=Job.getInstance(conf,"lab3-pagerank-"+i);job.setJarByClass(PageRankMR.class);job.setMapperClass(PRMapper.class);job.setReducerClass(PRReducer.class);
      job.setMapOutputKeyClass(Text.class);job.setMapOutputValueClass(Text.class);job.setOutputKeyClass(Text.class);job.setOutputValueClass(Text.class);job.setNumReduceTasks(1);
      FileInputFormat.addInputPath(job,state);Path next=new Path(root,String.format("iter%02d",i));FileOutputFormat.setOutputPath(job,next);
      if(!job.waitForCompletion(true))throw new IOException("Iteration "+i+" failed");state=next;
      System.out.println("ITERATION "+i+" COMPLETE dangling="+dangling);
    }
    List<String[]> ranks=new ArrayList<>();for(String line:read(fs,state))ranks.add(line.split("\t",-1));
    ranks.sort((a,b)->{int c=Double.compare(Double.parseDouble(b[1]),Double.parseDouble(a[1]));return c!=0?c:a[0].compareTo(b[0]);});
    Path out=new Path(root,"final");fs.mkdirs(out);
    try(PrintWriter w=new PrintWriter(new OutputStreamWriter(fs.create(new Path(out,"part-r-00000")),"UTF-8"))){for(String[] p:ranks){String s=String.format(Locale.ROOT,"(%s,%.10f)",p[0],Double.parseDouble(p[1]));w.println(s);System.out.println(s);}}
  }
}
