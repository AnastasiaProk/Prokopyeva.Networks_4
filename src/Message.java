import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created by Anastasia on 21.11.16.
 */
public class Message {

    private byte [] mes;
    private long guid;
    private int typeMessenge;
    private int count;
    private Param address;

    static final int SIZEMIN  = (Long.BYTES + Integer.BYTES);

    private static Random random;
    static {
        random = new Random(System.currentTimeMillis());
    }

    public Message(byte [] mes, int TYPE){
        this.guid = random.nextLong();
        this.count = 0;
        this.typeMessenge = TYPE;
        this.mes = mes.clone();
        this.address = null;
    }

    public Message(byte [] mes, int TYPE, Param address){
        this.guid = random.nextLong();
        this.count = 0;
        this.typeMessenge = TYPE;
        this.mes = mes.clone();
        this.address = address;
    }

    public Message(DatagramPacket messenge){
        this.count = 0;
        ByteBuffer byteBuffer = ByteBuffer.allocate(messenge.getData().length);
        byteBuffer.put(messenge.getData().clone());
        byteBuffer.flip();
        this.guid = byteBuffer.getLong();
        this.typeMessenge = byteBuffer.getInt();
        this.mes = new byte[messenge.getLength() - SIZEMIN];
        byteBuffer.get(this.mes);
        this.address = new Param(messenge.getAddress(), messenge.getPort());
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getGuid() {
        return guid;
    }

    public void setGuid(long guid) {
        this.guid = guid;
    }

    public byte[] getMes() {
        return mes;
    }

    public Param getAddress() {
        return address;
    }

    public DatagramPacket getMessenge(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(mes.length + SIZEMIN);
        byteBuffer.putLong(guid);
        byteBuffer.putInt(typeMessenge);
        byteBuffer.put(mes);
        byteBuffer.flip();
        if (address != null) {
            return new DatagramPacket(byteBuffer.array(), mes.length + SIZEMIN, address.getIp(), address.getPort());
        } else {
            return new DatagramPacket(byteBuffer.array(), mes.length + SIZEMIN);
        }
    }

    public int getType(){
        return typeMessenge;

    }

    public static class typeMessenge{

        public final static int MESSENG = 1;
        public final static int ANSWER = 2;
        public final static int DEAD = 3;
        public final static int PARENT = 4;
        public final static int CONNECT = 5;

    }


}
