import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * 实验二：文档倒排索引算法实现 (Inverted Index)
 * 
 * 功能描述：
 * 1. 从输入英文文档中切分单词并进行标准化（转小写、过滤标点）。
 * 2. 结合停词表过滤无检索价值的停用词。
 * 3. 统计每个单词在各个文档中的词频，以及在全部文档中的总出现次数。
 * 4. 按照实验指定格式输出：
 *    word <doc1.txt,count1>;<doc2.txt,count2>;...;<total,sum>.
 */
public class InvertedIndex {

    public static class InvertedIndexMapper extends Mapper<LongWritable, Text, Text, Text> {
        private String currentFileName;
        private final Set<String> stopWords = new HashSet<>();
        private final Map<String, Integer> localWordCount = new HashMap<>();
        private static final Pattern WORD_PATTERN = Pattern.compile("^[a-zA-Z]+$");

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            // 获取当前切片对应的文档文件名
            FileSplit fileSplit = (FileSplit) context.getInputSplit();
            currentFileName = fileSplit.getPath().getName();

            // 1. 尝试从 DistributedCache 加载停词表
            URI[] cacheFiles = context.getCacheFiles();
            boolean loadedFromCache = false;
            if (cacheFiles != null && cacheFiles.length > 0) {
                for (URI uri : cacheFiles) {
                    Path path = new Path(uri.getPath());
                    String fileName = path.getName();
                    if (fileName.contains("stopword") || fileName.endsWith(".txt")) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String w = line.trim().toLowerCase();
                                if (!w.isEmpty()) {
                                    stopWords.add(w);
                                }
                            }
                            loadedFromCache = true;
                        } catch (Exception e) {
                            // 忽略单个文件加载失败
                        }
                    }
                }
            }

            // 2. 尝试从 HDFS 配置路径或本地读取 stopwords.txt
            if (!loadedFromCache) {
                String stopwordPathStr = context.getConfiguration().get("stopwords.path");
                if (stopwordPathStr != null && !stopwordPathStr.isEmpty()) {
                    Path swPath = new Path(stopwordPathStr);
                    FileSystem fs = swPath.getFileSystem(context.getConfiguration());
                    if (fs.exists(swPath)) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fs.open(swPath)))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String w = line.trim().toLowerCase();
                                if (!w.isEmpty()) {
                                    stopWords.add(w);
                                }
                            }
                            loadedFromCache = true;
                        }
                    }
                }
            }

            // 3. 内置默认常用停用词作为兜底保障
            if (stopWords.isEmpty()) {
                String[] defaultStopwords = {
                    "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
                    "aren't", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both",
                    "but", "by", "can", "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does",
                    "doesn't", "doing", "don't", "down", "during", "each", "few", "for", "from", "further", "had",
                    "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her",
                    "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i", "i'd",
                    "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself",
                    "let's", "me", "more", "most", "mustn't", "my", "myself", "no", "nor", "not", "of", "off",
                    "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over",
                    "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so",
                    "some", "such", "than", "that", "that's", "the", "their", "theirs", "them", "themselves",
                    "then", "there", "there's", "these", "they", "they'd", "they'll", "they're", "they've", "this",
                    "those", "through", "to", "too", "under", "until", "up", "very", "was", "wasn't", "we",
                    "we'd", "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when", "when's",
                    "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't",
                    "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself",
                    "yourselves"
                };
                for (String sw : defaultStopwords) {
                    stopWords.add(sw);
                }
            }
        }

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            // 对每行文本进行分词清洗，过滤非字母字符
            String line = value.toString().toLowerCase();
            String[] tokens = line.split("[^a-zA-Z]+");
            for (String token : tokens) {
                token = token.trim();
                // 过滤长度小于2的单字符、停用词
                if (token.length() >= 2 && WORD_PATTERN.matcher(token).matches()) {
                    if (!stopWords.contains(token)) {
                        localWordCount.put(token, localWordCount.getOrDefault(token, 0) + 1);
                    }
                }
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            // Map 阶段结束时，以 (word, fileName:count) 的形式发送给 Reducer，实现 Combiner 的高效聚合
            Text outKey = new Text();
            Text outValue = new Text();
            for (Map.Entry<String, Integer> entry : localWordCount.entrySet()) {
                outKey.set(entry.getKey());
                outValue.set(currentFileName + ":" + entry.getValue());
                context.write(outKey, outValue);
            }
        }
    }

    public static class InvertedIndexReducer extends Reducer<Text, Text, Text, Text> {
        private final Text resultValue = new Text();

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            // 使用 TreeMap 保证输出按文档名自然排序 (document01.txt, document02.txt...)
            Map<String, Integer> docCounts = new TreeMap<>();
            int total = 0;

            for (Text val : values) {
                String[] parts = val.toString().split(":");
                if (parts.length == 2) {
                    String docName = parts[0];
                    int count = Integer.parseInt(parts[1]);
                    docCounts.put(docName, docCounts.getOrDefault(docName, 0) + count);
                    total += count;
                }
            }

            // 构建实验要求格式：<document01.txt,6>;<document02.txt,3>;...;<total,11>.
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> entry : docCounts.entrySet()) {
                sb.append("<").append(entry.getKey()).append(",").append(entry.getValue()).append(">;");
            }
            sb.append("<total,").append(total).append(">.");

            resultValue.set(sb.toString());
            context.write(key, resultValue);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        if (args.length < 2) {
            System.err.println("Usage: InvertedIndex <input path> <output path> [stopwords path]");
            System.exit(-1);
        }

        Job job = Job.getInstance(conf, "Inverted Index Generation");
        job.setJarByClass(InvertedIndex.class);

        job.setMapperClass(InvertedIndexMapper.class);
        job.setReducerClass(InvertedIndexReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 如果传入了第3个参数（停词表路径），加入 DistributedCache
        if (args.length >= 3) {
            job.addCacheFile(new URI(args[2]));
            job.getConfiguration().set("stopwords.path", args[2]);
        }

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
