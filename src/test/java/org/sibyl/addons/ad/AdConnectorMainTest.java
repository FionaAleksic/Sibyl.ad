package org.sibyl.addons.ad;
public final class AdConnectorMainTest {
    public static void main(String[] args) {
        try{
            AdConnectorMain.main(new String[]{});
            throw new AssertionError("Missing operation should fail");
        }catch(IllegalArgumentException expected){}
        System.out.println("AdConnectorMainTest PASS");
    }
}
