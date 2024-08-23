package utils.externalProcessExecution.externalScriptWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;

import config.GoalRecognitionConfiguration;

public class ExternalCommandScript implements IExternalScript{
    private static ExternalCommandScript instance;
    private File location;

    protected static ExternalCommandScript getInstance() {
        if(ExternalCommandScript.instance == null) {
            ExternalCommandScript.instance = new ExternalCommandScript();
        }

        return ExternalCommandScript.instance;
    }

    private ExternalCommandScript() {
        try {
            this.location = new File("externalExecution.sh");
            if(!this.location.exists()) {
                ExternalScriptType.makeFileExecutable(this.location);

                Files.writeString(Paths.get(this.location.getAbsolutePath()), this.generateScriptContent(), new OpenOption[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getLocation() {
        return this.location;
    }

    protected String generateScriptContent() {
        StringBuffer content = new StringBuffer();

        content.append("#!/bin/bash\n\n");
        content.append("ulimit -Sv " + GoalRecognitionConfiguration.MAXIMUM_EXTERNAL_PROCESS_MEMORY + "\n");
        content.append("ulimit -v\n\n");
        content.append("echo $1\n\n");
        content.append("$1");

        return content.toString();
    }

}