package at.searles.parsingtools.properties;

import at.searles.parsing.Initializer;
import at.searles.parsing.ParserStream;

public class PropertiesInitializer implements Initializer<Properties> {

    private static class Holder {
        static Properties instance = new Properties();
    }

    @Override
    public Properties parse(ParserStream stream) {
        return Holder.instance;
    }

    @Override
    public boolean consume(Properties properties) {
        return properties.isEmpty();
    }

    @Override
    public String toString() {
        return "{empty properties}";
    }
}