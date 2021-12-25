import java.util.*;
import java.io.*;
import java.nio.file.*;

public class DirWatcher implements Runnable {
  private Path path; 
  private File filesArray [];
  private DirController dirc;
  private HashMap<File,Long> dir = new HashMap<>();
  private HashMap<File,Long> siz = new HashMap<>();


  public DirWatcher(String path,DirController dirc) {
    this.path = Paths.get(path);     
    this.filesArray = new File(path).listFiles();     
    this.dirc=dirc;     
    for(int i = 0; i < filesArray.length; i++) {       
      this.dir.put(filesArray[i], (Long)(filesArray[i].lastModified()));       
      this.siz.put(filesArray[i], (Long)(filesArray[i].length()));     
    } 
  }
  
  public void run(){
    WatchService watchService = null;
    WatchKey key;
    
    try{dirc.waitForSync();} catch(InterruptedException e){e.printStackTrace();}
    
    try{ 
      watchService = FileSystems.getDefault().newWatchService(); 

      path.register( watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
      
      while(true){
        while((key = watchService.take()) != null) {   
          if (!FileState.isReceving(dirc.getFileState()) ){
            try{Thread.sleep(10000);} catch(Exception e) {}
            List<String> cf = eventParse(key.pollEvents());
            if(!(cf.isEmpty())) dirc.gotLocalChange(cf);
          }
          
          dirc.waitForSync(); 
          key.cancel(); watchService.close();
          watchService = FileSystems.getDefault().newWatchService(); 
          path.register( watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        }
      }
    }catch (Exception e) {e.printStackTrace();}
  }
  
  public List<String> eventParse(List<WatchEvent<?>> events){
    ArrayList<String> changedFiles = new ArrayList<>();
    for(int i=0;i<events.size();i++){
      WatchEvent<?> event = events.get(i);
      String file = "/"+event.context().toString();
      if(!file.contains("goutputstream")){
        if(!changedFiles.contains(file)){changedFiles.add(file);}
      }
    }
    for(int i=0;i<events.size();i++){
      WatchEvent<?> event = events.get(i);
      if(event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)){
        for(int j=i;j<events.size();j++){
          WatchEvent<?> temp = events.get(j);
          if(temp.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)&&(event.context().equals(temp.context()))){changedFiles.remove("/"+event.context().toString());}
        }
      }
    }
    return changedFiles;
  }
}