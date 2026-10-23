package dev.miles.metallurgy.qa;

import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/** Fail before FML/GLFW if the OS did not assign our private inactive desktop. */
public final class IsolationBootstrap {
    private static final NativeLibrary USER = NativeLibrary.getInstance("user32");
    private static String name(Pointer handle) {
        if (handle == null) throw new IllegalStateException("Missing Windows isolation handle");
        var buffer = new Memory(1024);
        int ok = USER.getFunction("GetUserObjectInformationW").invokeInt(new Object[]{handle, 2, buffer, 1024, new IntByReference()});
        if (ok == 0) throw new IllegalStateException("Cannot inspect Windows isolation handle");
        return buffer.getWideString(0);
    }
    public static void main(String[] args) throws Throwable {
        int tid = NativeLibrary.getInstance("kernel32").getFunction("GetCurrentThreadId").invokeInt(new Object[]{});
        String station = name(USER.getFunction("GetProcessWindowStation").invokePointer(new Object[]{}));
        String desktop = name(USER.getFunction("GetThreadDesktop").invokePointer(new Object[]{tid}));
        Pointer input = USER.getFunction("OpenInputDesktop").invokePointer(new Object[]{0, false, 1});
        if (input == null) throw new IllegalStateException("Cannot verify input desktop; refusing launch");
        String inputName;
        try { inputName = name(input); }
        finally { USER.getFunction("CloseDesktop").invokeInt(new Object[]{input}); }
        if (!station.equals(System.getProperty("metallurgy.qa.station")) ||
            desktop.equalsIgnoreCase(inputName) || !desktop.startsWith("MetallurgyQA_") ||
            !desktop.equals(System.getProperty("metallurgy.qa.desktop"))) {
            throw new IllegalStateException("Refusing game launch on unexpected station/desktop: "+station+"/"+desktop);
        }
        System.out.println("METALLURGY_QA_ISOLATION "+station+"/"+desktop+"; input="+inputName+"; inactive desktop; no desktop switching");
        Class.forName("net.neoforged.fml.startup.Client").getMethod("main", String[].class).invoke(null, (Object)args);
    }
}
