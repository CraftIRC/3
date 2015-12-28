package com.ensifera.animosity.craftirc;

final class Path {
    private final String sourceTag;
    private final String targetTag;

    Path(String sourceTag, String targetTag) {
        this.sourceTag = sourceTag;
        this.targetTag = targetTag;
    }

    @Override
    public int hashCode() {
        final int hashFirst = this.sourceTag != null ? this.sourceTag.hashCode() : 0;
        final int hashSecond = this.targetTag != null ? this.targetTag.hashCode() : 0;

        return ((hashFirst + hashSecond) * hashSecond) + hashFirst;
    }

    @Override
    public boolean equals(Object other) {
        if ((other != null) && (other instanceof Path)) {
            final Path otherPath = (Path) other;
            return (this.sourceTag.equals(otherPath.getSourceTag()) || this.sourceTag.equals("*")) && (this.targetTag.equals(otherPath.getTargetTag()) || this.targetTag.equals("*"));
        }
        return false;
    }

    @Override
    public String toString() {
        return this.sourceTag + " -> " + this.targetTag;
    }

    public String getSourceTag() {
        return this.sourceTag;
    }

    public String getTargetTag() {
        return this.targetTag;
    }
}
