package exporter;

import generator.PreviewGenerator;
import map.*;
import util.DDSHeader;
import util.Vector2f;
import util.Vector3f;
import util.Vector4f;
import util.serialized.LightingSettings;
import util.serialized.WaterSettings;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.*;
import java.nio.file.Path;

import static util.Swapper.swap;

public strictfp class SCMapExporter {

    public static File file;

    private static DataOutputStream out;

    public static void exportSCMAP(Path folderPath, String mapName, SCMap map) throws IOException {
        file = folderPath.resolve(mapName + ".scmap").toFile();
        boolean status = file.createNewFile();
        out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

        // header
        writeInt(SCMap.SIGNATURE);
        writeInt(SCMap.VERSION_MAJOR);
        writeInt(-1091567891); // unknown
        writeInt(2); // unknown
        writeFloat(map.getSize()); // width
        writeFloat(map.getSize()); // height
        writeInt(0); // unknown
        writeShort((short) 0); // unknown

        DDSHeader previewDDSHeader = new DDSHeader();
        previewDDSHeader.setWidth(256);
        previewDDSHeader.setHeight(256);
        previewDDSHeader.setRGBBitCount(32);
        previewDDSHeader.setRBitMask(0x00FF0000);
        previewDDSHeader.setGBitMask(0x0000FF00);
        previewDDSHeader.setBBitMask(0x000000FF);
        previewDDSHeader.setABitMask(0xFF000000);

        writeImageData(map.getPreview(), previewDDSHeader);

        writeInt(map.getMinorVersion());

        // heightmap
        writeInt(map.getSize()); // width
        writeInt(map.getSize()); // height
        writeFloat(map.getHeightMapScale());
        writeShorts(((DataBufferUShort) map.getHeightmap().getData().getDataBuffer()).getData()); // heightmap data

        writeByte((byte) 0); // unknown

        // textures
        writeStringNull(map.getTerrainShaderPath());
        writeStringNull(map.getBackgroundPath());
        writeStringNull(map.getSkyCubePath());
        writeInt(map.getCubeMapCount());
        for (CubeMap cubeMap : map.getCubeMaps()) {
            writeStringNull(cubeMap.getName());
            writeStringNull(cubeMap.getPath());
        }

        // lighting
        LightingSettings mapLightingSettings = map.getBiome().getLightingSettings();
        writeLightingSettings(mapLightingSettings);

        // water
        WaterSettings mapWaterSettings = map.getBiome().getWaterSettings();
        writeWaterSettings(mapWaterSettings);

        // wave generators
        writeInt(map.getWaveGeneratorCount());
        for (WaveGenerator waveGenerator : map.getWaveGenerators()) {
            writeWaveGenerator(waveGenerator);
        }

        // terrain textures
        TerrainMaterials mapTerrainMaterials = map.getBiome().getTerrainMaterials();
        writeInt(map.getMiniMapContourInterval());
        writeInt(map.getMiniMapDeepWaterColor());
        writeInt(map.getMiniMapContourColor());
        writeInt(map.getMiniMapShoreColor());
        writeInt(map.getMiniMapLandStartColor());
        writeInt(map.getMiniMapLandEndColor());

        if (map.getMinorVersion() > 56) {
            writeFloat(0);
        }
        for (int i = 0; i < TerrainMaterials.TERRAIN_TEXTURE_COUNT; i++) {
            writeStringNull(mapTerrainMaterials.getTexturePaths()[i]);
            writeFloat(mapTerrainMaterials.getTextureScales()[i]);
        }
        for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
            writeStringNull(mapTerrainMaterials.getNormalPaths()[i]);
            writeFloat(mapTerrainMaterials.getNormalScales()[i]);
        }

        writeInt(0); // unknown
        writeInt(0); // unknown

        // decals
        if (!map.isUnexplored()) {
            writeInt(map.getDecalCount());
            for (Decal decal : map.getDecals()) {
                writeDecal(decal, map.getDecals().indexOf(decal));
            }
        } else {
            writeInt(0);
        }

        writeInt(map.getDecalGroupCount());
        for (DecalGroup decalGroup : map.getDecalGroups()) {
            writeDecalGroup(decalGroup, map.getDecalGroups().indexOf(decalGroup));
        }

        writeInt(map.getSize()); // width
        writeInt(map.getSize()); // height

        DDSHeader normalDDSHeader = new DDSHeader();
        normalDDSHeader.setWidth(map.getNormalMap().getWidth());
        normalDDSHeader.setHeight(map.getNormalMap().getHeight());
        normalDDSHeader.setFourCC("DXT5");

        // normal maps
        writeInt(1); // normal map count
        writeImageData(map.getNormalMap(), normalDDSHeader);

        DDSHeader textureMaskLowDDSHeader = new DDSHeader();
        textureMaskLowDDSHeader.setWidth(map.getTextureMasksLow().getWidth());
        textureMaskLowDDSHeader.setHeight(map.getTextureMasksLow().getHeight());
        textureMaskLowDDSHeader.setRGBBitCount(32);
        textureMaskLowDDSHeader.setRBitMask(0x00FF0000);
        textureMaskLowDDSHeader.setGBitMask(0x0000FF00);
        textureMaskLowDDSHeader.setBBitMask(0x000000FF);
        textureMaskLowDDSHeader.setABitMask(0xFF000000);

        writeImageData(map.getTextureMasksLow(), textureMaskLowDDSHeader);

        DDSHeader textureMaskHighDDSHeader = new DDSHeader();
        textureMaskHighDDSHeader.setWidth(map.getTextureMasksHigh().getWidth());
        textureMaskHighDDSHeader.setHeight(map.getTextureMasksHigh().getHeight());
        textureMaskHighDDSHeader.setRGBBitCount(32);
        textureMaskHighDDSHeader.setRBitMask(0x00FF0000);
        textureMaskHighDDSHeader.setGBitMask(0x0000FF00);
        textureMaskHighDDSHeader.setBBitMask(0x000000FF);
        textureMaskHighDDSHeader.setABitMask(0xFF000000);

        writeImageData(map.getTextureMasksHigh(), textureMaskHighDDSHeader);

        DDSHeader waterDDSHeader = new DDSHeader();
        waterDDSHeader.setWidth(map.getWaterMap().getWidth());
        waterDDSHeader.setHeight(map.getWaterMap().getHeight());
        waterDDSHeader.setFourCC("DXT5");
        byte[] waterHeaderBytes = waterDDSHeader.toBytes();

        // water maps
        writeInt(1); // unknown
        writeInt(waterHeaderBytes.length + map.getWaterMap().getWidth() * map.getWaterMap().getHeight()); // watermap byte count
        writeBytes(waterHeaderBytes); // dds header
        writeBytes(((DataBufferByte) map.getWaterMap().getData().getDataBuffer()).getData()); // watermap data
        writeBytes(((DataBufferByte) map.getWaterFoamMask().getData().getDataBuffer()).getData()); // water foam mask data
        writeBytes(((DataBufferByte) map.getWaterFlatnessMask().getData().getDataBuffer()).getData()); // water flatness mask data
        writeBytes(((DataBufferByte) map.getWaterDepthBiasMask().getData().getDataBuffer()).getData()); // water depth bias mask data

        // terrain type
        writeInts(((DataBufferInt) map.getTerrainType().getData().getDataBuffer()).getData()); // terrain type data

        // additional skybox
        if (map.getMinorVersion() >= 60) {
            SkyBox skyBox = map.getSkyBox();
            writeSkyBox(skyBox);
        }

        //props
        if (!map.isUnexplored()) {
            writeInt(map.getPropCount());
            for (Prop prop : map.getProps()) {
                writeProp(prop);
            }
        } else {
            writeInt(0);
        }

        out.flush();
        out.close();
    }

    public static void exportSCMapString(Path folderPath, String mapname, SCMap map) {
        map.writeToFile(folderPath.resolve(mapname).resolve("debug").resolve(mapname + ".txt"));
    }

    public static void exportPreview(Path folderPath, String mapname, SCMap map) throws IOException {
        final String fileFormat = "png";
        File previewFile = folderPath.resolve(mapname + "_preview." + fileFormat).toFile();
        RenderedImage renderedImage = PreviewGenerator.addMarkers(map.getPreview(), map);
        try {
            ImageIO.write(renderedImage, fileFormat, previewFile);
        } catch (IOException e) {
            System.out.print("Could not write the preview image\n" + e.toString());
        }
    }

    private static void writeFloat(float f) throws IOException {
        out.writeInt(swap(Float.floatToRawIntBits(f)));
    }

    private static void writeInt(int i) throws IOException {
        out.writeInt(swap(i));
    }

    private static void writeShort(short s) throws IOException {
        out.writeShort(swap(s));
    }

    private static void writeByte(byte b) throws IOException {
        out.writeByte(b);
    }

    private static void writeBytes(byte[] b) throws IOException {
        out.write(b);
    }

    private static void writeShorts(short[] s) throws IOException {
        for (short value : s) {
            writeShort(value);
        }
    }

    private static void writeInts(int[] data) throws IOException {
        for (int i : data) {
            writeInt(i);
        }
    }

    private static void writeStringNull(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            out.writeByte(s.charAt(i));
        }
        out.writeByte(0);
    }

    private static void writeString(String s) throws IOException {
        writeInt(s.length());
        for (int i = 0; i < s.length(); i++) {
            out.writeByte(s.charAt(i));
        }
    }

    private static void writeVector3f(Vector3f v) throws IOException {
        writeFloat(v.x);
        writeFloat(v.y);
        writeFloat(v.z);
    }

    private static void writeVector4f(Vector4f v) throws IOException {
        writeFloat(v.x);
        writeFloat(v.y);
        writeFloat(v.z);
        writeFloat(v.w);
    }

    private static void writeVector2f(Vector2f v) throws IOException {
        writeFloat(v.x);
        writeFloat(v.y);
    }

    private static void writeProp(Prop prop) throws IOException {
        writeStringNull(prop.getPath());
        writeVector3f(prop.getPosition());
        writeVector3f(new Vector3f((float) StrictMath.cos(prop.getRotation()), 0f, (float) StrictMath.sin(prop.getRotation())));
        writeVector3f(new Vector3f(0f, 1f, 0f));
        writeVector3f(new Vector3f((float) -StrictMath.sin(prop.getRotation()), 0f, (float) StrictMath.cos(prop.getRotation())));
        writeVector3f(new Vector3f(1f, 1f, 1f)); //scale
    }

    private static void writeDecal(Decal decal, int id) throws IOException {
        writeInt(id);
        writeInt(decal.getType().getTypeNum());
        writeInt(2);
        writeString(decal.getPath());
        writeString("");
        writeVector3f(decal.getScale());
        writeVector3f(decal.getPosition());
        writeVector3f(decal.getRotation());
        writeFloat(decal.getCutOffLOD());
        writeFloat(0);
        writeInt(-1);
    }

    private static void writeDecalGroup(DecalGroup decalGroup, int id) throws IOException {
        writeInt(id);
        writeStringNull(decalGroup.getName());
        writeInt(decalGroup.getData().length);
        for (int j = 0; j < decalGroup.getData().length; j++) {
            writeInt(decalGroup.getData()[j]);
        }
    }

    private static void writeWaveGenerator(WaveGenerator waveGenerator) throws IOException {
        writeStringNull(waveGenerator.getTextureName());
        writeStringNull(waveGenerator.getRampName());
        writeVector3f(waveGenerator.getPosition());
        writeFloat(waveGenerator.getRotation());
        writeVector3f(waveGenerator.getVelocity());
        writeFloat(waveGenerator.getLifeTimeFirst());
        writeFloat(waveGenerator.getLifeTimeSecond());
        writeFloat(waveGenerator.getPeriodFirst());
        writeFloat(waveGenerator.getPeriodSecond());
        writeFloat(waveGenerator.getScaleFirst());
        writeFloat(waveGenerator.getScaleSecond());
        writeFloat(waveGenerator.getFrameCount());
        writeFloat(waveGenerator.getFrameRateFirst());
        writeFloat(waveGenerator.getFrameRateSecond());
        writeFloat(waveGenerator.getStripCount());
    }

    private static void writeWaterSettings(WaterSettings waterSettings) throws IOException {
        writeByte((byte) (waterSettings.isWaterPresent() ? 1 : 0));
        writeFloat(waterSettings.getElevation());
        writeFloat(waterSettings.getElevationDeep());
        writeFloat(waterSettings.getElevationAbyss());
        writeVector3f(waterSettings.getSurfaceColor());
        writeVector2f(waterSettings.getColorLerp());
        writeFloat(waterSettings.getRefractionScale());
        writeFloat(waterSettings.getFresnelBias());
        writeFloat(waterSettings.getFresnelPower());
        writeFloat(waterSettings.getUnitReflection());
        writeFloat(waterSettings.getSkyReflection());
        writeFloat(waterSettings.getSunShininess());
        writeFloat(waterSettings.getSunStrength());
        writeVector3f(waterSettings.getSunDirection());
        writeVector3f(waterSettings.getSunColor());
        writeFloat(waterSettings.getSunReflection());
        writeFloat(waterSettings.getSunGlow());
        writeStringNull(waterSettings.getTexPathCubemap());
        writeStringNull(waterSettings.getTexPathWaterRamp());

        // waves
        for (WaterSettings.WaveTexture waveTexture : waterSettings.getWaveTextures()) {
            writeFloat(waveTexture.getNormalRepeat());
        }

        for (WaterSettings.WaveTexture waveTexture : waterSettings.getWaveTextures()) {
            writeVector2f(waveTexture.getNormalMovement());
            writeStringNull(waveTexture.getTexPath());
        }
    }

    private static void writeLightingSettings(LightingSettings lightingSettings) throws IOException {
        writeFloat(lightingSettings.getLightingMultiplier());
        writeVector3f(lightingSettings.getSunDirection());
        writeVector3f(lightingSettings.getSunAmbience());
        writeVector3f(lightingSettings.getSunColor());
        writeVector3f(lightingSettings.getShadowFillColor());
        writeVector4f(lightingSettings.getSpecularColor());
        writeFloat(lightingSettings.getBloom());
        writeVector3f(lightingSettings.getFogColor());
        writeFloat(lightingSettings.getFogStart());
        writeFloat(lightingSettings.getFogEnd());
    }

    private static void writeSkyBox(SkyBox skyBox) throws IOException {
        writeVector3f(skyBox.getPosition());
        writeFloat(skyBox.getHorizonHeight());
        writeFloat(skyBox.getScale());
        writeFloat(skyBox.getSubHeight());
        writeInt(skyBox.getSubDivAx());
        writeInt(skyBox.getSubDivHeight());
        writeFloat(skyBox.getZenithHeight());
        writeVector3f(skyBox.getHorizonColor());
        writeVector3f(skyBox.getZenithColor());
        writeFloat(skyBox.getDecalGlowMultiplier());

        writeStringNull(skyBox.getAlbedo());
        writeStringNull(skyBox.getGlow());

        // Array of Planets/Stars
        writeInt(skyBox.getPlanets().length);
        for (SkyBox.Planet planet : skyBox.getPlanets()) {
            writeVector3f(planet.getPosition());
            writeFloat(planet.getRotation());
            writeVector2f(planet.getScale());
            writeVector4f(planet.getUv());
        }

        // Mid
        writeByte((byte) skyBox.getMidRgbColor().getRed());
        writeByte((byte) skyBox.getMidRgbColor().getBlue());
        writeByte((byte) skyBox.getMidRgbColor().getGreen());

        // Cirrus
        writeFloat(skyBox.getCirrusMultiplier());
        writeVector3f(skyBox.getCirrusColor());
        writeStringNull(skyBox.getCirrusTexture());

        writeInt(skyBox.getCirrusLayers().length);
        for (SkyBox.Cirrus cirrus : skyBox.getCirrusLayers()) {
            writeVector2f(cirrus.getFrequency());
            writeFloat(cirrus.getSpeed());
            writeVector2f(cirrus.getDirection());
        }
        writeFloat(skyBox.getClouds7());
    }

    private static void writeImageData(BufferedImage image, DDSHeader ddsHeader) throws IOException {
        byte[] headerBytes = ddsHeader.toBytes();
        writeInt(headerBytes.length + image.getWidth() * image.getHeight() * 4); // image byte count
        writeBytes(headerBytes);
        writeInts(((DataBufferInt) image.getData().getDataBuffer()).getData()); // image data
    }
}
