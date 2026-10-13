package io.github.katakishi0.oberonextended.mobile;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal RFC6455 LAN WebSocket server for the original Xbox Oberon client.
 * No remote commands or HTTP endpoints; only the 0xFA input request is accepted.
 */
public final class OberonWebSocketServer {
    public interface Listener { void onStatus(String text,int clientCount); }
    private final InputState input;
    private final Listener listener;
    private final AtomicBoolean active=new AtomicBoolean(false);
    private volatile ServerSocket listenerSocket;
    private volatile Socket client;
    private volatile int connections;
    private volatile boolean lowLatencyTcp=true;
    public void setLowLatencyTcp(boolean enabled){lowLatencyTcp=enabled;}
    private volatile long packetsRequested;
    private volatile long packetsSent;
    private volatile double requestIntervalMs;
    private volatile long lastRequestNanos;
    private Thread thread;

    public OberonWebSocketServer(InputState input,Listener listener) {
        this.input=input;this.listener=listener;
    }
    public boolean isRunning(){return active.get();}
    public int clientCount(){return connections;}
    public long requested(){return packetsRequested;}
    public long sent(){return packetsSent;}
    /** Time since prior Xbox input request, NOT Xbox round-trip latency. */
    public double requestIntervalMs(){return requestIntervalMs;}
    public synchronized void start() throws IOException {
        if(active.get())return;
        ServerSocket server=new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress("0.0.0.0",OberonProtocol.PORT),2);
        listenerSocket=server;
        active.set(true);
        thread=new Thread(this::acceptLoop,"OberonMobile-WebSocket");
        thread.setDaemon(true);
        thread.start();
        notice("Waiting for Xbox");
    }
    public synchronized void stop(){
        active.set(false);
        closeQuietly(client);
        closeQuietly(listenerSocket);
        listenerSocket=null;client=null;
        connections=0;
        input.reset();
        lastRequestNanos=0;
        notice("Server stopped");
    }
    private void notice(String text){listener.onStatus(text,connections);}
    private void acceptLoop(){
        while(active.get()){
            try {
                Socket socket=listenerSocket.accept();
                socket.setTcpNoDelay(lowLatencyTcp);
                socket.setSoTimeout(6000);
                Socket previous=client;
                if(previous!=null)closeQuietly(previous);
                client=socket;
                handleClient(socket);
            } catch(IOException e){
                if(active.get())notice("Connection error: "+e.getMessage());
            } finally {
                connections=0;
                client=null;
                if(active.get())notice("Waiting for Xbox");
            }
        }
    }
    private void handleClient(Socket socket) throws IOException {
        try(socket){
            InputStream in=socket.getInputStream();
            OutputStream out=socket.getOutputStream();
            String headers=readHeaders(in);
            String key=null;
            String[] lines=headers.split("\\r\\n");
            if(lines.length==0 || !lines[0].startsWith("GET "))return;
            boolean upgraded=false;
            for(String line:lines){
                int colon=line.indexOf(':');
                if(colon<=0)continue;
                String field=line.substring(0,colon).trim().toLowerCase(Locale.ROOT);
                String value=line.substring(colon+1).trim();
                if(field.equals("sec-websocket-key"))key=value;
                if(field.equals("upgrade") && value.equalsIgnoreCase("websocket"))upgraded=true;
            }
            if(!upgraded || key==null || key.length()>100)return;
            String accept;
            try {
                byte[] sha1=MessageDigest.getInstance("SHA-1")
                    .digest((key+"258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.US_ASCII));
                accept=Base64.getEncoder().encodeToString(sha1);
            } catch(Exception e){throw new IOException("Cannot complete WebSocket handshake",e);}
            out.write(("HTTP/1.1 101 Switching Protocols\r\n"+
                "Upgrade: websocket\r\nConnection: Upgrade\r\n"+
                "Sec-WebSocket-Accept: "+accept+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
            connections=1;
            socket.setSoTimeout(15000);
            byte[] device="Oberon Extended Mobile".getBytes(StandardCharsets.UTF_8);
            byte[] hello=new byte[device.length+1];
            hello[0]=0x0A;System.arraycopy(device,0,hello,1,device.length);
            sendFrame(out,2,hello);
            notice("Xbox connected");
            while(active.get() && !socket.isClosed()){
                Frame frame=receiveFrame(in);
                if(frame==null)break;
                if(frame.opcode==8)break;
                if(frame.opcode==9){sendFrame(out,10,frame.payload);continue;}
                if(frame.opcode!=2)continue;
                if(frame.payload.length!=1 && frame.payload.length!=17)continue;
                if((frame.payload[0]&0xFF)!=0xFA)continue;
                long now=System.nanoTime();
                if(lastRequestNanos!=0){
                    double delta=(now-lastRequestNanos)/1000000.0;
                    if(delta>0 && delta<10000)
                        requestIntervalMs=requestIntervalMs==0?delta:
                            requestIntervalMs*.8+delta*.2;
                }
                lastRequestNanos=now;
                packetsRequested++;
                byte[] snapshot=OberonProtocol.encode(input.snapshot(),input.deadzone());
                sendFrame(out,2,snapshot);
                packetsSent++;
            }
        }
    }
    private static String readHeaders(InputStream in) throws IOException {
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        int state=0;
        while(b.size()<8192){
            int v=in.read();if(v<0)throw new EOFException();
            b.write(v);
            if((state==0||state==2)&&v==13)state++;
            else if((state==1||state==3)&&v==10)state++;
            else state=v==13?1:0;
            if(state==4)return new String(b.toByteArray(),StandardCharsets.US_ASCII);
        }
        throw new IOException("Request headers too long");
    }
    private static Frame receiveFrame(InputStream in) throws IOException {
        int first=in.read();if(first<0)return null;
        int second=in.read();if(second<0)throw new EOFException();
        int opcode=first&15;
        if((first&0x80)==0)throw new IOException("Fragmented WebSocket frame not supported");
        if((second&0x80)==0)throw new IOException("Unmasked client frame");
        long size=second&127;
        if(size==126)size=((long)readByte(in)<<8)|readByte(in);
        else if(size==127){size=0;for(int i=0;i<8;i++)size=(size<<8)|readByte(in);}
        if(size>4096 || size<0)throw new IOException("Oversized frame");
        byte[] mask=readFully(in,4), payload=readFully(in,(int)size);
        for(int i=0;i<payload.length;i++)payload[i]^=mask[i&3];
        return new Frame(opcode,payload);
    }
    private static int readByte(InputStream in)throws IOException{
        int b=in.read();if(b<0)throw new EOFException();return b;
    }
    private static byte[] readFully(InputStream in,int n)throws IOException{
        byte[] b=new byte[n];int offset=0;
        while(offset<n){int r=in.read(b,offset,n-offset);if(r<0)throw new EOFException();offset+=r;}
        return b;
    }
    private static void sendFrame(OutputStream out,int opcode,byte[] payload)throws IOException{
        synchronized(out){
            out.write(0x80|opcode);
            if(payload.length<126)out.write(payload.length);
            else {out.write(126);out.write(payload.length>>>8);out.write(payload.length);}
            out.write(payload);out.flush();
        }
    }
    private static void closeQuietly(Closeable c){if(c!=null)try{c.close();}catch(IOException ignored){}}
    private static final class Frame {
        final int opcode;final byte[] payload;
        Frame(int opcode,byte[] payload){this.opcode=opcode;this.payload=payload;}
    }
}
