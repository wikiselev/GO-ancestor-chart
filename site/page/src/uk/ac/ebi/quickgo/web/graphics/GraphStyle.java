package uk.ac.ebi.quickgo.web.graphics;

import uk.ac.ebi.interpro.common.StringUtils;
import uk.ac.ebi.quickgo.web.Request;

import java.awt.*;
import java.util.Map;

public class GraphStyle {
	public enum GraphStyleParameter {
		FONT_NAME("fontName"),
		FONT_SIZE("font"),
		FILL("fill"),
		SHOW_TERM_IDS("ids"),
		SHOW_KEY("key"),
		SHOW_SLIM_COLOURS("slim"),
		SHOW_CHILDREN("showChildren"),
		WIDTH("width"),
		HEIGHT("height");

		public String name;
		public String key;

		GraphStyleParameter(String name) {
			this.name = name;
			this.key = "c$" + name;
		}
	}

	private Font nameFont;
	
	public String fontName = "Arial";
    public int fontSize = 11;
	public boolean fill = true;
    public boolean termIds = false;
	public boolean key = true;
    public boolean slimColours = true;
	public boolean showChildren = false;
    public int width = 85;
	public int height = 55;

	public GraphStyle(Request r) {
		fontName = StringUtils.nvl(r.getCookieValue(GraphStyleParameter.FONT_NAME.key), fontName);
		fontSize = StringUtils.parseInt(r.getCookieValue(GraphStyleParameter.FONT_SIZE.key), fontSize);
		fill = StringUtils.parseBoolean(r.getCookieValue(GraphStyleParameter.FILL.key), fill);
		termIds = StringUtils.parseBoolean(r.getCookieValue(GraphStyleParameter.SHOW_TERM_IDS.key), termIds);
		key = StringUtils.parseBoolean(r.getCookieValue(GraphStyleParameter.SHOW_KEY.key), key);
		slimColours = StringUtils.parseBoolean(r.getCookieValue(GraphStyleParameter.SHOW_SLIM_COLOURS.key), slimColours);
		showChildren = StringUtils.parseBoolean(r.getCookieValue(GraphStyleParameter.SHOW_CHILDREN.key), showChildren);
		width = StringUtils.parseInt(r.getCookieValue(GraphStyleParameter.WIDTH.key), width);
		height = StringUtils.parseInt(r.getCookieValue(GraphStyleParameter.HEIGHT.key), height);
	}

    public GraphStyle(Map<String, String> parameters) {
	    initialise(parameters);
    }

	public void initialise(Map<String, String> parameters) {
		fontName = StringUtils.nvl(parameters.get(GraphStyleParameter.FONT_NAME.name), fontName);
		fontSize = StringUtils.parseInt(parameters.get(GraphStyleParameter.FONT_SIZE.name), fontSize);
		fill = StringUtils.parseBoolean(parameters.get(GraphStyleParameter.FILL.name), fill);
		termIds = StringUtils.parseBoolean(parameters.get(GraphStyleParameter.SHOW_TERM_IDS.name), termIds);
		key = StringUtils.parseBoolean(parameters.get(GraphStyleParameter.SHOW_KEY.name), key);
		slimColours = StringUtils.parseBoolean(parameters.get(GraphStyleParameter.SHOW_SLIM_COLOURS.name), slimColours);
		showChildren = StringUtils.parseBoolean(parameters.get(GraphStyleParameter.SHOW_CHILDREN.name), showChildren);
		width = StringUtils.parseInt(parameters.get(GraphStyleParameter.WIDTH.name), width);
		height = StringUtils.parseInt(parameters.get(GraphStyleParameter.HEIGHT.name), height);
	}

	public void saveSettings(Request r) {
		r.setCookieValue(GraphStyleParameter.FONT_NAME.key, fontName);
		r.setCookieValue(GraphStyleParameter.FONT_SIZE.key, Integer.toString(fontSize));
		r.setCookieValue(GraphStyleParameter.FILL.key, fill ? "true" : "false");
		r.setCookieValue(GraphStyleParameter.SHOW_TERM_IDS.key, termIds ? "true" : "false");
		r.setCookieValue(GraphStyleParameter.SHOW_KEY.key, key ? "true" : "false");
		r.setCookieValue(GraphStyleParameter.SHOW_SLIM_COLOURS.key, slimColours ? "true" : "false");
		r.setCookieValue(GraphStyleParameter.SHOW_CHILDREN.key, showChildren ? "true" : "false");
		r.setCookieValue(GraphStyleParameter.WIDTH.key, Integer.toString(width));
		r.setCookieValue(GraphStyleParameter.HEIGHT.key, Integer.toString(height));
	}

	Font getFont() {
	    if (nameFont == null) {
		    nameFont = new Font(fontName, Font.PLAIN, fontSize);
	    }
	    return nameFont;
	}
}
