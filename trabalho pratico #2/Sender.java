import java.net.*;

public class Sender implements Runnable{

    private final int auTime = 5000;
    private final int transferTimeout = 1500;
    private final int errorTimeout = 1000;
    private DatagramSocket socket = null;
    private InetAddress ip = null;
    private DirController dir = null;

    public Sender(DatagramSocket socket,InetAddress ip, DirController dir) {
        this.socket = socket;
        this.ip = ip;
        this.dir = dir;
    }

    @Override
    public void run() {
        byte[] message = new byte[FileStat.MAX_SIZE];
        DatagramPacket packet = new DatagramPacket(message, message.length,ip,socket.getLocalPort());
        int interruptedCon = 0;
        FileState last = DirController.initState;
        
        while(true){
            if (interruptedCon == 5){
                System.out.println("Sender : The connection was Lost!!");
                dir.reset(); interruptedCon = 0;
            }
            
            try {
                //System.out.println("Sender : waiting");
                FileState state = dir.isUpdate();
                
                switch(state){
                    case PREAUTICATION:
                        //System.out.println("Sender : Sending a Question.");
                        message = dir.getQuestion();
                        if (message != null){
                            packet.setData(message, 0, message.length);
                            socket.send(packet);
                        }
                        //System.out.println("Sender : going to wait");
                        Thread.sleep(auTime);
                    break;
                    
                    case SENDINGANSWER:
                        //System.out.println("Sender : sending Answer code");
                        message = dir.getAnswer();
                        if (message != null){
                            packet.setData(message, 0, message.length);
                            socket.send(packet);
                        }
                        //System.out.println("Sender : going to wait");
                        Thread.sleep(auTime);
                    break;
                    
                    case SENDINGCODE:
                        message = dir.getCodeTry();
                        //System.out.println("Sender : Sending the Code.");
                        if (message != null){
                            packet.setData(message, 0, message.length);
                            socket.send(packet);
                        }
                        //System.out.println("Sender : going to wait");
                        Thread.sleep(auTime);
                    break;
                    
                    case VALIDATECODE:
                        message = dir.getValidationQuestion();
                        //System.out.println("Sender : Sending the Validation.");
                        if (message != null){
                            packet.setData(message, 0, message.length);
                            socket.send(packet);
                        }
                        //System.out.println("Sender : going to wait");
                        Thread.sleep(auTime);
                    break;

                    case PRESYNC:
                        //System.out.println("Sender : sending presync timer");
                        message = dir.presync();
                        if (message != null){
                            packet.setData(message, 0, message.length);
                            socket.send(packet);
                        }
                        Thread.sleep(auTime);
                    break;
                    
                    case TESTTIME:
                        //System.out.println("Sender : sender presync timer");
                        message = dir.testtimer();
                        if (message != null){
                            packet.setData(message, 0, message.length);
                            socket.send(packet);
                        }
                        Thread.sleep(transferTimeout);
                    break;

                    case SENDINGAV:
                        //System.out.println("Sender : enter Sending AV");
                        message = dir.getAV();
                        //System.out.println("Sender : Sending AV");
                        if (message != null){
                            packet.setData(message, 0, message.length);
                            socket.send(packet);
                        }
                        break;

                    case WAITOKAV:
                        //System.out.println("Sender : enter Wait For AV ");
                        message = dir.getAVEnd();
                        //System.out.println("Sender : Sending AV -1 -1");
                        if (message != null){
                            packet.setData(message, 0, message.length);
                            socket.send(packet);
                            Thread.sleep(transferTimeout);
                        }
                        break;

                    case RECIVINGAV:    
                        //System.out.println("Sender : enter Reciving AV");
                        state = dir.waitRecivingAV();
                        //System.out.println("Sender : Reciving Event in AV");
                        break;

                    case OKAV:
                        message = dir.finishSendAV();
                        if (message != null){
                            packet.setData(message, 0, message.length);
                            socket.send(packet);
                        }
                        break;

                    case SENDINGFL: 
                        message = dir.readBytes();
                        if (message != null){
                            //System.out.println("Sender : Sending File Part");
                            packet.setData(message, 0, message.length);
                            socket.send(packet);
                        }
                        break;

                    case WAITOKFL:
                        //System.out.println("Sender : enter Wait For FL");
                        message = dir.getFLEnd();
                        //System.out.println("Sender : Sending FL -1 -1");
                        if (message != null){
                            packet.setData(message, 0, message.length);
                            socket.send(packet);
                            Thread.sleep(transferTimeout);
                        }
                        break;
                    
                    case RECIVINGFL: 
                        //System.out.println("Sender : enter Reciving File");
                        state = dir.waitRecivingFL();
                        //System.out.println("Sender : Reciving Event in File");
                        break;
                    
                    case OKFL: 
                        message = dir.finishSendFL();
                        if (message != null){
                            packet.setData(message, 0, message.length);
                            socket.send(packet);
                        }
                        break;
                    
                    case ERRORAV:
                        message = dir.errorStateAV();
                        if (message != null){
                            //System.out.println("Sender: Error AV");
                            packet.setData(message, 0, message.length);
                            socket.send(packet);
                            Thread.sleep(errorTimeout);
                        }
                        break;

                    case ERRORFL:
                        message = dir.errorStateFL();
                        if (message != null){
                            //System.out.println("Sender: Error FL");
                            packet.setData(message, 0, message.length);
                            socket.send(packet);
                            Thread.sleep(errorTimeout);
                        }
                        break;
                    
                    default: break;
                }
                
                
                Thread.sleep(dir.getSendingTimer());
                
                if (last == state){
                    if (FileState.interrupted(state)) interruptedCon += 1;
                } else{ 
                    interruptedCon = 0;
                    last = state;
                } 

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }    

} 