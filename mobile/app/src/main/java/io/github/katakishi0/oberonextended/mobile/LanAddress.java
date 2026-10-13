package io.github.katakishi0.oberonextended.mobile;

import java.net.*;
import java.util.*;
public final class LanAddress {
    private LanAddress(){}
    public static String find(){
        try {
            for(NetworkInterface ni:Collections.list(NetworkInterface.getNetworkInterfaces())){
                if(!ni.isUp()||ni.isLoopback()||ni.isVirtual())continue;
                for(InetAddress addr:Collections.list(ni.getInetAddresses())){
                    if(addr instanceof Inet4Address&&!addr.isLoopbackAddress()&&
                        addr.isSiteLocalAddress())return addr.getHostAddress();
                }
            }
        }catch(SocketException ignored){}
        return "No local Wi-Fi IP found";
    }
}
