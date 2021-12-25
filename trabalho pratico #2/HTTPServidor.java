

import java.io.*;
import java.net.*;
import com.sun.net.httpserver.*;

/* 
root@Servidor1:/home/simao/Desktop/Projeto-CC# javac HTTPServer.java 
root@Servidor1:/home/simao/Desktop/Projeto-CC# java HTTPServer 

root@Orca:/tmp/pycore.45923/Orca.conf# wget http://10.2.2.1/

*/
public class HTTPServidor implements Runnable {

    private HttpServer httpS;
    private InetAddress ipReceiver;
    private DirController dc;

    public HTTPServidor (DirController dc,InetAddress ipReceiver) throws IOException{
        this.ipReceiver = ipReceiver;
        this.dc = dc;
        this.httpS = HttpServer.create(new InetSocketAddress("0.0.0.0", 80), 80); 
    }

    public void run() {
        httpS.createContext("/", new MyHttpHandler(dc,ipReceiver));
        httpS.setExecutor(null);
        httpS.start();
    }
}
