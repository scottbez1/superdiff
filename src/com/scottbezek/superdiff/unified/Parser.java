package com.scottbezek.superdiff.unified;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    private static final String FILENAME_PATTERN = "(.+)";
    private static final String RANGE = "(\\d+)(?:,(\\d+))?";

    private static final Pattern HEADER_LEFT_FILE = Pattern.compile("^--- " + FILENAME_PATTERN);
    private static final Pattern HEADER_RIGHT_FILE = Pattern.compile("^\\+\\+\\+ " + FILENAME_PATTERN);
    private static final Pattern CHUNK = Pattern.compile("^@@ -" + RANGE + " \\+" + RANGE + " @@(?: (.*))?");
//
//    // Additional syntax defined by git-diff. http://git-scm.com/docs/git-diff
//    private static final Pattern GIT_DIFF_CMD = Pattern.compile("^diff");
//    private static final Pattern GIT_INDEX = Pattern.compile("^index");
//    private static final Pattern GIT_NEW_FILE_MODE = Pattern.compile("^new file mode");
//    private static final Pattern GIT_DELETED_FILE_MODE = Pattern.compile("^deleted file mode");
//    private static final Pattern GIT_MERGE_CHUNK_HEADER = Pattern.compile("^@@(@+)");

    // TODO(sbezek): Using a PrintStream to avoid dependencies on Android's Log, but probably don't need such a rich interface...
    private final PrintStream mDebugStream;

    public Parser(PrintStream debugOutput) {
        mDebugStream = debugOutput;
    }

    public List<SingleFileDiff> parse(Scanner input) throws DiffParseException {
        final List<SingleFileDiff> fileDiffs = new ArrayList<SingleFileDiff>();

        SingleFileDiff.Builder currentFileBuilder = new SingleFileDiff.Builder();
        Chunk.Builder chunkBuilder = null;

        final Matcher headerLeftFile = HEADER_LEFT_FILE.matcher("");
        final Matcher headerRightFile = HEADER_RIGHT_FILE.matcher("");
        final Matcher chunkHeader = CHUNK.matcher("");

        while (input.hasNextLine()) {
            final String line = input.nextLine();
            if (chunkBuilder != null) {
                switch(line.charAt(0)) {
                case ' ':
                    chunkBuilder.appendLineUnchanged(line.substring(1));
                    break;
                case '-':
                    chunkBuilder.appendLineLeftRemoved(line.substring(1));
                    break;
                case '+':
                    chunkBuilder.appendLineRightAdded(line.substring(1));
                    break;
                default:
                    throw new DiffParseException("Expected a line diff, but instead got:" + line);
                }
                if (chunkBuilder.isComplete()) {
                    currentFileBuilder.addChunk(chunkBuilder.build());
                    chunkBuilder = null;
                }
            } else {
                headerLeftFile.reset(line);
                if (headerLeftFile.matches()) {
                    if (currentFileBuilder.isPotentiallyComplete()) {
                        fileDiffs.add(currentFileBuilder.build());
                        currentFileBuilder = new SingleFileDiff.Builder();
                    }
                    currentFileBuilder.setLeftFilename(headerLeftFile.group(1));
                    continue;
                }
                headerRightFile.reset(line);
                if (headerRightFile.matches()) {
                    if (currentFileBuilder.isPotentiallyComplete()) {
                        fileDiffs.add(currentFileBuilder.build());
                        currentFileBuilder = new SingleFileDiff.Builder();
                    }
                    currentFileBuilder.setRightFilename(headerRightFile.group(1));
                    continue;
                }
                chunkHeader.reset(line);
                if (chunkHeader.matches()) {
                    try {
                        int leftStartLine = Integer.parseInt(chunkHeader.group(1));
                        int leftLength = Integer.parseInt(chunkHeader.group(2));
                        int rightStartLine = Integer.parseInt(chunkHeader.group(3));
                        int rightLength = Integer.parseInt(chunkHeader.group(4));

                        String chunkContextSnippet = chunkHeader.group(5);

                        chunkBuilder = new Chunk.Builder(leftStartLine, leftLength, rightStartLine, rightLength);
                        continue;
                    } catch (NumberFormatException e) {
                        throw new DiffParseException("Failed to parse line numbers in header:" + line, e);
                    }
                }

                // Unknown line type
                mDebugStream.println("Unknown diff line:" + line);
            }
        }

        fileDiffs.add(currentFileBuilder.build());
        return fileDiffs;
    }

    public static class DiffParseException extends Exception {

        private static final long serialVersionUID = 3306841385008577257L;

        public DiffParseException() {
            super();
        }

        public DiffParseException(String message) {
            super(message);
        }

        public DiffParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
