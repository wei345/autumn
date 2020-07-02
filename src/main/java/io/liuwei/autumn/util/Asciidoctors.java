package io.liuwei.autumn.util;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.jruby.internal.JRubyAsciidoctor;

/**
 * @author liuwei
 * @since 2020-07-02 13:55
 */
public class Asciidoctors {

    public static Asciidoctor getAsciidoctor(){
        return Holder.ASCIIDOCTOR;
    }

    private static class Holder {
        private static final Asciidoctor ASCIIDOCTOR = new JRubyAsciidoctor();
    }
}
