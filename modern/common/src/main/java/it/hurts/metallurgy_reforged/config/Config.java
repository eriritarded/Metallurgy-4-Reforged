package it.hurts.metallurgy_reforged.config;
import java.lang.annotation.*;
/** Metadata retained from the six source config definitions, independent of the removed1.12 loader API. */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
public @interface Config {
    String modid();String name();String category() default "";
    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.TYPE}) @interface Name {String value();}
    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.TYPE}) @interface LangKey {String value();}
    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.TYPE}) @interface Comment {String[] value();}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) @interface RangeInt {int min() default Integer.MIN_VALUE;int max() default Integer.MAX_VALUE;}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) @interface RangeDouble {double min() default -Double.MAX_VALUE;double max() default Double.MAX_VALUE;}
    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.TYPE}) @interface RequiresMcRestart {}
    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.TYPE}) @interface RequiresWorldRestart {}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) @interface Ignore {}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) @interface SlidingOption {}
}
