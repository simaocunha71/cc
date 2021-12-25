import java.io.Console;
import java.net.*;

public class Fast_Sync{

    private static final int port = 8888;
    public static void main(String[] args) {
        if (args.length <= 1) { 
            System.out.println("Needs two arguments: the folder to sync and the other machine's ip;"); 
            return;}
        
        DatagramSocket socket = null;
        InetAddress ip = null; 
        DirWatcher dirWatch = null;
        Logs logs = null;

        System.out.println("Defina Palavra Passe para a coneção: ");
        Console read = System.console();
        String passeCode = read.readLine();
        //System.out.println(System.currentTimeMillis());
        DirController dirContr =  new DirController(args[0],passeCode, System.currentTimeMillis());
        Sender send; Reciver recive;
        HTTPServidor httpsv;
        try{
            socket = new DatagramSocket(port);
            ip = InetAddress.getByName(args[1]);
            dirWatch = new DirWatcher(args[0],dirContr);
            httpsv = new HTTPServidor(dirContr, ip); 
            logs = new Logs(dirContr,ip.getHostAddress());
            socket.setSoTimeout(30000);
        }catch (Exception e){
            e.printStackTrace();
           // System.out.println("Error starting");
            return;
        }
        
        send = new Sender(socket,ip,dirContr);
        recive = new Reciver(socket,ip,dirContr);
        
        Thread[] threads = new Thread[5];
        threads[0] = new Thread(dirWatch); threads[1] = new Thread(send); threads[2] = new Thread(recive); threads[3] = new Thread(logs);threads[4] = new Thread(httpsv);
        threads[0].start(); threads[1].start(); threads[2].start();threads[3].start();threads[4].start();
    }
}