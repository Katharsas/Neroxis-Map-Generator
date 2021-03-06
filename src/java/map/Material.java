package map;

import lombok.Data;
import util.serialized.MaterialSet;

import java.awt.*;

@Data
public strictfp class Material {
    private static final String texturePathFormat = "/env/%s/layers/%s_albedo.dds";
    private static final String normalPathFormat = "/env/%s/layers/%s.dds";

    private final String texturePath;
    private final String normalPath;
    private final float textureScale;
    private final float normalScale;
    private final Color previewColor;

    public Material(String environment, String name, float scale) {
        this(environment, environment, name, name, scale, scale, null);
    }

    public Material(String texEnv, String normalEnv, String texture, String normal, float textureScale, float normalScale, MaterialSet.PreviewColor previewColor) {
        texturePath = String.format(texturePathFormat, texEnv, texture);
        normalPath = String.format(normalPathFormat, normalEnv, normal);
        this.textureScale = textureScale;
        this.normalScale = normalScale;
        if (previewColor != null) {
            this.previewColor = new Color(previewColor.getRed(), previewColor.getGreen(), previewColor.getBlue());
        } else {
            this.previewColor = new Color(127, 127, 127);
        }
    }

}