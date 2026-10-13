package io.github.katakishi0.oberonextended.mobile;

import org.junit.Test;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class OberonWebSocketServerTest {
    @Test public void originalXboxHandshakeAndInputRequest() throws Exception {
        InputState input=new InputState();
        input.setTouch(0,0,0,0,0,0,InputState.A);
        OberonWebSocketServer server=new OberonWebSocketServer(input,(text,count)->{});
        server.start();
        try(Socket s=new Socket("127.0.0.1",26401)){
            s.setSoTimeout(5000);
            OutputStream out=s.getOutputStream();
            InputStream in=s.getInputStream();
            String req="GET / HTTP/1.1\r\n"+
                "Host: 127.0.0.1:26401\r\n"+
                "Upgrade: websocket\r\nConnection: Upgrade\r\n"+
                "Sec-WebSocket-Version: 13\r\n"+
                "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n\r\n";
            out.write(req.getBytes(StandardCharsets.US_ASCII));out.flush();
            StringBuilder response=new StringBuilder();
            int state=0;
            while(state<4){
                int b=in.read();assertTrue(b>=0);response.append((char)b);
                if((state==0||state==2)&&b==13)state++;
                else if((state==1||state==3)&&b==10)state++;
                else state=b==13?1:0;
            }
            assertTrue(response.toString().startsWith("HTTP/1.1 101"));
            assertTrue(response.toString().contains("s3pPLMBiTxaQ9kYGzzhZRbK+xOo="));
            byte[] hello=readFrame(in);
            assertEquals(0x0A,hello[0]&0xFF);
            // RFC6455 clients mask all outgoing payloads.
            out.write(new byte[]{(byte)0x82,(byte)0x81,1,2,3,4,(byte)(0xFA^1)});
            out.flush();
            byte[] packet=readFrame(in);
            assertEquals(100,packet.length);
            assertEquals(0xFF,packet[0]&0xFF);
            assertEquals(0x80,packet[13]&0xFF);
        } finally {server.stop();}
    }
    private static byte[] readFrame(InputStream in)throws IOException{
        int op=in.read(),len=in.read();
        if(op<0||len<0)throw new EOFException();
        int size=len&127;
        if(size==126)size=(in.read()<<8)|in.read();
        byte[] p=new byte[size];int n=0;
        while(n<size){int r=in.read(p,n,size-n);if(r<0)throw new EOFException();n+=r;}
        return p;
    }
}
