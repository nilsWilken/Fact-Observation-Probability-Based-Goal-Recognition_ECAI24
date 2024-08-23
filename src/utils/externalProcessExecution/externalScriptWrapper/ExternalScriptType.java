package utils.externalProcessExecution.externalScriptWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public enum ExternalScriptType {

    EXTERNAL_COMMAND_EXECUTION,
    CLEANUP;

    public static IExternalScript getExternalScriptWrapper(ExternalScriptType type) {
        switch(type) {
            case CLEANUP:
                return CleanupScript.getInstance();
            case EXTERNAL_COMMAND_EXECUTION:
                return ExternalCommandScript.getInstance();
            default:
                return null;

        }
    }

    protected static void makeFileExecutable(File f) {
        try {
            f.createNewFile();
            Set<PosixFilePermission> permission = Files.getPosixFilePermissions(Paths.get(f.getAbsolutePath()));
            permission.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(Paths.get(f.getAbsolutePath()), permission);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
