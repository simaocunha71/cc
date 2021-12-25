import java.io.*;
import java.net.InetAddress;

import com.sun.net.httpserver.*;

public class MyHttpHandler implements HttpHandler {  
    DirController dc;
    InetAddress ipReceiver;

    public MyHttpHandler(DirController dc,InetAddress ipReceiver){
      this.dc = dc;
      this.ipReceiver = ipReceiver;
    }

    @Override
  public void handle(HttpExchange httpExchange) throws IOException{
    if("GET".equals(httpExchange.getRequestMethod())) {
       handleResponse(httpExchange); 
    }
  }

      private void handleResponse(HttpExchange httpExchange)  throws  IOException {
        OutputStream outputStream = httpExchange.getResponseBody();
        BufferedOutputStream outFile = new BufferedOutputStream(outputStream);
        StringBuilder htmlBuilder = new StringBuilder();

           
        htmlBuilder.append("<!DOCTYPE html>")
                   .append("<html>")
                   .append("<body>")
                   //append("<h1>Trabalho prático 2 - FolderFastSync</h1>")
                   //append("<h1>PL1 - Grupo 6</h1><hr>")
                   //append("<h2>Alexandre Soares (a93267) | Pedro Sousa (a93225) | Simão Cunha (a93262)</h2><hr>")
                   .append("<h1>").append("Estado da aplicacao | Pasta a sincronizar | Host | Peer ").append("</h1>")
                   .append("<p>").append(dc.getFileState().toString())
                   .append(" | ").append(dc.getPasta())
                   .append(" | ").append(InetAddress.getLocalHost().toString())
                   .append(" | ").append(ipReceiver.toString())
                   .append("</p>")
                   .append("</body>")
                   .append("</html>");
          
        String htmlResponse = htmlBuilder.toString();
        httpExchange.sendResponseHeaders(200, htmlResponse.length());
        outFile.write(htmlResponse.getBytes());
        outFile.flush();
        outFile.close();
        
    }
} 