package at.searles.parsing.tools.common;

import at.searles.parsing.Mapping;
import at.searles.parsing.ParserStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToInt implements Mapping<CharSequence, Integer> {
    @Override
    public Integer parse(ParserStream stream, @NotNull CharSequence left) {
        try {
            return Integer.parseInt(left.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    @Override
    public CharSequence left(@NotNull Integer result) {
        return result.toString();
    }
}
