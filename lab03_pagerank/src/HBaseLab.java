import java.io.*;
import java.util.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

/** Uses only lab3_ prefixed tables in the demonstration. */
public class HBaseLab implements AutoCloseable {
  private final Connection connection;
  public HBaseLab() throws IOException { connection=ConnectionFactory.createConnection(HBaseConfiguration.create()); }
  static byte[] b(String s){return Bytes.toBytes(s);}
  static String[] col(String field){String[] p=field.split(":",2);return new String[]{p[0],p.length==2?p[1]:"value"};}
  public void createTable(String tableName,String[] fields) throws IOException {
    TableName name=TableName.valueOf(tableName);
    Set<String> families=new TreeSet<>();for(String field:fields){String f=col(field)[0];if(f.isEmpty())throw new IllegalArgumentException("Empty family");families.add(f);}
    if(families.isEmpty())throw new IllegalArgumentException("At least one column family required");
    try(Admin a=connection.getAdmin()){
      if(a.tableExists(name)){if(a.isTableEnabled(name))a.disableTable(name);a.deleteTable(name);}
      TableDescriptorBuilder d=TableDescriptorBuilder.newBuilder(name);
      for(String f:families)d.setColumnFamily(ColumnFamilyDescriptorBuilder.of(f));a.createTable(d.build());
    }
  }
  public void addRecord(String tableName,String row,String[] fields,String[] values) throws IOException {
    if(fields.length!=values.length)throw new IllegalArgumentException("fields/values length mismatch");
    Put p=new Put(b(row));for(int i=0;i<fields.length;i++){String[] c=col(fields[i]);p.addColumn(b(c[0]),b(c[1]),b(values[i]));}
    try(Table t=connection.getTable(TableName.valueOf(tableName))){t.put(p);}
  }
  public void listTables() throws IOException {try(Admin a=connection.getAdmin()){for(TableDescriptor t:a.listTableDescriptors())System.out.println("TABLE "+t);}}
  public void scanTable(String tableName) throws IOException {
    try(Table t=connection.getTable(TableName.valueOf(tableName));ResultScanner rs=t.getScanner(new Scan())){
      for(Result r:rs)for(Cell c:r.rawCells())System.out.printf("%s %s:%s=%s%n",Bytes.toString(r.getRow()),Bytes.toString(CellUtil.cloneFamily(c)),Bytes.toString(CellUtil.cloneQualifier(c)),Bytes.toString(CellUtil.cloneValue(c)));
    }
  }
  /** Full-row scan is intentional: selecting a column in Scan would hide null rows. */
  public Map<String,Map<String,String>> scanColumn(String tableName,String column) throws IOException {
    String[] wanted=column.split(":",2);Map<String,Map<String,String>> result=new TreeMap<>();
    try(Table t=connection.getTable(TableName.valueOf(tableName));ResultScanner rs=t.getScanner(new Scan())){
      for(Result r:rs){Map<String,String> cells=new TreeMap<>();for(Cell c:r.rawCells()){
        String f=Bytes.toString(CellUtil.cloneFamily(c)),q=Bytes.toString(CellUtil.cloneQualifier(c));
        if(f.equals(wanted[0])&&(wanted.length==1||q.equals(wanted[1])))cells.put(f+":"+q,Bytes.toString(CellUtil.cloneValue(c)));
      }result.put(Bytes.toString(r.getRow()),cells.isEmpty()?null:cells);}
    }System.out.println("COLUMN "+column+" "+result);return result;
  }
  public void modifyData(String tableName,String row,String column,String value) throws IOException {addRecord(tableName,row,new String[]{column},new String[]{value});}
  // Assignment's three-argument signature has no new value: read it from stdin.
  public void modifyData(String tableName,String row,String column) throws IOException {
    System.out.print("New value: ");String v=new BufferedReader(new InputStreamReader(System.in,"UTF-8")).readLine();
    if(v==null)throw new EOFException("No new value supplied");modifyData(tableName,row,column,v);
  }
  public void deleteRow(String tableName,String row) throws IOException {try(Table t=connection.getTable(TableName.valueOf(tableName))){t.delete(new Delete(b(row)));}}
  public void addFamily(String tableName,String family) throws IOException {try(Admin a=connection.getAdmin()){a.addColumnFamily(TableName.valueOf(tableName),ColumnFamilyDescriptorBuilder.of(family));}}
  public void deleteFamily(String tableName,String family) throws IOException {try(Admin a=connection.getAdmin()){a.deleteColumnFamily(TableName.valueOf(tableName),b(family));}}
  public void deleteColumn(String tableName,String row,String column) throws IOException {String[] c=col(column);try(Table t=connection.getTable(TableName.valueOf(tableName))){t.delete(new Delete(b(row)).addColumns(b(c[0]),b(c[1])));}}
  public void clearTable(String tableName) throws IOException {try(Admin a=connection.getAdmin()){TableName n=TableName.valueOf(tableName);if(a.isTableEnabled(n))a.disableTable(n);a.truncateTable(n,true);}}
  public long countRows(String tableName) throws IOException {long n=0;try(Table t=connection.getTable(TableName.valueOf(tableName));ResultScanner rs=t.getScanner(new Scan().setFilter(new org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter()))){for(Result r:rs)n++;}System.out.println("COUNT "+tableName+"="+n);return n;}
  public void close() throws IOException {connection.close();}
  static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);System.out.println("PASS "+message);}
  public static void main(String[] args) throws Exception {
    try(HBaseLab h=new HBaseLab()){
      String student="lab3_Student",course="lab3_Course",sc="lab3_SC";
      h.createTable(student,new String[]{"Info","Score"});h.createTable(course,new String[]{"Info"});h.createTable(sc,new String[]{"Info"});
      String[][] students={{"2015001","Zhangsan","male","23"},{"2015002","Mary","female","22"},{"2015003","Lisi","male","24"}};
      for(String[] s:students)h.addRecord(student,s[1],new String[]{"Info:S_No","Info:S_Name","Info:S_Sex","Info:S_Age"},s);
      String[][] courses={{"123001","Math","2.0"},{"123002","Computer Science","5.0"},{"123003","English","3.0"}};
      for(String[] c:courses)h.addRecord(course,c[0],new String[]{"Info:C_No","Info:C_Name","Info:C_Credit"},c);
      String[][] choices={{"2015001","123001","86"},{"2015001","123003","69"},{"2015002","123002","77"},{"2015002","123003","99"},{"2015003","123001","98"},{"2015003","123002","95"}};
      for(String[] s:choices){h.addRecord(sc,s[0]+"_"+s[1],new String[]{"Info:SC_Sno","Info:SC_Cno","Info:SC_Score"},s);
        String name=null,subject=null;for(String[] a:students)if(a[0].equals(s[0]))name=a[1];for(String[] c:courses)if(c[0].equals(s[1]))subject=c[1];
        h.addRecord(student,name,new String[]{"Score:"+subject},new String[]{s[2]});}
      h.listTables();h.scanTable(student);h.scanTable(course);h.scanTable(sc);
      check(h.countRows(student)==3,"student rows=3");check(h.countRows(course)==3,"course rows=3");check(h.countRows(sc)==6,"SC rows=6");
      h.scanColumn(student,"Score");check(h.scanColumn(student,"Score:Math").get("Mary")==null,"missing Math returns null");
      h.modifyData(student,"Zhangsan","Score:Math","90");check("90".equals(h.scanColumn(student,"Score:Math").get("Zhangsan").get("Score:Math")),"modify score");
      h.modifyData(student,"Zhangsan","Score:Math","86"); // restore base dataset
      String scratch="lab3_ops";h.createTable(scratch,new String[]{"Info"});h.addRecord(scratch,"temp",new String[]{"Info:x"},new String[]{"1"});
      h.createTable(scratch,new String[]{"Info"});check(h.countRows(scratch)==0,"create existing table resets data");
      h.addFamily(scratch,"Extra");h.addRecord(scratch,"temp",new String[]{"Info:x","Extra:y"},new String[]{"1","2"});
      h.deleteColumn(scratch,"temp","Extra:y");check(h.scanColumn(scratch,"Extra:y").get("temp")==null,"delete column all versions");
      h.deleteFamily(scratch,"Extra");h.deleteRow(scratch,"temp");check(h.countRows(scratch)==0,"delete row");
      h.addRecord(scratch,"temp",new String[]{"Info:x"},new String[]{"1"});h.clearTable(scratch);check(h.countRows(scratch)==0,"truncate keeps table");
      System.out.println("ALL_HBASE_CHECKS_PASSED");
    }
  }
}
