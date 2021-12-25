import java.net.*;

public class Reciver implements Runnable{
    
    private DatagramSocket socket = null;
    private DirController dir = null;
    private InetAddress ip = null;


    public Reciver(DatagramSocket socket,InetAddress ip,DirController dir){
        this.socket = socket;
        this.ip = ip;
        this.dir = dir; 
    }

    @Override
    public void run() {
        byte[] message = new byte[FileStat.MAX_SIZE];
        DatagramPacket packet = new DatagramPacket(message, message.length,ip,socket.getLocalPort());
        String type = null; int timeOut = 0;
        while(true){
            try{
                //System.out.println("Reciver : waiting");
                socket.receive(packet);               
                //System.out.println("Reciver : got it");
                type = new String(packet.getData(),0,4,"UTF-16");
                //System.out.println("Reciver : message type " + type );
                switch(type){
                    
                    //Autentiaction Messages
                    case "AU": timeOut = 1; dir.gotAu(packet.getData(),packet.getLength()); break;
                    
                    //Code Messages
                    case "PS": timeOut = 1; dir.validateCode(packet.getData(),packet.getLength()); break;
                    
                    //PreSync Messages
                    case "PR": timeOut = 1; dir.gotPr(packet.getData(),packet.getLength()); break;
                    
                    //File Warnings Messages
                    case "AV": timeOut = 1; dir.gotAV(packet.getData(),packet.getLength()); break;
                    
                    //Files Messages
                    case "FL": timeOut = dir.writeByte(packet.getData(),packet.getLength()); break;     
                    
                    //Error Messages
                    case "ER": timeOut = 0; dir.gotError(packet.getData(),packet.getLength()); break;
                    
                    //Files OK or Warning OK Messages
                    case "OK": timeOut = dir.gotOK(packet.getData(),packet.getLength()); break;
                
                }
            }catch(SocketTimeoutException e){
                if (timeOut == 1){
                    timeOut = 0;
                    System.out.println("Reciver : The connection was Lost!!"); dir.reset();
                }              
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    
}
