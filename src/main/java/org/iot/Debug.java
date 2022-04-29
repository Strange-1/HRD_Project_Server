package org.iot;

public class Debug {
    public static void println(Class className, String sentence)
    {
        System.out.println("["+className.getSimpleName()+"] "+sentence);
    }
}
