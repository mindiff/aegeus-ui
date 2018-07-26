package io.aegeus.jaxrs;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

final class Options {

    @Option(name = "--help", help = true)
    boolean help;

    @Argument
    List<String> args = new ArrayList<String>();
}
