package uk.ac.ebi.interpro.common;

import uk.ac.ebi.interpro.common.collections.CollectionUtils;

import java.awt.*;
import java.util.Map;

public class ColourUtils {
    /** Extract alpha value from rgb */
    public static int alpha(int rgb) {
        return (rgb >> 24) & 0xff;
    }

    /** Extract red value from rgb */
    public static int red(int rgb) {
        return (rgb >> 16) & 0xff;
    }

    /** Extract green value from rgb */
    public static int green(int rgb) {
        return (rgb >> 8) & 0xff;
    }

    /** Extract blue value from rgb */
    public static int blue(int rgb) {
        return (rgb) & 0xff;
    }

    /** Construct rgb value */
    public static int rgb(int red, int green, int blue) {
        return 0xff000000+(red << 16) + (green << 8) + blue;
    }

    /** Construct rgb value */
    public static int argb(int alpha,int red, int green, int blue) {
        return (alpha << 24 )+ (red << 16) + (green << 8) + blue;
    }

    /** return shade of grey */
    public static int grey(int lightness) {
        return rgb(lightness, lightness, lightness);
    }

    /** transform a hsb value by an illumination value (0 -> black 1->colour 2->white)*/
    public static int light(float[] hsb, float light) {
        if (light < 1.0f)
            return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2] * light);
        else
            return Color.HSBtoRGB(hsb[0], hsb[1] * (2.0f - light), 1.0f-(1.0f-hsb[2])*(2.0f - light));
    }


    public static int combine(int a, int b) {
        float alpha=ColourUtils.alpha(a)/255f;
        return rgb((int)(red(a)*alpha+red(b)*(1-alpha)),(int)(green(a)*alpha+green(b)*(1-alpha)),(int)(blue(a)*alpha+blue(b)*(1-alpha)));
    }

    public static String css(int c) {
        return "#"+ StringUtils.encodeHex((byte)(c>>16))+StringUtils.encodeHex((byte)(c>>8))+StringUtils.encodeHex((byte)c);
    }

    public static Map<String,String> namedColours= CollectionUtils.arrayToMap(new String[][]{
            {"red","f00"},
            {"green","0f0"},
            {"blue","00f"},
            {"yellow","ff0"},
            {"cyan","0ff"},
            {"magenta","f0f"},
            {"black","000"},
            {"white","fff"}
    });



    public static int[] decodeColour(String text) {
        if (text.startsWith("#")) {
            if (text.length()==4) return new int[]{
                    Integer.parseInt(text.substring(1,2),16)*17,
                    Integer.parseInt(text.substring(2,3),16)*17,
                    Integer.parseInt(text.substring(3,4),16)*17
            };
            if (text.length()==7) return new int[]{
                    Integer.parseInt(text.substring(1,3),16),
                    Integer.parseInt(text.substring(3,5),16),
                    Integer.parseInt(text.substring(5,7),16)
            };
        }
        String code=namedColours.get(text);
        if (code!=null) return decodeColour("#"+code);



        throw new IllegalArgumentException("Unknown colour "+text);

    }

	public static int intDecodeColour(String text) {
		int[] c=decodeColour(text);
		return (c[0]<<16)+(c[1]<<8)+c[2];
    }


	static String[] colourPalette={
"#000000",
"#FF0000",
"#00FF00",
"#0000FF",
"#FF00FF",
"#00FFFF",
"#FFFF00",
"#70DB93",
"#B5A642",
"#5F9F9F",
"#B87333",
"#2F4F2F",
"#9932CD",
"#871F78",
"#855E42",
"#545454",
"#8E2323",
"#F5CCB0",
"#238E23",
"#CD7F32",
"#DBDB70",
"#C0C0C0",
"#527F76",
"#9F9F5F",
"#8E236B",
"#2F2F4F",
"#EBC79E",
"#CFB53B",
"#FF7F00",
"#DB70DB",
"#D9D9F3",
"#5959AB",
"#8C1717",
"#238E68",
"#6B4226",
"#8E6B23",
"#007FFF",
"#00FF7F",
"#236B8E",
"#38B0DE",
"#DB9370",
"#ADEAEA",
"#5C4033",
"#4F2F4F",
"#CC3299",
"#99CC32"
};

	public static void main(String[] args) {
		for (String c : colourPalette) {
			System.out.println("\"#"+Integer.toString(combine(0x80ffffff, intDecodeColour(c)) & 0xffffff,16)+"\",");
		}
	}

}