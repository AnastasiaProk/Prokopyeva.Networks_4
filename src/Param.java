import java.net.InetAddress;

/**
 * Created by Anastasia on 23.11.16.
 */
public class Param {

    private InetAddress ip;
    private int port;

    public Param(InetAddress ip, int port){
        this.ip = ip;
        this.port = port;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void setIp(InetAddress ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public int hashCode() {
        return ip.hashCode() | port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Param other = (Param) obj;
        if (port != other.port)
            return false;
        if (!ip.equals(other.getIp()))
            return false;

        return true;
    }

    @Override
    public String toString() {
        return ip.toString() + " " + port;
    }

}
