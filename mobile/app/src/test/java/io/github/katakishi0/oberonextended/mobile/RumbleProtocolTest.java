package io.github.katakishi0.oberonextended.mobile;

import org.junit.Test;
import static org.junit.Assert.*;

public final class RumbleProtocolTest {
    @Test public void decodesAllFourXboxMotorGroups() {
        byte[] request=new byte[17];request[0]=(byte)0xFA;
        request[1]=(byte)255;request[2]=(byte)120;request[3]=30;request[4]=7;
        request[5]=22;request[6]=23;request[7]=24;request[8]=25;
        request[13]=60;request[14]=61;request[15]=62;request[16]=63;
        int[][] result=RumbleFeedback.decodeMotors(request);
        assertArrayEquals(new int[]{255,120,30,7},result[0]);
        assertArrayEquals(new int[]{22,23,24,25},result[1]);
        assertArrayEquals(new int[]{0,0,0,0},result[2]);
        assertArrayEquals(new int[]{60,61,62,63},result[3]);
    }
    @Test public void ignoresInvalidRumblePackets() {
        int[][] invalid=RumbleFeedback.decodeMotors(new byte[17]);
        for(int i=0;i<4;i++)assertArrayEquals(new int[4],invalid[i]);
        assertArrayEquals(new int[4],RumbleFeedback.decodeMotors(new byte[]{(byte)0xFA})[0]);
    }
}
