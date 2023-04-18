package util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ArgsParser {
    private Option id, numMsgs, clientCount, duration, msgSize, readMsgsFromFile, skeen, debug, 
    warmup, tpcc, profiler, numPartitions, cpuUsage, batchSize, batchTimeout, locality, disjoint,
    homewarehouse, region, algorithm;
    private Options options;
    private CommandLineParser parser;
    private CommandLine line;
    private String command;

    private ArgsParser() {
        id = Option.builder("i").desc("server id").argName("id").hasArg().numberOfArgs(1).type(Short.class).build();
        algorithm = Option.builder("a").desc("algorithm").argName("algorithm").hasArg().numberOfArgs(1).type(Short.class).build();
        clientCount = Option.builder("c").desc("total number of clients").argName("clients").hasArg().numberOfArgs(1).type(Short.class).build();
        numMsgs = Option.builder("m").desc("number of messages each client send (defaults to -1)").argName("messages").hasArg().numberOfArgs(1).type(Integer.class).build();
        duration = Option.builder("d").desc("time to execute in seconds (defaults to 120)").argName("seconds").hasArg().numberOfArgs(1).type(Short.class).build();
        msgSize = Option.builder("s").desc("message size in bytes (defaults to 64)").argName("bytes").hasArg().numberOfArgs(1).type(Integer.class).build();
        readMsgsFromFile = Option.builder("f").desc("client read msgs to send from file").argName("msgs from file").type(Boolean.class).build();
        skeen = Option.builder("sk").desc("skeen algorithm").argName("skeen").hasArg().numberOfArgs(1).type(Integer.class).build();
        debug = Option.builder("dbg").desc("prints debug messages").argName("debug").type(Boolean.class).build();
        warmup = Option.builder("w").desc("time to warmup in seconds (defaults to 10)").argName("warmup").hasArg().numberOfArgs(1).type(Short.class).build();
        tpcc = Option.builder("t").desc("tpcc workload").argName("tpcc").type(Boolean.class).build();
        profiler = Option.builder("p").desc("profiler on").argName("profiler").type(Boolean.class).build();
        numPartitions = Option.builder("np").desc("max number of partitions per request (defaults to 10)").argName("#partitions").hasArg().numberOfArgs(1).type(Short.class).build();
        cpuUsage = Option.builder("cpu").desc("record cpu usage").argName("cpu usage").type(Boolean.class).build();
        batchSize = Option.builder("bs").desc("batch size in # of msgs (defaults to 0)").argName("batch size").hasArg().numberOfArgs(1).type(Integer.class).build();
        batchTimeout = Option.builder("bt").desc("batch timeout in microsec").argName("batch timeout").hasArg().numberOfArgs(1).type(Integer.class).build();
        locality = Option.builder("l").desc("with locality").argName("locality").hasArg().numberOfArgs(1).type(Integer.class).build();
        disjoint = Option.builder("dj").desc("disjoint").argName("disjoint").type(Boolean.class).build();
        homewarehouse = Option.builder("w").desc("home warehouse").argName("homewarehouse").hasArg().numberOfArgs(1).type(Integer.class).build();
        region = Option.builder("r").desc("region").argName("region").hasArg().numberOfArgs(1).type(String.class).build();
        options = new Options();
        parser = new DefaultParser();
        line = null;
    }

    public static ArgsParser getClientParser(String[] args) {
        ArgsParser parser = new ArgsParser();
        parser.command = "Client";
        parser.id.setRequired(true);
        parser.clientCount.setRequired(true);
        parser.algorithm.setRequired(true);
        parser.options.addOption(parser.id);
        parser.options.addOption(parser.algorithm);
        parser.options.addOption(parser.clientCount);
        parser.options.addOption(parser.duration);
        parser.options.addOption(parser.msgSize);
        parser.options.addOption(parser.numMsgs);
        parser.options.addOption(parser.readMsgsFromFile);
        parser.options.addOption(parser.skeen);
        parser.options.addOption(parser.tpcc);
        parser.options.addOption(parser.debug);
        parser.options.addOption(parser.numPartitions);
        parser.options.addOption(parser.locality);
        parser.options.addOption(parser.disjoint);
        parser.options.addOption(parser.homewarehouse);
        parser.options.addOption(parser.region);
        parser.parse(args);
        return parser;
    }

    public static ArgsParser getServerParser(String[] args) {
        ArgsParser parser = new ArgsParser();
        parser.command = "Server";
        parser.id.setRequired(true);
        parser.clientCount.setRequired(true);
        parser.algorithm.setRequired(true);
        parser.options.addOption(parser.id);
        parser.options.addOption(parser.algorithm);
        parser.options.addOption(parser.clientCount);
        parser.options.addOption(parser.duration);
        parser.options.addOption(parser.skeen);
        parser.options.addOption(parser.debug);
        parser.options.addOption(parser.profiler);
        parser.options.addOption(parser.warmup);
        parser.options.addOption(parser.cpuUsage);
        parser.options.addOption(parser.batchSize);
        parser.options.addOption(parser.batchTimeout);
        parser.parse(args);
        return parser;
    }

    public short getId() {
        return Short.valueOf(line.getOptionValue("i"));
    }

    public short getAlgorithm() {
        return Short.valueOf(line.getOptionValue("a"));
    }

    public int getNumMessages() {
        String v = line.getOptionValue("m");
        return v == null ? -1 : Integer.parseInt(v);
    }

    public short getClientCount() {
        String v = line.getOptionValue("c");
        return v == null ? 1 : Short.valueOf(v);
    }

    public short getDuration() {
        String v = line.getOptionValue("d");
        return v == null ? 120 : Short.valueOf(v);
    }

    public short getWarmup() {
        String v = line.getOptionValue("w");
        return v == null ? 10 : Short.valueOf(v);
    }

    public int getMsgSize() {
        String v = line.getOptionValue("s");
        return v == null ? 64 : Integer.parseInt(v);
    }

    public int getBatchSize() {
        String v = line.getOptionValue("bs");
        return v == null ? 0 : Integer.parseInt(v);
    }

    public int getBatchTimeout() {
        String v = line.getOptionValue("bt");
        return v == null ? 1000 : Integer.parseInt(v);
    }

    public int skeen() {
        String v = line.getOptionValue("sk");
        return v == null ? 0 : Integer.parseInt(v);
    }

    public boolean isSkeen() {
        String v = line.getOptionValue("sk");
        return v == null ? false : Integer.parseInt(v) > 0;
    }

    public boolean isTpcc() {
        return line.hasOption("t");
    }

    public boolean isDebug() {
        return line.hasOption("dbg");
    }
    
    public boolean isProfiler() {
        return line.hasOption("p");
    }

    public short getNumPartitions() {
        String v = line.getOptionValue("np");
        return v == null ? 10 : Short.valueOf(v);
    }

    public boolean cpuUsage(){
        return line.hasOption("cpu");
    }

    public int getLocality(){
        String v = line.getOptionValue("l");
        return v == null ? 0 : Integer.valueOf(v);
    }

    public int getHomeWarehouse(){
        String v = line.getOptionValue("w");
        return v == null ? 0 : Integer.valueOf(v);
    }

    public String getRegion(){
        String v = line.getOptionValue("r");
        return v == null ? "" : v;
    }

    public boolean disjoint(){
        return line.hasOption("dj");
    }

    private void parse(String args[]) {
        try {
            line = parser.parse(options, args, false);
        }
        catch (ParseException exp) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(command, options);
            System.err.println("Fail to parse command line options!");
            System.err.println(exp.getMessage());
            System.exit(-1);
        }
    }

    public boolean getReadMsgsFromFile() {
        return line.hasOption("f");
    }
}
