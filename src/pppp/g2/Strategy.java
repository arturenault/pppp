package pppp.g2;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by naman on 9/20/15.
 */
public class Strategy {

    public StrategyType type;
    public Map<String, Object> properties;

    public Strategy() {
        this.type = StrategyType.diffusion;
        this.properties = new HashMap<String, Object>();
    }

    public Strategy(StrategyType type) {
        this.type = type;
        this.properties = new HashMap<String, Object>();
    }

    public Object getProperty(String property) {
        return this.properties.get(property);
    }

    public void setProperty(String name, Object val) {
        this.properties.put(name, val);
    }

    public boolean isPropertySet(String property) {
        return this.properties.containsKey(property);
    }

    public void incrementProperty(String property) {
        if(isPropertySet(property)) {
            this.properties.put(property, (Integer) this.properties.get(property) + 1);
        }
    }
}
