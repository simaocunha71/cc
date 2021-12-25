import java.io.File;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class DirController {
    
    //
    private ReentrantLock l = new ReentrantLock();
    private Condition c = l.newCondition();
    //
    private String pasta = null;
    private long timer = 0;
    private int timerCmp = 0;
    //
    private String code = null;
    private boolean codeValidation = false;
    //
    public static final FileState initState = FileState.PREAUTICATION;
    private FileState state = initState;
    //
    private Map<Integer,FileStat> updateFiles = new HashMap<Integer,FileStat>();
    //
    private ArrayList<Integer> filesID = new ArrayList<Integer>();
    private ArrayList<Integer> filesIDEnded = new ArrayList<Integer>();
    //
    private ArrayList<Integer> errorsID = new ArrayList<Integer>();
    //
    private int counterAV = 0;
    private int counterFL = 0;
    //
    private int windowMax = 0;
    private int errors = 0;
    private int maxPermErrors = 15; 


    public DirController(String pasta, String passeCode, long timer){ 
        this.pasta = pasta; 
        this.code = passeCode;
        this.timer = timer;
    }

    // 

    public Map<Integer,FileStat> getUpdateFiles(){
        l.lock();
        try{return new HashMap<Integer,FileStat>(this.updateFiles);}
        finally{l.unlock();}
    }

    public FileState getFileState(){
        l.lock();
        try{ return this.state; }
        finally{ l.unlock(); }
    }

    public String getPasta(){
        l.lock();
        try{return this.pasta;}
        finally {l.unlock();}
    }

    public FileState isUpdate() throws InterruptedException{
        l.lock();
        try{
            while(this.state == FileState.SYNC){ c.await(); }
            return state;
        }finally{ l.unlock(); }
    }

    public FileState waitSendingFL() throws InterruptedException{
        l.lock();
        try{
            while(this.state != FileState.SENDINGFL){ c.await(); }
            return state;
        }finally{ l.unlock(); }
    }

    public FileState waitForSync() throws InterruptedException{
        l.lock();
        try{
            FileState last = FileState.SYNC;
            while(this.state != FileState.SYNC){ 
                last = this.state;
                c.await(); 
            }
            return last;
        }finally{ l.unlock(); }
    }

    public FileState waitRecivingAV() throws InterruptedException{
        l.lock();
        try{
            while(this.state != FileState.OKAV && this.state != FileState.ERRORAV){ c.await(); }
            return state;
        }finally{ l.unlock(); }
    }

    public FileState waitRecivingFL() throws InterruptedException{
        l.lock();
        try{
            while(this.state != FileState.OKFL && this.state != FileState.ERRORFL){ c.await(); }
            return state;
        }finally{ l.unlock(); }
    }

    public boolean isReciving(){
        l.lock();
        try{
            return (this.state == FileState.RECIVINGAV || this.state == FileState.RECIVINGFL);
        }finally{ l.unlock(); }
    }

    //

    public void gotAu(byte[] data, int length) {
        boolean stateChange = false;
        try{
            char type = ByteBuffer.wrap(data).getChar(4);
            l.lock();
            switch(type){
                case 'Q': 
                    if (this.state != FileState.SENDINGANSWER ) {this.state = FileState.SENDINGANSWER; stateChange = true;}
                break;
                
                case 'A':
                    if (this.state != FileState.SENDINGCODE) {this.state = FileState.SENDINGCODE; stateChange = true;}
                break;
                
                case 'C': 
                    System.out.println("Autencation was successful");
                    this.state = FileState.PRESYNC; stateChange = true;
                break;
                
                case 'N': 
                    System.out.println("Autentication wasnt successful: The code was wrong.");
                    this.state = FileState.PREAUTICATION; stateChange = true;
                break;
            }
        }catch(Exception e){}
        finally{
            if (stateChange) c.signalAll();
            l.unlock(); 
        }
    
    }


	public void validateCode(byte[] data, int length) {
        boolean stateChange = false;
        try{
            l.lock();
            String type = new String(data,4,length-4);
            if(type.equals(code)) codeValidation = true;
            else codeValidation = false;
            if (codeValidation){
                if (this.state == FileState.SENDINGANSWER) {this.state = FileState.SENDINGCODE; stateChange = true;}
                else{ this.state = FileState.VALIDATECODE; stateChange = true; } 
            }else {this.state = FileState.VALIDATECODE; stateChange = true;}
            
        }finally{
            if (stateChange) c.signalAll();
            l.unlock();         
        }
    }

    //

    public byte[] getQuestion() {
        ByteBuffer m = ByteBuffer.allocate(6);
        m = m.putChar('A').putChar('U').putChar('Q'); 
        return m.array();
    }

    public byte[] getAnswer(){
        ByteBuffer m = ByteBuffer.allocate(6);
        m = m.putChar('A').putChar('U').putChar('A');
        return m.array();
	}
    
    public byte[] getCodeTry(){
        System.out.println("Whats the other machine code");
        String message = System.console().readLine();
        ByteBuffer m = ByteBuffer.allocate(message.length() + 4);
        m = m.putChar('P').putChar('S').put(message.getBytes());
        return m.array();   
    }
    
    public byte[] getValidationQuestion(){
        ByteBuffer m = ByteBuffer.allocate(6);
        m = m.putChar('A').putChar('U');
        if(codeValidation){ 
            m = m.putChar('C');
            System.out.println("Autencation was successful");
        }
        else{ 
            m = m.putChar('N');
            System.out.println("Autentication wasnt successful: The code was wrong.");
        }
        return m.array();
    }

    //
    
    public void gotPr(byte[] data, int length) {
        boolean stateChange = false;
        ByteBuffer m = ByteBuffer.wrap(data);
        //System.out.println(length);
        if (length == 12){
            long timer = m.getLong(4);
            try{
                l.lock();
                this.timerCmp = (int) (timer - this.timer); this.state = FileState.TESTTIME; stateChange = true; //recived - this, >=0 -> Send,  <0 -> Recive  
            }finally{ 
                if (stateChange) c.signalAll();
                l.unlock(); 
            }    
        }else {
            char type = m.getChar(4);
            try{
                l.lock();
                if (type == 'R'){
                    System.out.println("A transfer has started"); 
                    this.getAllFiles();
                    this.state = FileState.SENDINGAV; stateChange = true;
                }
                else {
                    this.state = FileState.RECIVINGAV; stateChange = true;
                    FileStat.clearDir(pasta);
                }
            }finally{ 
                if (stateChange) c.signalAll();
                l.unlock(); 
           }
        }   
    }   

    public byte[] presync() {
        ByteBuffer m = ByteBuffer.allocate(12);
        m = m.putChar('P').putChar('R').putLong(timer);
        return m.array();
    }

    public byte[] testtimer() {
        boolean stateChange = false;
        ByteBuffer m = ByteBuffer.allocate(6);
        m = m.putChar('P').putChar('R');
        try{
            l.lock();
            if (timerCmp >= 0){
                m = m.putChar('S'); 
                this.getAllFiles();
                System.out.println("A transfer has started");
                this.state = FileState.SENDINGAV; stateChange = true;
            } else {
                m.putChar('R');
                FileStat.clearDir(pasta);
            }
        }finally{ 
            if (stateChange) c.signalAll();
            l.unlock(); 
        } 
        return m.array();
    }

    //
    
    public void gotLocalChange(List<String> s){
        boolean stateChange = false;
        try{
            l.lock();
            if (this.state != FileState.SYNC) return;
            System.out.println("A transfer has started");
            this.clearEverything();
            state = FileState.SENDINGAV; stateChange = true;
            for(int i =0; i < s.size();i++){
                String w = s.get(i);
                FileStat f = FileStat.fileStatIn(pasta ,w);
                updateFiles.put(i,f); filesID.add(i);
            }
        }
        finally{ 
            if (stateChange) c.signalAll();
            l.unlock(); 
        }   
    }

    //

    public void gotAV(byte[] message, int length){
        ByteBuffer w = ByteBuffer.wrap(message);
        int id = w.getInt(4),total = w.getInt(8);
        String name = new String(message,12,length-12);
        //System.out.println(name + " " + id + " " + total);
        FileStat stat = FileStat.fileStatOut(pasta,name,total);
        boolean stateChange = false;  
        try{
            l.lock();
            if (counterAV == 0) System.out.println("A transfer has started");
            if (this.state == FileState.PRESYNC) FileStat.clearDir(pasta);
            
            if (id == -1 &&  total == -1 && this.state == FileState.RECIVINGAV) {state = FileState.OKAV; stateChange = true; return;}
            
            state = FileState.RECIVINGAV; stateChange = true;
            
            if (id != -1 && total == -1) {counterAV += 1; FileStat.removeFile(pasta + name); return;} 
            else{ 
                if (id == counterAV){ 
                    counterAV += 1;
                    //System.out.println("got AV ID " + id + " counter is " + counterAV); 
                    updateFiles.put(id, stat);
                    if(total == 0) { filesIDEnded.add(id); }
                }
                else if (id > counterAV) { /*System.out.println("AV IS BAD");*/ state = FileState.ERRORAV; stateChange = true; }

            }
        }finally{ 
            //System.out.println("MAP SIZE" + updateFiles.size());
            if(stateChange) c.signalAll();
            l.unlock(); 
        }
    }

    public int gotOK(byte[] message, int length){
        boolean stateChange = false;
        try{ 
            l.lock();
            String type = new String(message,4,4,"UTF-16");
            switch(type){
                case "AV":
                    int avs = ByteBuffer.wrap(message).getInt(8);
                    if(avs >= counterAV ){ state = FileState.SENDINGFL; stateChange = true;} 
                    else{ 
                        //System.out.println("AV WAS NOT TOTTALY TRANSFER : " + avs);
                        state = FileState.SENDINGAV; counterAV = avs; stateChange = true;
                    }
                    return 1;
                case "FL":
                    state = FileState.SYNC; stateChange = true;
                    System.out.println("A transfer has ended");
                    this.clearEverything();
                    return 0;
            }
        }catch(Exception e){}
        finally{ 
            if(stateChange) c.signalAll();
            l.unlock(); 
        }
        return 0;
    }

    public void gotError (byte[] message, int length){
        ByteBuffer m = ByteBuffer.wrap(message);
        int id = m.getInt(8);
        boolean stateChange = false;
        try{ 
            String type = new String(message,4,4,"UTF-16");
            l.lock();
            //System.out.println(type);
            switch(type){
                case "AV":
                    //System.out.println("ERROR WAS DETECET AV: " + id);
                    state = FileState.SENDINGAV; stateChange = true;
                    counterAV = id;
                    //System.out.println("COUNTERAV IS NOW " + counterAV);
                    //Thread.sleep(1000);
                break;
                case "FL":
                    int part = m.getInt(12);
                    //System.out.println("ERROR WAS DETECTED FL: " + id + " " + part);
                    state = FileState.SENDINGFL; stateChange = true;
                
                    if (errors < maxPermErrors) errors += 1;
                    if (errors == maxPermErrors) windowMax = filesID.size();

                    FileStat f = updateFiles.get(id);
                    
                    //System.out.println(" total " + f.getTotal());
                    //Thread.sleep(1000);
                    
                    f.resetInput(pasta,part);
                    filesIDEnded.remove((Integer) (id));
                
                    break;
            }

        }catch(Exception e){}
        finally{ 
            if(stateChange) c.signalAll();
            l.unlock(); 
        }
    }

    //

    public byte[] errorStateAV(){
        ByteBuffer m = ByteBuffer.allocate(12);
        m = m.putChar('E').putChar('R').putChar('A').putChar('V').putInt(counterAV);
        return m.array();
    }

    public byte[] errorStateFL(){
        ByteBuffer m = ByteBuffer.allocate(16);
        int id = 0, part = 0; 
        try {
            l.lock();
            id = errorsID.get(0);
            FileStat f = updateFiles.get(id);
            part = f.getParts();
        }
        finally {l.unlock();}
        m = m.putChar('E').putChar('R').putChar('F').putChar('L').putInt(id).putInt(part);
        return m.array();
    }

    //

    public byte[] finishSendAV(){
        boolean stateChange = false;
        try{ 
            l.lock();
            state = FileState.RECIVINGFL; stateChange = true; 
        }catch (Exception e) {}
        finally{ 
            if(stateChange) c.signalAll();
            l.unlock(); 
        }
        //System.out.println("AV FINISHED COUNTER IS " + counterAV);
        ByteBuffer m = ByteBuffer.allocate(12);
        m = m.putChar('O').putChar('K').putChar('A').putChar('V').putInt(counterAV); 
        return m.array();
    }

    public byte[] finishSendFL(){
        boolean stateChange = false;
        try{ 
            l.lock();
            System.out.println("A transfer has ended");
            state = FileState.SYNC; stateChange = true;
            this.clearEverything();
        }
        finally{ 
            if(stateChange) c.signalAll(); 
            l.unlock(); 
        }
        ByteBuffer m = ByteBuffer.allocate(8);
        m = m.putChar('O').putChar('K').putChar('F').putChar('L');
        return m.array();
    }

    //

    public byte[] getAV(){
        int id = 0, total = 0;
        byte[] message = new byte[FileStat.MAX_SIZE-12];  
        boolean stateChange = false;
        try{
            l.lock();
            //System.out.println("ONE AV:");
            //System.out.println (counterAV);
            //System.out.println (filesID.size());

            if (counterAV >= filesID.size()){ this.state = FileState.WAITOKAV; stateChange = true; return null; }
            id = filesID.get(counterAV); counterAV += 1;
            FileStat f = updateFiles.get(id);
            message = f.getNameAsBytes();
            total = f.getTotal();

            //System.out.println(f.getName());
        } finally{ 
            if(stateChange) c.signalAll();
            l.unlock(); 
        }
        ByteBuffer mBuffer = ByteBuffer.allocate(message.length + 12);
        mBuffer = mBuffer.putChar('A').putChar('V').putInt(id).putInt(total).put(message,0,message.length);
        return mBuffer.array();
    }

    public byte[] getAVEnd(){
        ByteBuffer mBuffer = ByteBuffer.allocate(12);
        mBuffer = mBuffer.putChar('A').putChar('V').putInt(-1).putInt(-1);
        return mBuffer.array();
    }
 
    //

    public byte[] readBytes() {
        int id = 0, parts = 0 , read = 0;
        byte[] message = new byte[FileStat.MAX_SIZE-12];
        boolean stateChange = false; 
        try{
            l.lock();
            id = filesID.get(counterFL);
            counterFL = (counterFL + 1) % filesID.size();
            //if (filesIDEnded.contains((Integer) id)) return null;
            FileStat f = updateFiles.get(id); 
            read = f.readByte(message);
            //System.out.println(read);
            if (read == -1 || read == 0){
                if (!filesIDEnded.contains(id)) filesIDEnded.add(id);
                if (filesID.size() == filesIDEnded.size()){ this.state = FileState.WAITOKFL; stateChange = true;}
                return null;
            }
            parts = f.getParts();
        } catch (IndexOutOfBoundsException e){
            id = -2; parts = -2;
            this.state = FileState.WAITOKFL; stateChange = true;
        }finally{ 
            if(stateChange) c.signalAll();
            l.unlock(); 
        }
        //System.out.println(id + " " + parts + " " + read + " " /*+  new String(message,0,read)*/);
        ByteBuffer mBuffer = ByteBuffer.allocate(read + 12);
        mBuffer = mBuffer.putChar('F').putChar('L').putInt(id).putInt(parts).put(message,0,read);
        return mBuffer.array();
    }

    public byte[] getFLEnd() {
        ByteBuffer mBuffer = ByteBuffer.allocate(12);
        mBuffer = mBuffer.putChar('F').putChar('L').putInt(-1).putInt(-1);
        return mBuffer.array();
    }

    //

    public int writeByte(byte[] data, int length) {
        ByteBuffer message = ByteBuffer.wrap(data);
        message.getChar();message.getChar();
        int id = message.getInt();
        int part = message.getInt();
        byte[] m = new byte[length - 12];
        message = message.get(m);
        //System.out.println(id + " " + part);
        boolean stateChange = false;
        try{
            l.lock();
            
            if (part == -2 && id == -2){
                FileStat.clearDir(pasta);
                this.state = FileState.OKFL; stateChange = true;
                return 0; 
            }
            
            if (part == -1 && id == -1){
                if (updateFiles.size() == filesIDEnded.size()){
                    this.state = FileState.OKFL; stateChange = true;
                    return 0; 
                }else {
                    this.state = FileState.ERRORFL; stateChange = true;
                    errorsID.addAll(this.getUnfinished());
                    return 1;
                }
            }
            
            FileStat f = updateFiles.get(id);
            
            if (errorsID.contains(id)){
                if (f.writeByte(part, m)) {errorsID.remove((Integer) id);}
                if (errorsID.isEmpty()) {this.state = FileState.RECIVINGFL; stateChange = true;}
            } else if (!f.writeByte(part,m)) { 
                state = FileState.ERRORFL; stateChange = true;
                errorsID.add((Integer) id);
            }

            if(f.getParts() == f.getTotal() ) {filesIDEnded.add(id);}

        }finally{ 
            if(stateChange) c.signalAll();
            l.unlock(); 
        }
        return 1;
    }

    //

    public long getSendingTimer() {
        try{
            l.lock();
            if (windowMax == 0) return 0;
            //System.out.println("Window " + windowMax);
            return this.getWindowTimer();
        }finally{
            l.unlock();
        }   
    }

    private long getWindowTimer() {
        return 50;
    }

    //

    private Collection<Integer> getUnfinished() {
        return updateFiles.keySet().stream().filter((x) -> (!filesIDEnded.contains(x))).collect(Collectors.toList());
    }

    private void clearEverything() {
        this.counterAV = 0;
        this.counterFL = 0;
        this.windowMax = 0;
        this.errors = 0;
        this.timerCmp = 0;
        this.errorsID.clear();
        this.filesID.clear();
        this.filesIDEnded.clear();
        this.updateFiles.clear();
    }

    public void reset() {
        boolean stateChange = false;
        try{ 
            l.lock();
            this.clearEverything();
            this.state = initState; stateChange = true;
        }  
        finally{ 
            if(stateChange) c.signalAll();
            l.unlock();
        }
    }

    private void getAllFiles() {
        try{
            l.lock();
            File dir = new File(pasta);
            String[] list = dir.list();
            for(int i = 0; i < list.length; i++){
                //System.out.println(list[i]);
                FileStat f = FileStat.fileStatIn(pasta ,("/"+list[i]));
                updateFiles.putIfAbsent(i, f);
                if (!filesID.contains(i))filesID.add(i);
            }
        }finally{l.unlock();}
    }

}