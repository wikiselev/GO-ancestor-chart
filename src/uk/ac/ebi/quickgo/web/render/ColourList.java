package uk.ac.ebi.quickgo.web.render;

import uk.ac.ebi.interpro.common.*;

public class ColourList {
    private static int[] magicColours = { 0xff9999, 0x00ffff, 0xff0000, 0x00ff00, 0x0000ff, 0xffcc00, 0xFF6600, 0x009933, 0xffff00, 0x00ccff, 0xff00cc, 0x00ffcc, 0xccff00, 0xcc00ff  };
    private int[] colours;

    public String getColourCSS(int index) {
        return ColourUtils.css(getColourCode(index));
    }

    public int getColourCode(int index) {
        return colours[index % colours.length];
    }

    public ColourList(int combine) {
        colours = new int[magicColours.length];
        for (int i = 0; i < magicColours.length; i++) {
	        colours[i] = ColourUtils.combine(combine, magicColours[i]);
        }
    }
}
