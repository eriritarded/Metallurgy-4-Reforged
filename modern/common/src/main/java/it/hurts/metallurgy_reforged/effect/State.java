package it.hurts.metallurgy_reforged.effect;

/** A native NBT/component transaction, closed once after an effect mutation. */
public interface State extends AutoCloseable {
    String string(String key);
    int integer(String key);
    long longValue(String key);
    float decimal(String key);
    double doubleValue(String key);
    boolean flag(String key);
    boolean has(String key);
    void string(String key,String value);
    void integer(String key,int value);
    void longValue(String key,long value);
    void decimal(String key,float value);
    void doubleValue(String key,double value);
    void flag(String key,boolean value);
    void remove(String key);
    @Override void close();
}
