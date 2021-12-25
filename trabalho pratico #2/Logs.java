import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Logs implements Runnable{
    private String path;
    private File logFile;
    private DirController dirc;
    private String serverIp;
    //private BufferedWriter writer;

public Logs(DirController dirc,String ip){
    this.path=dirc.getPasta();
    this.logFile=createDirectory("/Logs");//new File(System.getProperty("user.dir")+"/Logs/logs.txt");
    this.dirc=dirc;
    this.serverIp=ip;
    //this.writer=null;

}

public File createDirectory(String s){
    Path path = Paths.get(System.getProperty("user.dir")+"/Logs");
    try{
    Files.createDirectory(path);
    }
    catch(Exception e){};
    return new File(path+"/logs.txt");
}

public void writeToFile(String s) throws IOException{
    BufferedWriter writer=new BufferedWriter(new FileWriter(this.logFile,true));
    writer.append(s);
    writer.append("\n");
    writer.close();
}

public static double round(double value, int places) {
    if (places < 0) throw new IllegalArgumentException();

    BigDecimal bd = BigDecimal.valueOf(value);
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.doubleValue();
}

public double doubleDiv(double n,double d){
    double result=n/d;
    return round(result,2);
}

public void run(){
    FileState fs=FileState.SYNC;
    Map<Integer,FileStat> updateFilesC = new HashMap<>();
    StringBuilder fn;
    try{
        writeToFile("Synching Folder:"+this.path+" With Server:"+this.serverIp+" \nFileState.SYNC");
    }
    catch(IOException e){
        e.printStackTrace();
    }
    try{
        fs = dirc.isUpdate();
    }
    catch(InterruptedException e){
        e.printStackTrace();
    }
    while(true){
        switch(fs){
            case SENDINGAV:
                updateFilesC = dirc.getUpdateFiles();
                fn = new StringBuilder();
                fn.append("FileState.SENDINGAV\n");
                fn.append("Sending Files: ");
                for(FileStat f:updateFilesC.values()){
                    fn.append(f.getName()+"; ");
                }
                try {
                    writeToFile(fn.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try{
                    fs=dirc.waitSendingFL();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case SENDINGFL:
                fn = new StringBuilder();
                long start = System.currentTimeMillis();
                try{
                    fs=dirc.waitForSync();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long end = System.currentTimeMillis();
                end = end - start;
                long size=0,sizeb=0;
                for(FileStat f:updateFilesC.values()){
                    try{
                    size+=Files.size(Paths.get(this.path+f.getName()));
                    }
                    catch(Exception e) {};
                }
                double time = (double)end/1000;
                sizeb=size*8;
                double result = doubleDiv(size,time);
                fn.append("FileState.SENDINGFL\n");
                fn.append("Files Sent in:"+time+"s;"+end+"ms; "+"Size of File:"+sizeb+"bits;"+size+"bytes;"+" Speed of Transfer:"+result+"bits/second");//result
                try {
                    writeToFile(fn.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try{
                    fs=dirc.waitForSync();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case RECIVINGAV:
                try {
                    writeToFile("FileState.RECIVINGAV");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try{
                    fs=dirc.waitRecivingFL();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case RECIVINGFL:
                updateFilesC = dirc.getUpdateFiles();
                fn = new StringBuilder();
                fn.append("FileState.RECIVINGFL\n");
                fn.append("Reciving Files: ");
                for(FileStat f:updateFilesC.values()){
                    fn.append(f.getName()+"; ");
                }
                try{writeToFile(fn.toString());} 
                catch (IOException e){e.printStackTrace();}
                try {fs=dirc.waitForSync();}
                catch (InterruptedException e) {e.printStackTrace();}
                break;
            default:
                try{
                    fs = dirc.isUpdate();
                }
                catch(InterruptedException e){
                    e.printStackTrace();
                }
                break;
        }    
    }
}

}