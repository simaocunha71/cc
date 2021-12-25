import java.io.*;
import java.nio.file.*;

public class FileStat implements AutoCloseable{
    public static final int MAX_SIZE = 5120;

    private String name = null;
    private InputStream inFile = null;
    private OutputStream outFile = null;
    private int parts = 0;
    private int total = 0;
    
    public String getName(){ return name;}
    public int getParts(){ return parts; }
    public int getTotal(){ return total; }

    public FileStat() {}
    
    public void setSize(String file){
        try{ 
            long size = Files.size(Paths.get(file)); 
            this.total = intDiv(size,MAX_SIZE-12);
        }catch(Exception e) {};
    }

    public static FileStat fileStatIn(String pasta,String file){
        FileStat f = new FileStat(); f.name = file; 
        try{f.inFile = new FileInputStream(pasta + f.name); f.setSize(pasta + f.name);}
        catch(FileNotFoundException e) {f.total = -1;}
        catch(Exception e) {}
        return f;
    } 

    public static FileStat fileStatOut(String pasta,String file,int total){
        FileStat f = new FileStat();
        f.name = file; f.total = total;
        if (file.equals("") || file == null) return f; 
        try{ f.outFile = new FileOutputStream(pasta + f.name);}
        catch(Exception e) {};
        return f;
    }

    public byte[] getNameAsBytes() {
        return this.name.getBytes();        
    }

    public int readByte(byte[] message){
        if (this.parts > this.total){ return 0; }
        int read = 0;
        try{ 
            read = inFile.read(message); 
            this.parts += 1;
        }catch(Exception e) {}
        return read;
    }

    public boolean writeByte (int part,byte[] message) {
        if (part == (this.parts + 1)){
            this.parts += 1;      
            try {this.outFile.write(message);}
            catch(Exception e) {}
            return true;
        }
        return part <= this.parts;
    }

    private int intDiv(long f,int d){
        int r = 0;
        int n = (int) f;
        if ((n%d)==0) r = n/d;
        else r = n/d + 1;
        return r;
    }
    
    public static void removeFile(String name) {
        File f = new File(name);
        f.delete();
    }
    
    public void resetInput(String pasta, int part) {
        try{
            this.inFile.close();
            this.inFile = new FileInputStream(pasta + this.name);
            this.inFile.skip((long) (part * (MAX_SIZE-12)) );
            this.parts = part;
        } catch (Exception e){}
    }
    
    @Override
    public void close() throws Exception {
        if (inFile != null) {inFile.close(); inFile = null;}
        if (outFile != null) {outFile.close(); outFile = null;}
    }
   
    public static void clearDir(String pasta) {
        File dir = new File(pasta);
        File[] allFiles = dir.listFiles();
        for(File f : allFiles) if (!f.isDirectory()) f.delete();
    
    }

}