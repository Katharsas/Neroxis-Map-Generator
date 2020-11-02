package map;

import brushes.Brushes;
import generator.VisualDebugger;
import lombok.Getter;
import lombok.SneakyThrows;
import util.Util;
import util.Vector2f;
import util.Vector3f;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

import static brushes.Brushes.loadBrush;

@Getter
public strictfp class FloatMask extends Mask {
    private final Random random;
    private float[][] mask;

    public FloatMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this.mask = new float[size][size];
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = null;
        }
        this.symmetrySettings = symmetrySettings;
        for (int y = 0; y < this.getSize(); y++) {
            for (int x = 0; x < this.getSize(); x++) {
                this.mask[x][y] = 0f;
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    public FloatMask(BufferedImage image, Long seed, SymmetrySettings symmetrySettings) {
        this.mask = new float[image.getWidth()][image.getHeight()];
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = null;
        }
        Raster imageData = image.getData();
        this.symmetrySettings = symmetrySettings;
        for (int y = 0; y < this.getSize(); y++) {
            for (int x = 0; x < this.getSize(); x++) {
                int[] vals = new int[1];
                imageData.getPixel(x, y, vals);
                this.mask[x][y] = vals[0] / 255f;
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    public FloatMask(FloatMask mask, Long seed) {
        this.mask = new float[mask.getSize()][mask.getSize()];
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = null;
        }
        this.symmetrySettings = mask.getSymmetrySettings();
        for (int y = 0; y < mask.getSize(); y++) {
            for (int x = 0; x < mask.getSize(); x++) {
                this.mask[x][y] = mask.get(x, y);
            }
        }
        VisualDebugger.visualizeMask(this);
    }

    public int getSize() {
        return mask[0].length;
    }

    public float get(Vector2f pos) {
        return mask[(int) pos.x][(int) pos.y];
    }

    public float get(int x, int y) {
        return mask[x][y];
    }

    public float getMin() {
        float val = Float.MAX_VALUE;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                val = StrictMath.min(val, get(x, y));
            }
        }
        return val;
    }

    public float getMax() {
        float val = 0;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                val = StrictMath.max(val, get(x, y));
            }
        }
        return val;
    }

    public float getSum() {
        float val = 0;
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                val += get(x, y);
            }
        }
        return val;
    }

    public float getAvg() {
        return getSum() / getSize() / getSize();
    }

    public boolean isLocalMax(int x, int y) {
        float value = get(x, y);
        return ((x > 0 && get(x - 1, y) <= value)
                && (y > 0 && get(x, y - 1) <= value)
                && (x < getSize() - 1 && get(x + 1, y) <= value)
                && (y < getSize() - 1 && get(x, y + 1) <= value)
                && (get(x - 1, y - 1) <= value)
                && (get(x + 1, y - 1) <= value)
                && (get(x + 1, y + 1) <= value)
                && (get(x + 1, y + 1) <= value));
    }

    public void set(Vector2f location, float value) {
        set((int) location.x, (int) location.y, value);
    }

    public void set(Vector3f location, float value) {
        set((int) location.x, (int) location.z, value);
    }

    public void set(int x, int y, float value) {
        mask[x][y] = value;
    }

    public void add(int x, int y, float value) {
        mask[x][y] += value;
    }

    public void subtract(int x, int y, float value) {
        add(x, y, -value);
    }

    public void multiply(int x, int y, float value) {
        mask[x][y] *= value;
    }

    public FloatMask init(BinaryMask other, float low, float high) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (other.get(x, y)) {
                    set(x, y, high);
                } else {
                    set(x, y, low);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask copy() {
        if (random != null) {
            return new FloatMask(this, random.nextLong());
        } else {
            return new FloatMask(this, null);
        }
    }

    public FloatMask clear() {
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                set(x, y, 0);
            }
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask multiply(FloatMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                multiply(x, y, other.get(x, y));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask multiply(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                multiply(x, y, val);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask add(FloatMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                add(x, y, other.get(x, y));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask add(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                add(x, y, val);
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask subtract(FloatMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        add(other.copy().multiply(-1));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask add(FloatMask other, Vector2f loc) {
        return add(other, (int) loc.x, (int) loc.y);
    }

    public FloatMask subtract(FloatMask other, Vector2f loc) {
        return add(other.copy().multiply(-1f), loc);
    }

    public FloatMask add(FloatMask other, int offsetX, int offsetY) {
        for (int y = 0; y < other.getSize(); y++) {
            for (int x = 0; x < other.getSize(); x++) {
                int shiftX = x - other.getSize() / 2 + offsetX;
                int shiftY = y - other.getSize() / 2 + offsetY;
                if (inBounds(shiftX, shiftY)) {
                    add(shiftX, shiftY, other.get(x, y));
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask subtract(FloatMask other, int offsetX, int offsetY) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        return add(other.copy().multiply(-1f), offsetX, offsetY);
    }

    public FloatMask add(BinaryMask other, float value) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        FloatMask otherFloat = new FloatMask(getSize(), null, symmetrySettings);
        otherFloat.init(other, 0, value);
        add(otherFloat);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addGaussianNoise(float scale) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                add(x, y, (float) random.nextGaussian() * scale);
            }
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask addWhiteNoise(float scale) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                add(x, y, random.nextFloat() * scale);
            }
        }
        applySymmetry();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask subtract(BinaryMask other, float value) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        FloatMask otherFloat = new FloatMask(getSize(), null, symmetrySettings);
        otherFloat.init(other, 0, -value);
        add(otherFloat);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask sqrt() {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                set(x, y, (float) StrictMath.sqrt(get(x, y)));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask min(FloatMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                set(x, y, StrictMath.min(get(x, y), other.get(x, y)));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask clampMin(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                set(x, y, StrictMath.max(get(x, y), val));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask threshold(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (get(x, y) < val) {
                    set(x, y, 0f);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask max(FloatMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                set(x, y, StrictMath.max(get(x, y), other.get(x, y)));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask clampMax(float val) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                set(x, y, StrictMath.min(get(x, y), val));
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask enlarge(int size) {
        float[][] largeMask = new float[size][size];
        int smallX;
        int smallY;
        for (int x = 0; x < size; x++) {
            smallX = StrictMath.min(x / (size / getSize()), getSize() - 1);
            for (int y = 0; y < size; y++) {
                smallY = StrictMath.min(y / (size / getSize()), getSize() - 1);
                largeMask[x][y] = get(smallX, smallY);
            }
        }
        mask = largeMask;
        applySymmetry(symmetrySettings.getSpawnSymmetry());
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask enlarge2 (int size) {
        float[][] largeMask = new float[size][size];
        int oldSize = getSize();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                largeMask[x][y] = get(StrictMath.round(x / (size / oldSize)), StrictMath.round(y / (size / oldSize)));
            }
        }
        mask = largeMask;
        applySymmetry(symmetrySettings.getSpawnSymmetry());
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask shrink2 (int size) {
        float[][] smallMask = new float[size][size];
        int oldSize = getSize();
        float sum = 0;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < (oldSize / size); z++) {
                    for (int w = 0; w < (oldSize / size); w++) {
                        sum += get((x * oldSize / size) + z, (y * oldSize / size) + w);
                    }
                }
                smallMask[x][y] = sum / oldSize * size / oldSize * size;
                sum = 0;
            }
        }
        mask = smallMask;
        applySymmetry(symmetrySettings.getSpawnSymmetry());
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask shrink(int size) {
        float[][] smallMask = new float[size][size];
        int largeX;
        int largeY;
        for (int x = 0; x < size; x++) {
            largeX = (x * getSize()) / size + (getSize() / size / 2);
            if (largeX >= getSize())
                largeX = getSize() - 1;
            for (int y = 0; y < size; y++) {
                largeY = (y * getSize()) / size + (getSize() / size / 2);
                if (largeY >= getSize())
                    largeY = getSize() - 1;
                smallMask[x][y] = get(largeX, largeY);
            }
        }
        mask = smallMask;
        VisualDebugger.visualizeMask(this);
        applySymmetry(symmetrySettings.getTeamSymmetry());
        return this;
    }

    public FloatMask setSize(int size) {
        if (getSize() > size) {
            shrink2(size);
        }
        if (getSize() < size) {
            enlarge2(size);
        }
        return this;
    }

    public FloatMask removeValuesOutsideOf(BinaryMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (!other.get(x, y)) {
                    set(x, y, 0f);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask removeValuesInsideOf(BinaryMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (other.get(x, y)) {
                    set(x, y, 0f);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask removeValuesOutsideOfRange(float min, float max) {
        for (int y = 0; y < getSize(); y++) {
            for (int x = 0; x < getSize(); x++) {
                if (this.get(x, y) < min || this.get(x, y) > max) {
                    set(x, y, 0f);
                }
            }
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask removeValuesInRange(float min, float max) {
        subtract(this.copy().removeValuesOutsideOfRange(min, max));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask replaceValuesInRangeWith(BinaryMask range, FloatMask replacement) {
        if (range.getSize() != getSize() || replacement.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        removeValuesInsideOf(range).add(replacement.copy().removeValuesOutsideOf(range));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask copyWithinRange(BinaryMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        return copy().removeValuesOutsideOf(other);
    }

    public FloatMask copyOutsideRange(BinaryMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        return copy().removeValuesInsideOf(other);
    }

    public FloatMask smoothWithinSpecifiedDistanceOfEdgesOf(BinaryMask other, int distance) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        for (int x = 0; x < distance; x = x + 2) {
            replaceValuesInRangeWith(other.getAreasWithinSpecifiedDistanceOfEdges(x + 1), copy().smooth(1));
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask reduceValuesOnIntersectingSmoothingZones(BinaryMask avoidMakingZonesHere, float floatMax) {
        if (avoidMakingZonesHere.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        avoidMakingZonesHere = avoidMakingZonesHere.copy();
        FloatMask newMaskInZones = copy().smooth(34).subtract(copy()).subtract(avoidMakingZonesHere, 1f * floatMax);
        BinaryMask zones = newMaskInZones.copy().removeValuesInRange(0f * floatMax, 0.5f * floatMax).smooth(2).convertToBinaryMask(0.5f * floatMax, 1f * floatMax).inflate(34);
        BinaryMask newMaskInZonesBase = convertToBinaryMask(1f * floatMax, 1f * floatMax).deflate(3).minus(zones.copy().invert());
        newMaskInZones.init(newMaskInZonesBase, 0, 1).smooth(4).clampMax(0.35f * floatMax).add(newMaskInZonesBase, 1f * floatMax).smooth(2).clampMax(0.65f * floatMax).add(newMaskInZonesBase, 1f * floatMax).smooth(1).add(newMaskInZonesBase, 1f * floatMax).clampMax(1f * floatMax);
        replaceValuesInRangeWith(zones, newMaskInZones).smoothWithinSpecifiedDistanceOfEdgesOf(zones, 30);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask convertToBinaryMask(float minValueToConvert, float maxValueToConvert) {
        BinaryMask newMask = new BinaryMask(this.copy().removeValuesOutsideOfRange(minValueToConvert, maxValueToConvert), minValueToConvert, random.nextLong());
        VisualDebugger.visualizeMask(this);
        return newMask;
    }

    public FloatMask getDistanceFieldForRange(float minValue, float maxValue) {
        convertToBinaryMask(minValue, maxValue).getDistanceField();
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask removeAreasOutsideOfSpecifiedIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        FloatMask tempMask2 = copy().init(this.copy().convertToBinaryMask(minIntensity, maxIntensity).removeAreasOutsideOfSpecifiedSize(minSize, maxSize).invert(), 0f, 1f);
        this.subtract(tempMask2).clampMin(0f);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask removeAreasOfSpecifiedIntensityAndSize(int minSize, int maxSize, float minIntensity, float maxIntensity) {
        subtract(this.copy().removeAreasOutsideOfSpecifiedIntensityAndSize(minSize, maxSize, minIntensity, maxIntensity));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask removeAreasOfSpecifiedSizeWithLocalMaximums(int minSize, int maxSize, int levelOfPrecision, float floatMax) {
        for (int x = 0; x < levelOfPrecision; x++) {
            removeAreasOfSpecifiedIntensityAndSize(minSize, maxSize, ((1f - (float) x / (float) levelOfPrecision) * floatMax), 1f * floatMax);
        }
        removeAreasOfSpecifiedIntensityAndSize(minSize, maxSize, 0.0000001f, 1f * floatMax);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public BinaryMask getLocalMaximums(int minValue, int maxValue) {
        BinaryMask localMaxima = new BinaryMask(getSize(), random.nextLong(), symmetrySettings);
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                float value = get(x, y);
                if (value > minValue && value < maxValue && isLocalMax(x, y)) {
                    localMaxima.set(x, y, true);
                }
            }
        }
        return localMaxima;
    }

    public FloatMask maskToHills(BinaryMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        FloatMask brush = loadBrush(Brushes.HILL_BRUSHES[random.nextInt(Brushes.HILL_BRUSHES.length)], symmetrySettings);
        BinaryMask otherCopy = other.copy().fillHalf(false);
        FloatMask otherDistance = other.copy().invert().getDistanceField();
        LinkedList<Vector2f> coordinates = new LinkedList<>(otherCopy.getRandomCoordinates(4));
        while (coordinates.size() > 0) {
            Vector2f loc = coordinates.removeFirst();
            FloatMask useBrush = brush.copy().shrink((int) otherDistance.get(loc) * 4).multiply(otherDistance.get(loc));
            add(useBrush, loc);
            add(useBrush, getSymmetryPoint(loc));
            coordinates.removeIf(cloc -> loc.getDistance(cloc) < otherDistance.get(loc));
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask maskToMountains(BinaryMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        FloatMask brush = loadBrush(Brushes.MOUNTAIN_BRUSHES[random.nextInt(Brushes.MOUNTAIN_BRUSHES.length)], symmetrySettings);
        brush.multiply(1 / brush.getMax());
        BinaryMask otherCopy = other.copy().fillHalf(false);
        FloatMask otherDistance = other.copy().invert().getDistanceField();
        LinkedList<Vector2f> coordinates = new LinkedList<>(otherCopy.getRandomCoordinates(4));
        while (coordinates.size() > 0) {
            Vector2f loc = coordinates.removeFirst();
            FloatMask useBrush = brush.copy().shrink((int) otherDistance.get(loc) * 4).multiply(otherDistance.get(loc));
            add(useBrush, loc);
            add(useBrush, getSymmetryPoint(loc));
            coordinates.removeIf(cloc -> loc.getDistance(cloc) < otherDistance.get(loc));
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask maskToOceanHeights(float underWaterSlope, BinaryMask other) {
        if (other.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        FloatMask otherDistance = other.getDistanceField();
        add(otherDistance.multiply(-underWaterSlope));
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask getInnerCount() {
        FloatMask innerCount = new FloatMask(getSize(), null, symmetrySettings);

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                float val = get(x, y);
                innerCount.set(x, y, StrictMath.round(val * 1000) / 1000f);
                innerCount.add(x, y, x > 0 ? innerCount.get(x - 1, y) : 0);
                innerCount.add(x, y, y > 0 ? innerCount.get(x, y - 1) : 0);
                innerCount.subtract(x, y, x > 0 && y > 0 ? innerCount.get(x - 1, y - 1) : 0);
            }
        }

        return innerCount;
    }

    public FloatMask smooth(int radius) {
        FloatMask innerCount = getInnerCount();

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                int xLeft = StrictMath.max(0, x - radius);
                int xRight = StrictMath.min(getSize() - 1, x + radius);
                int yUp = StrictMath.max(0, y - radius);
                int yDown = StrictMath.min(getSize() - 1, y + radius);
                float countA = xLeft > 0 && yUp > 0 ? innerCount.get(xLeft - 1, yUp - 1) : 0;
                float countB = yUp > 0 ? innerCount.get(xRight, yUp - 1) : 0;
                float countC = xLeft > 0 ? innerCount.get(xLeft - 1, yDown) : 0;
                float countD = innerCount.get(xRight, yDown);
                float count = countD + countA - countB - countC;
                int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
                set(x, y, count / area);
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask smooth(int radius, BinaryMask limiter) {
        if (limiter.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        FloatMask innerCount = getInnerCount();

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (limiter.get(x, y)) {
                    int xLeft = StrictMath.max(0, x - radius);
                    int xRight = StrictMath.min(getSize() - 1, x + radius);
                    int yUp = StrictMath.max(0, y - radius);
                    int yDown = StrictMath.min(getSize() - 1, y + radius);
                    float countA = xLeft > 0 && yUp > 0 ? innerCount.get(xLeft - 1, yUp - 1) : 0;
                    float countB = yUp > 0 ? innerCount.get(xRight, yUp - 1) : 0;
                    float countC = xLeft > 0 ? innerCount.get(xLeft - 1, yDown) : 0;
                    float countD = innerCount.get(xRight, yDown);
                    float count = countD + countA - countB - countC;
                    int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
                    set(x, y, count / area);
                }
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask spike(int radius) {
        FloatMask innerCount = getInnerCount();

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                int xLeft = StrictMath.max(0, x - radius);
                int xRight = StrictMath.min(getSize() - 1, x + radius);
                int yUp = StrictMath.max(0, y - radius);
                int yDown = StrictMath.min(getSize() - 1, y + radius);
                float countA = xLeft > 0 && yUp > 0 ? innerCount.get(xLeft - 1, yUp - 1) : 0;
                float countB = yUp > 0 ? innerCount.get(xRight, yUp - 1) : 0;
                float countC = xLeft > 0 ? innerCount.get(xLeft - 1, yDown) : 0;
                float countD = innerCount.get(xRight, yDown);
                float count = countD + countA - countB - countC;
                int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
                set(x, y, count / area * count / area);
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask spike(int radius, BinaryMask limiter) {
        if (limiter.getSize() != getSize()) {
            throw new IllegalArgumentException("Masks not the same size");
        }
        FloatMask innerCount = getInnerCount();

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (limiter.get(x, y)) {
                    int xLeft = StrictMath.max(0, x - radius);
                    int xRight = StrictMath.min(getSize() - 1, x + radius);
                    int yUp = StrictMath.max(0, y - radius);
                    int yDown = StrictMath.min(getSize() - 1, y + radius);
                    float countA = xLeft > 0 && yUp > 0 ? innerCount.get(xLeft - 1, yUp - 1) : 0;
                    float countB = yUp > 0 ? innerCount.get(xRight, yUp - 1) : 0;
                    float countC = xLeft > 0 ? innerCount.get(xLeft - 1, yDown) : 0;
                    float countD = innerCount.get(xRight, yDown);
                    float count = countD + countA - countB - countC;
                    int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
                    set(x, y, count / area * count / area);
                }
            }
        }

        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask gradient() {
        float[][] maskCopy = new float[getSize()][getSize()];
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                int xNeg = StrictMath.max(0, x - 1);
                int xPos = StrictMath.min(getSize() - 1, x + 1);
                int yNeg = StrictMath.max(0, y - 1);
                int yPos = StrictMath.min(getSize() - 1, y + 1);
                float xSlope = get(xPos, y) - get(xNeg, y);
                float ySlope = get(x, yPos) - get(x, yNeg);
                maskCopy[x][y] = (float) StrictMath.sqrt(xSlope * xSlope + ySlope * ySlope);
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public FloatMask supcomGradient() {
        float[][] maskCopy = new float[getSize()][getSize()];
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                int xPos = StrictMath.min(getSize() - 1, x + 1);
                int yPos = StrictMath.min(getSize() - 1, y + 1);
                float xSlope = StrictMath.abs(get(x, y) - get(xPos, y));
                float ySlope = StrictMath.abs(get(x, y) - get(x, yPos));
                float diagSlope = StrictMath.abs(get(x, y) - get(xPos, yPos));
                maskCopy[x][y] = Collections.max(Arrays.asList(xSlope, ySlope, diagSlope));
            }
        }
        mask = maskCopy;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public void applySymmetry() {
        applySymmetry(symmetrySettings.getTerrainSymmetry());
    }

    public void applySymmetry(Symmetry symmetry) {
        applySymmetry(symmetrySettings.getTerrainSymmetry(), false);
    }

    public void applySymmetry(boolean reverse) {
        applySymmetry(symmetrySettings.getTerrainSymmetry(), reverse);
    }

    public void applySymmetry(Symmetry symmetry, boolean reverse) {
        switch (symmetry) {
            case QUAD, DIAG -> {
                for (int x = getMinXBound(symmetry); x < getMaxXBound(symmetry); x++) {
                    for (int y = getMinYBound(x, symmetry); y < getMaxYBound(x, symmetry); y++) {
                        Vector2f[] symPoints = getTerrainSymmetryPoints(x, y, symmetry);
                        for (Vector2f symPoint : symPoints) {
                            set(symPoint, get(x, y));
                        }
                    }
                }
            }
            default -> {
                for (int x = getMinXBound(symmetry); x < getMaxXBound(symmetry); x++) {
                    for (int y = getMinYBound(x, symmetry); y < getMaxYBound(x, symmetry); y++) {
                        Vector2f symPoint = getSymmetryPoint(x, y, symmetry);
                        if (reverse) {
                            set(x, y, get(symPoint));
                        } else {
                            set(symPoint, get(x, y));
                        }
                    }
                }
            }
        }
    }

    public void applySymmetry(float angle) {
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (inHalf(x, y, angle)) {
                    Vector2f symPoint = getSymmetryPoint(x, y, Symmetry.POINT);
                    set(symPoint, get(x, y));
                }
            }
        }
    }
    // -------------------------------------------

    @SneakyThrows
    public void writeToFile(Path path) {
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path.toFile())));

        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                out.writeFloat(get(x, y));
            }
        }

        out.close();
    }

    public String toHash() throws NoSuchAlgorithmException {
        ByteBuffer bytes = ByteBuffer.allocate(getSize() * getSize() * 4);
        for (int x = getMinXBound(symmetrySettings.getSpawnSymmetry()); x < getMaxXBound(symmetrySettings.getSpawnSymmetry()); x++) {
            for (int y = getMinYBound(x, symmetrySettings.getSpawnSymmetry()); y < getMaxYBound(x, symmetrySettings.getSpawnSymmetry()); y++) {
                bytes.putFloat(get(x, y));
            }
        }
        byte[] data = MessageDigest.getInstance("MD5").digest(bytes.array());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte datum : data) {
            stringBuilder.append(String.format("%02x", datum));
        }
        return stringBuilder.toString();
    }

    public void show() {
        VisualDebugger.visualizeMask(this);
    }

    public void startVisualDebugger(String maskName) {
        startVisualDebugger(maskName, Util.getStackTraceParentClass());
    }

    public void startVisualDebugger(String maskName, String parentClass) {
        VisualDebugger.whitelistMask(this, maskName, parentClass);
        show();
    }
}
