import java.net.*;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.io.*;


/**
 * Created by Anastasia on 21.11.16.
 */
public class Node {

    private static final int MAXSEND = 5;
    private static final int TIMEOUT = 1000 / 3;
    private static final int SIZE = 1024;
    private static final int TIMESEND = 5000;

    private DatagramSocket mes;
    private String name;
    private int lossPercentage;
    private Random random;
    private int port;
    private Param parent;

    private List<Param> child;

    private Map<Message, Long> waitMessage;
    private Queue<Message> allMes;

    private Node(InetAddress ipParent, int portParent, String name, int port, int lossPercentage){
        this.name = name + ": ";
        this.parent = new Param(ipParent, portParent);
        this.port = port;
        this.mes = null;
        this.lossPercentage = lossPercentage;
        child = new ArrayList<>();
        waitMessage = new HashMap<>();
        allMes = new LinkedList<>();
        random = new Random(System.currentTimeMillis());
    }

    private Node(String name, int port, int lossPercentage){
        this.name = name + ": ";
        this.parent = null;
        this.port = port;
        this.mes = null;
        this.lossPercentage = lossPercentage;
        child = new ArrayList<>();
        waitMessage = new HashMap<>();
        allMes = new LinkedList<>();
        random = new Random(System.currentTimeMillis());
    }

    public void mainFunction() throws IOException {

        try {
            mes = new DatagramSocket(port);
            mes.setSoTimeout(TIMEOUT);
        } catch (SocketException e){
            System.err.println(e.toString());
            System.err.println("Error start new vertex to port " + port);
            return;
        }

        connectNode(mes);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        DatagramPacket packet = new DatagramPacket(new byte[SIZE], SIZE);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run(){
                System.out.println("Died");
                waitMessage.clear();
                if(parent == null){
                    if(child.size() > 0){
                        parent = child.get(0);
                    }
                    else{
                        return;
                    }
                }
                else{
                    sendMessage(new Message(new byte[0], Message.typeMessenge.DEAD, parent));
                }
                ByteBuffer buffer = ByteBuffer.allocate(parent.getIp().getAddress().length + Integer.BYTES);
                buffer.putInt(parent.getPort()).put(parent.getIp().getAddress()).flip();
                Message messageNewParent = new Message(buffer.array(), Message.typeMessenge.PARENT);
                for (Param param: child){
                    if(!param.equals(parent)){
                        sendMessage(new Message(messageNewParent.getMes(), messageNewParent.getType(), param));
                    }

                }
                DatagramPacket packet1 = new DatagramPacket(new byte[Message.SIZEMIN + Long.BYTES], Message.SIZEMIN + Long.BYTES);
                while(waitMessage.size() > 0){
                    try {
                        mes.receive(packet1);
                        Message message = new Message(packet1);
                        if(message.getType() == Message.typeMessenge.ANSWER) {
                            receiveAnswer(message);
                        }
                    }catch (SocketTimeoutException e) {
                        // вышли по таймауту
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                    repeatSend();
                }
            }
        });
        for( ; ; ){
            if(bufferedReader.ready()){
                sendAll(new Message((name + bufferedReader.readLine()).getBytes(Charset.forName("UTF-8")), Message.typeMessenge.MESSENG));
            }
            try {
                mes.receive(packet);
                if(random.nextInt(100) >= lossPercentage) {
                    Message newMes = new Message(packet);
                    recvMesseng(newMes);
                    sendAnswer(newMes);
                }
            }catch (SocketTimeoutException e){
                //вышли по таймауту
            }

            repeatSend();
        }

    }

    public boolean connectNode(DatagramSocket mes) throws IOException {

        if (parent == null) {
            return true;
        }
        boolean connected = false;
        Message messageConnect = new Message(new byte[0], Message.typeMessenge.CONNECT, parent);
        sendMessage(messageConnect);

        DatagramPacket pack = new DatagramPacket(new byte[Message.SIZEMIN + Long.BYTES], Message.SIZEMIN + Long.BYTES);
        for (int i = 0; i < MAXSEND; ++i) {
            try {
                mes.receive(pack);
                if (pack.getAddress().equals(parent.getIp()) && pack.getPort() == parent.getPort()) {
                    Message messageAnswer = new Message(pack);
                    if (messageAnswer.getType() == Message.typeMessenge.ANSWER) {
                        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
                        byteBuffer.put(messageAnswer.getMes());
                        byteBuffer.flip();
                        if (byteBuffer.getLong() == waitMessage.keySet().iterator().next().getGuid()) {
                            connected = true;
                            waitMessage.clear();
                            break;
                        }
                    }
                }
            } catch (SocketTimeoutException e){
                repeatSend();
            }
        }
        return connected;

    }

    public void sendAll(Message message){
        if(message.getType() == Message.typeMessenge.ANSWER){
            return;
        }

        if(parent != null){
            if(!parent.equals(message.getAddress())){
                sendMessage(new Message(message.getMes(), message.getType(), parent));
            }
        }
        for (Param param: child){
            if(!param.equals(message.getAddress())){
                sendMessage(new Message(message.getMes(), message.getType(), param));
            }
        }
    }

    public void sendAnswer(Message message){
        if (message.getType() == Message.typeMessenge.ANSWER) {
            return;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
        byteBuffer.putLong(message.getGuid());
        byteBuffer.flip();
        Message messageAnswer = new Message(byteBuffer.array(), Message.typeMessenge.ANSWER, message.getAddress());
        sendMessage(messageAnswer);
    }

    public void sendMessage(Message message){
        try {
            mes.send(message.getMessenge());
        } catch (IOException e) {
            e.printStackTrace();
        }
        message.setCount(message.getCount() + 1);
        if (message.getType() != Message.typeMessenge.ANSWER) {
            if (waitMessage.size() <= child.size() * MAXSEND) {
                waitMessage.put(message, System.currentTimeMillis());
            }
        }
    }

    public void recvMesseng(Message message) throws IOException {
        ByteBuffer byteBuffer;
        switch (message.getType()){
            case Message.typeMessenge.ANSWER:{
                receiveAnswer(message);
                break;
            }
            case Message.typeMessenge.MESSENG:{
                if(!allMes.contains(message)){
                    System.out.println(new String(message.getMes(), Charset.forName("UTF-8")));
                    allMes.add(message);
                    sendAll(message);
                }
                else{
                    System.out.println("Повторная отпраvка");
                }

                break;
            }
            case Message.typeMessenge.DEAD:{
                if (child.contains(message.getAddress())) {
                    System.err.println("dead children: " + message.getAddress().toString());
                    child.remove(message.getAddress());
                }
                break;
            }
            case Message.typeMessenge.PARENT:{

                byteBuffer = ByteBuffer.allocate(Integer.BYTES + 4);
                byteBuffer.put(message.getMes());
                byteBuffer.flip();
                int newPortParent = byteBuffer.getInt();
                byte[] newInetAddressParentByte = new byte[message.getMes().length - Integer.BYTES];
                byteBuffer.get(newInetAddressParentByte, 0, message.getMes().length - Integer.BYTES);
                try {
                    InetAddress newInetAddressParent = InetAddress.getByAddress(newInetAddressParentByte);
                    parent = new Param(newInetAddressParent, newPortParent);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    parent = null;
                }

                if (parent != null) {
                    if (parent.getIp().equals(InetAddress.getLocalHost()) && parent.getPort() == port) {
                        parent = null;
                        System.err.println("You become the head of the tree!!!");
                        break;
                    }
                    Message messageConnect = new Message(new byte[0], Message.typeMessenge.CONNECT, parent);
                    sendMessage(messageConnect);
                }
                if (parent != null) {
                    System.err.println("New parent " + parent);
                } else {
                    System.err.println("You become the head of the tree!!!");
                }

                break;
            }
            case Message.typeMessenge.CONNECT:{
                if (!child.contains(message.getAddress())) {
                    child.add(message.getAddress());
                }
            }

        }

    }

    private void repeatSend() {
        for (Iterator<Map.Entry<Message, Long>> it = waitMessage.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Message, Long> entry = it.next();
            if (System.currentTimeMillis() - entry.getValue() > TIMESEND) {
                sendMessage(entry.getKey());
                if (entry.getKey().getCount() >= MAXSEND) {
                    if (entry.getKey().getType() == Message.typeMessenge.CONNECT) {
                        parent = null;
                    }
                    it.remove();
                }
            }
        }
    }

    private void receiveAnswer(Message message) {
        /* Убирает из очереди на ожидание сообщение, ответ на который пришёл с помощью message
        * */
        if (message.getType() == Message.typeMessenge.ANSWER) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
            byteBuffer.put(message.getMes());
            byteBuffer.flip();
            long guidAnswer = byteBuffer.getLong();
            for (Iterator<Map.Entry<Message, Long>> it = waitMessage.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Message, Long> entry = it.next();
                if (entry.getKey().getGuid() == guidAnswer && entry.getKey().getAddress().equals(message.getAddress())) {
                    it.remove();
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        Node node;
        try {
            node = new Node("Node-Parent", 10000, 0);
            node = new Node(InetAddress.getLocalHost(), 10000, "Node-1", 10001, 0);
            node = new Node(InetAddress.getLocalHost(), 10000, "Node-2", 10002, 0);
            node = new Node(InetAddress.getLocalHost(), 10001, "Node-3", 10003, 0);
            //node = new Node(InetAddress.getLocalHost(), 10001, "Node-4", 10004, 10);
            //node = new Node(InetAddress.getLocalHost(), 10002, "Node-5", 10005, 10);
            //node = new Node(InetAddress.getLocalHost(), 10004, "Node-6", 10006, 10);
            //node = new Node(InetAddress.getLocalHost(), 10003, "Node-7", 10007, 10);
            node.mainFunction();
        } catch (IOException e) {
            System.err.println(e.toString());
            System.exit(-1);
        }
    }

}

//не будет ли повторных пересылок сообщений
//не будет ли повторно добавляться ребенок