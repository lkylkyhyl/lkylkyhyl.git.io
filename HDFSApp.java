import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import java.io.*;
import java.util.Scanner;

/**
 * HDFSApp - HDFS Java API demo covering the 10 experiment tasks:
 *  1 upload (append/overwrite when exists)   2 download (auto rename)
 *  3 cat to terminal                        4 file info (perm/size/time/path)
 *  5 recursive listing                      6 create/delete file (auto mkdir parent)
 *  7 mkdir/rmdir (only delete empty dir)    8 append content to head or tail
 *  9 delete file                            10 move file
 */
public class HDFSApp {
    static FileSystem fs;

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs://localhost:9000");
        fs = FileSystem.get(conf);
        String cmd = args.length > 0 ? args[0] : "help";
        try {
            switch (cmd) {
                case "upload":     doUpload(args); break;
                case "download":   doDownload(args); break;
                case "cat":        doCat(args); break;
                case "info":       doInfo(args); break;
                case "lsr":        doLSR(args); break;
                case "create":     doCreateFile(args); break;
                case "delete":     doDeleteFile(args); break;
                case "mkdir":      doMkdir(args); break;
                case "rmdir":      doRmdir(args); break;
                case "append":     doAppend(args); break;
                case "move":       doMove(args); break;
                default:
                    System.out.println("Usage: HDFSApp <cmd> [args]");
                    System.out.println("  upload <local> <remote> [append|overwrite]");
                    System.out.println("  download <remote> [localdir]");
                    System.out.println("  cat <remote> | info <remote> | lsr <dir>");
                    System.out.println("  create <remote> | delete <remote>");
                    System.out.println("  mkdir <dir> | rmdir <dir>");
                    System.out.println("  append <remote> <content> [head|tail]");
                    System.out.println("  move <src> <dst>");
            }
        } finally {
            fs.close();
        }
    }

    /* 1: upload; if remote exists, append or overwrite (user-specified) */
    static void doUpload(String[] args) throws Exception {
        String local = args[1], remote = args[2];
        String mode = args.length > 3 ? args[3] : "ask";
        Path rp = new Path(remote);
        if (fs.exists(rp)) {
            if ("ask".equals(mode)) {
                System.out.println("[upload] target " + remote + " already exists on HDFS.");
                System.out.print("[upload] append(a) or overwrite(o)? ");
                Scanner sc = new Scanner(System.in);
                String ans = sc.nextLine().trim();
                mode = ans.startsWith("a") ? "append" : "overwrite";
            }
            if ("append".equals(mode)) {
                FSDataOutputStream out = fs.append(rp);
                FileInputStream in = new FileInputStream(local);
                byte[] buf = new byte[4096]; int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                in.close(); out.close();
                System.out.println("[upload] file exists -> content APPENDED to: " + remote);
            } else {
                fs.copyFromLocalFile(false, true, new Path(local), rp);
                System.out.println("[upload] file exists -> OVERWRITTEN: " + remote);
            }
        } else {
            fs.copyFromLocalFile(false, false, new Path(local), rp);
            System.out.println("[upload] uploaded: " + local + " -> " + remote);
        }
    }

    /* 2: download; auto-rename if a local file with the same name exists */
    static void doDownload(String[] args) throws Exception {
        Path rp = new Path(args[1]);
        String localDir = args.length > 2 ? args[2] : ".";
        File localFile = new File(localDir, rp.getName());
        if (localFile.exists()) {
            int i = 1;
            File renamed;
            do { renamed = new File(localDir, rp.getName() + "." + i); i++; }
            while (renamed.exists());
            System.out.println("[download] local file " + localFile.getName()
                    + " already exists -> auto renamed to: " + renamed.getName());
            localFile = renamed;
        }
        fs.copyToLocalFile(rp, new Path(localFile.getAbsolutePath()));
        System.out.println("[download] saved to: " + localFile.getAbsolutePath());
    }

    /* 3: cat file content to terminal */
    static void doCat(String[] args) throws Exception {
        FSDataInputStream in = fs.open(new Path(args[1]));
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line; int count = 0;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
            if (++count >= 22) { System.out.println("... (truncated for demo)"); break; }
        }
        br.close();
    }

    /* 4: show file permission / size / create time / path */
    static void doInfo(String[] args) throws Exception {
        Path p = new Path(args[1]);
        FileStatus st = fs.getFileStatus(p);
        System.out.println("[info] path       : " + st.getPath());
        System.out.println("[info] permission : " + st.getPermission());
        System.out.println("[info] size       : " + st.getLen() + " bytes");
        System.out.println("[info] create time: " + new java.util.Date(st.getModificationTime()));
        System.out.println("[info] owner/group: " + st.getOwner() + " / " + st.getGroup());
    }

    /* 5: recursive listing of a directory */
    static void doLSR(String[] args) throws Exception {
        RemoteIterator<LocatedFileStatus> it = fs.listFiles(new Path(args[1]), true);
        int cnt = 0;
        while (it.hasNext()) {
            LocatedFileStatus st = it.next();
            System.out.println("[lsr] " + st.getPermission() + "  size=" + st.getLen()
                    + "  " + new java.util.Date(st.getModificationTime()) + "  " + st.getPath());
            cnt++;
        }
        System.out.println("[lsr] total " + cnt + " file(s) under " + args[1]);
    }

    /* 6: create a file; automatically create its parent directory if missing */
    static void doCreateFile(String[] args) throws Exception {
        Path p = new Path(args[1]);
        if (p.getParent() != null && !fs.exists(p.getParent())) {
            fs.mkdirs(p.getParent());
            System.out.println("[create] parent directory created: " + p.getParent());
        }
        FSDataOutputStream out = fs.create(p, false);
        out.writeBytes("hello hdfs\n");
        out.close();
        System.out.println("[create] file created: " + p);
    }

    /* 9: delete a file */
    static void doDeleteFile(String[] args) throws Exception {
        Path p = new Path(args[1]);
        if (fs.exists(p) && fs.getFileStatus(p).isFile()) {
            fs.delete(p, false);
            System.out.println("[delete] file deleted: " + p);
        } else {
            System.out.println("[delete] no such file: " + p);
        }
    }

    /* 7a: create directory (parents auto-created) */
    static void doMkdir(String[] args) throws Exception {
        Path p = new Path(args[1]);
        if (fs.exists(p)) {
            System.out.println("[mkdir] directory already exists: " + p);
        } else {
            fs.mkdirs(p);
            System.out.println("[mkdir] directory created: " + p);
        }
    }

    /* 7b: delete directory only when it is empty */
    static void doRmdir(String[] args) throws Exception {
        Path p = new Path(args[1]);
        if (!fs.exists(p)) { System.out.println("[rmdir] no such directory: " + p); return; }
        FileStatus[] children = fs.listStatus(p);
        if (children.length == 0) {
            fs.delete(p, false);
            System.out.println("[rmdir] empty directory deleted: " + p);
        } else {
            System.out.println("[rmdir] directory NOT empty (" + children.length
                    + " item(s)) -> not deleted: " + p);
        }
    }

    /* 8: append content to the head or the tail of a file (user-specified) */
    static void doAppend(String[] args) throws Exception {
        Path rp = new Path(args[1]);
        String content = args[2];
        String pos = args.length > 3 ? args[3] : "tail";
        if ("tail".equals(pos)) {
            FSDataOutputStream out = fs.append(rp);
            out.writeBytes(content + "\n");
            out.close();
            System.out.println("[append] content appended to TAIL of " + args[1]);
        } else {
            FSDataInputStream in = fs.open(rp);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            FSDataOutputStream out = fs.create(rp, true);
            out.writeBytes(content + "\n");
            out.write(bos.toByteArray());
            out.close();
            System.out.println("[append] content prepended to HEAD of " + args[1]);
        }
    }

    /* 10: move file from source path to destination path */
    static void doMove(String[] args) throws Exception {
        Path src = new Path(args[1]), dst = new Path(args[2]);
        if (fs.rename(src, dst)) {
            System.out.println("[move] moved: " + src + " -> " + dst);
        } else {
            System.out.println("[move] failed");
        }
    }
}
