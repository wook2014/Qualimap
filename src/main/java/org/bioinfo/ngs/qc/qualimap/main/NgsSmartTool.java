package org.bioinfo.ngs.qc.qualimap.main;

import java.io.File;

import EDU.oswego.cs.dl.util.concurrent.FJTask;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.bioinfo.commons.log.Logger;
import org.bioinfo.ngs.qc.qualimap.gui.utils.Constants;
import org.bioinfo.tool.OptionFactory;

public abstract class NgsSmartTool {
	
	// log
	protected Logger logger;
	
	// environment
	protected String homePath;
	
	// arguments
	protected Options options;
	protected CommandLine commandLine;
	protected CommandLineParser parser;
	
	// common params
	protected String outdir;
    protected String toolName;
    protected String outputType;
    protected boolean outDirIsRequired;

    static String OPTION_NAME_OUTDIR = "outdir";
    static String OPTION_NAME_HOMDIR = "home";
    static String OPTION_NAME_OUTPUT_TYPE = "outformat";

	public NgsSmartTool(String toolName){

		this.toolName = toolName;
		// log
		logger = new Logger();
        outDirIsRequired = true;

		// environment
		homePath = System.getenv("QUALIMAP_HOME");
        if (homePath == null) {
            homePath = "";
        }
		if(homePath.endsWith(File.separator)){
			homePath = homePath.substring(0,homePath.length()-1);
		}

		// arguments
		options = new Options();
		parser = new PosixParser();
		outdir = "";
        outputType = Constants.REPORT_TYPE_HTML;

		initCommonOptions();
		
		initOptions();
	}
	
	private void initCommonOptions(){
		options.addOption(OPTION_NAME_HOMDIR, true, "home folder of Qualimap");
		if (outDirIsRequired) {
            options.addOption( OPTION_NAME_OUTDIR, true, "output folder" );
        }

        options.addOption( OPTION_NAME_OUTPUT_TYPE, true, "output report format (PDF or HTML, default is HTML)");

	}
	
	// init options
	protected abstract void initOptions();
	
	// parse options 
	protected void parse(String[] args) throws ParseException{
		// get command line
		commandLine = parser.parse(options, args);

        // fill common options
		if(commandLine.hasOption(OPTION_NAME_OUTDIR)){
			outdir = commandLine.getOptionValue(OPTION_NAME_HOMDIR);
		}


        if (commandLine.hasOption(OPTION_NAME_OUTPUT_TYPE)) {
            outputType = commandLine.getOptionValue(OPTION_NAME_OUTPUT_TYPE);
            if (outputType != Constants.REPORT_TYPE_HTML && outputType != Constants.REPORT_TYPE_PDF) {
                throw new ParseException("Unknown output report format " + outputType);
            }
        }

	}
	
	// check options
	protected abstract void checkOptions() throws ParseException;
	
	// execute tool
	protected abstract void execute() throws Exception;
	
	
	// public run (parse and execute)
	public void run(String[] args) throws Exception{
		// parse
		parse(args);
		
		// check options
		checkOptions();
		
		// execute
		execute();
	}
	
	protected void printHelp(){
		HelpFormatter h = new HelpFormatter();
		h.setWidth(150);		
		h.printHelp("qualimap " + toolName, options, true);
		logger.println("");
		logger.println("");
		logger.println("");
	}

	protected void initOutputDir(){

        if(!outdir.isEmpty()){
        	if(new File(outdir).exists()){
				logger.warn("output folder already exists");
			} else {
				new File(outdir).mkdirs();
			}
		}
	}
	
	protected boolean exists(String fileName){
		return new File(fileName).exists();
	}

}
