package dev.davidv.bergamot;

/**
 * JNI bindings for CLD2 language detection in libbergamot-sys.so
 */
@SuppressWarnings("PMD.AvoidUsingNativeCode")
public class LangDetect {

    static {
        System.loadLibrary("bergamot-sys");
    }

    public native DetectionResult detectLanguage(String text, String hint);
}
