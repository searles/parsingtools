package at.searles.parsingtools.common;

import at.searles.parsing.ParserStream;

public class SyntaxInfo {

    private final long start;
    private final long end;

    protected SyntaxInfo(ParserStream stream) {
        this.start = stream.getStart();
        this.end = stream.getEnd();
    }

    protected SyntaxInfo(SyntaxInfo node) {
        this.start = node.getStart();
        this.end = node.getEnd();
    }

    public String toString() {
        return String.format("[%d:%d]", getStart(), getEnd());
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }
}
