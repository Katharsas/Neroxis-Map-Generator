package map;

import generator.VisualDebugger;
import lombok.Getter;
import util.Util;
import util.Vector2f;
import util.Vector3f;

import java.util.ArrayList;
import java.util.Random;

@Getter
public strictfp abstract class Mask<T> {
    protected final Random random;
    protected T[][] mask;
    protected SymmetrySettings symmetrySettings;

    protected Mask(Long seed) {
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = null;
        }
    }

    protected abstract T[][] getEmptyMask(int size);

    public abstract Mask<T> interpolate();

    public T getValueAt(Vector3f location) {
        return getValueAt((int) location.x, (int) location.z);
    }

    public T getValueAt(Vector2f location) {
        return getValueAt((int) location.x, (int) location.y);
    }

    public T getValueAt(int x, int y) {
        return mask[x][y];
    }

    protected void setValueAt(Vector3f location, T value) {
        setValueAt((int) location.x, (int) location.z, value);
    }

    protected void setValueAt(Vector2f location, T value) {
        setValueAt((int) location.x, (int) location.y, value);
    }

    protected void setValueAt(int x, int y, T value) {
        mask[x][y] = value;
    }

    public int getSize() {
        return mask[0].length;
    }

    public Mask<T> setSize(int size) {
        if (getSize() < size)
            enlarge(size);
        if (getSize() > size) {
            shrink(size);
        }
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public boolean inBounds(Vector3f location) {
        return inBounds(new Vector2f(location));
    }

    public boolean inBounds(Vector2f location) {
        return inBounds((int) location.x, (int) location.y);
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < getSize() && y >= 0 && y < getSize();
    }

    public ArrayList<SymmetryPoint> getSymmetryPoints(Vector3f v, SymmetryType symmetryType) {
        return getSymmetryPoints(new Vector2f(v), symmetryType);
    }

    public ArrayList<SymmetryPoint> getSymmetryPoints(Vector2f v, SymmetryType symmetryType) {
        return getSymmetryPoints(v.x, v.y, symmetryType);
    }

    public ArrayList<SymmetryPoint> getSymmetryPoints(float x, float y, SymmetryType symmetryType) {
        ArrayList<SymmetryPoint> symmetryPoints = new ArrayList<>();
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        switch (symmetry) {
            case POINT2 -> symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - x - 1, getSize() - y - 1), Symmetry.POINT2));
            case POINT4 -> {
                symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - x - 1, getSize() - y - 1), Symmetry.POINT2));
                symmetryPoints.add(new SymmetryPoint(new Vector2f(y, getSize() - x - 1), Symmetry.POINT2));
                symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - y - 1, x), Symmetry.POINT2));
            }
            case POINT6, POINT8, POINT10, POINT12, POINT14, POINT16 -> {
                symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - x - 1, getSize() - y - 1), Symmetry.POINT2));
                for (int i = 1; i < symmetry.getNumSymPoints() / 2; i++) {
                    float angle = (float) (2 * StrictMath.PI * i / symmetry.getNumSymPoints());
                    float newX = (float) ((x - getSize() / 2f) * StrictMath.cos(angle) - (y - getSize() / 2f) * StrictMath.sin(angle) + getSize() / 2f);
                    float newY = (float) ((x - getSize() / 2f) * StrictMath.sin(angle) + (y - getSize() / 2f) * StrictMath.cos(angle) + getSize() / 2f);
                    if (inBounds((int) newX, (int) newY)) {
                        symmetryPoints.add(new SymmetryPoint(new Vector2f(newX, newY), Symmetry.POINT2));
                    }
                    angle += StrictMath.PI;
                    newX = (float) ((x - getSize() / 2f) * StrictMath.cos(angle) - (y - getSize() / 2f) * StrictMath.sin(angle) + getSize() / 2f);
                    newY = (float) ((x - getSize() / 2f) * StrictMath.sin(angle) + (y - getSize() / 2f) * StrictMath.cos(angle) + getSize() / 2f);
                    if (inBounds((int) newX, (int) newY)) {
                        symmetryPoints.add(new SymmetryPoint(new Vector2f(newX, newY), Symmetry.POINT2));
                    }
                }
            }
            case POINT3, POINT5, POINT7, POINT9, POINT11, POINT13, POINT15 -> {
                for (int i = 1; i < symmetry.getNumSymPoints(); i++) {
                    float angle = (float) (2 * StrictMath.PI * i / symmetry.getNumSymPoints());
                    float newX = (float) ((x - getSize() / 2f) * StrictMath.cos(angle) - (y - getSize() / 2f) * StrictMath.sin(angle) + getSize() / 2f);
                    float newY = (float) ((x - getSize() / 2f) * StrictMath.sin(angle) + (y - getSize() / 2f) * StrictMath.cos(angle) + getSize() / 2f);
                    if (inBounds((int) newX, (int) newY)) {
                        symmetryPoints.add(new SymmetryPoint(new Vector2f(newX, newY), Symmetry.POINT2));
                    }
                }
            }
            case X -> symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - x - 1, y), Symmetry.X));
            case Z -> symmetryPoints.add(new SymmetryPoint(new Vector2f(x, getSize() - y - 1), Symmetry.Z));
            case XZ -> symmetryPoints.add(new SymmetryPoint(new Vector2f(y, x), Symmetry.XZ));
            case ZX -> symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - y - 1, getSize() - x - 1), Symmetry.ZX));
            case QUAD -> {
                if (symmetrySettings.getTeamSymmetry() == Symmetry.Z) {
                    symmetryPoints.add(new SymmetryPoint(new Vector2f(x, getSize() - y - 1), Symmetry.Z));
                    symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - x - 1, y), Symmetry.X));
                    symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - x - 1, getSize() - y - 1), Symmetry.Z));
                } else {
                    symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - x - 1, y), Symmetry.X));
                    symmetryPoints.add(new SymmetryPoint(new Vector2f(x, getSize() - y - 1), Symmetry.Z));
                    symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - x - 1, getSize() - y - 1), Symmetry.X));
                }
            }
            case DIAG -> {
                if (symmetrySettings.getTeamSymmetry() == Symmetry.ZX) {
                    symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - y - 1, getSize() - x - 1), Symmetry.ZX));
                    symmetryPoints.add(new SymmetryPoint(new Vector2f(y, x), Symmetry.XZ));
                    symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - x - 1, getSize() - y - 1), Symmetry.ZX));
                } else {
                    symmetryPoints.add(new SymmetryPoint(new Vector2f(y, x), Symmetry.XZ));
                    symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - y - 1, getSize() - x - 1), Symmetry.ZX));
                    symmetryPoints.add(new SymmetryPoint(new Vector2f(getSize() - x - 1, getSize() - y - 1), Symmetry.XZ));
                }
            }
        }
        return symmetryPoints;
    }

    public ArrayList<Float> getSymmetryRotation(float rot) {
        return getSymmetryRotation(rot, SymmetryType.SPAWN);
    }

    public ArrayList<Float> getSymmetryRotation(float rot, SymmetryType symmetryType) {
        ArrayList<Float> symmetryRotation = new ArrayList<>();
        final float xRotation = (float) StrictMath.atan2(-StrictMath.sin(rot), StrictMath.cos(rot));
        final float zRotation = (float) StrictMath.atan2(-StrictMath.cos(rot), StrictMath.sin(rot));
        final float diagRotation = (float) StrictMath.atan2(-StrictMath.cos(rot), -StrictMath.sin(rot));
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        switch (symmetry) {
            case POINT2 -> symmetryRotation.add(rot + (float) StrictMath.PI);
            case POINT4 -> {
                symmetryRotation.add(rot + (float) StrictMath.PI);
                symmetryRotation.add(rot + (float) StrictMath.PI / 2);
                symmetryRotation.add(rot - (float) StrictMath.PI / 2);
            }
            case POINT3, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15, POINT16 -> {
                for (int i = 1; i < symmetry.getNumSymPoints(); i++) {
                    symmetryRotation.add(rot + (float) (2 * StrictMath.PI * i / symmetry.getNumSymPoints()));
                }
            }
            case X -> symmetryRotation.add(xRotation);
            case Z -> symmetryRotation.add(zRotation);
            case XZ, ZX -> symmetryRotation.add(diagRotation);
            case QUAD -> {
                if (symmetrySettings.getTeamSymmetry() == Symmetry.Z) {
                    symmetryRotation.add(zRotation);
                    symmetryRotation.add(xRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                } else {
                    symmetryRotation.add(xRotation);
                    symmetryRotation.add(zRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                }
            }
            case DIAG -> {
                if (symmetrySettings.getTeamSymmetry() == Symmetry.ZX) {
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                } else {
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                }
            }
        }
        return symmetryRotation;
    }

    protected int getMinXBound(SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        return switch (symmetry) {
            default -> 0;
        };
    }

    protected int getMaxXBound(SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        return switch (symmetry) {
            case POINT2, POINT3, POINT4, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15, POINT16 -> StrictMath.max(getMaxXFromAngle(360f / symmetry.getNumSymPoints()), getSize() / 2);
            case X, QUAD, DIAG -> getSize() / 2;
            default -> getSize();
        };
    }

    protected int getMinYBound(int x, SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        return switch (symmetry) {
            case POINT2, POINT3, POINT4, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15, POINT16 -> getMinYFromXOnArc(x, 360f / symmetry.getNumSymPoints());
            case DIAG, XZ -> x;
            default -> 0;
        };
    }

    protected int getMaxYBound(int x, SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        return switch (symmetry) {
            case POINT2, POINT3, POINT4, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15, POINT16 -> getMaxYFromXOnArc(x, 360f / symmetry.getNumSymPoints());
            case ZX, DIAG -> getSize() - x;
            case Z, QUAD -> getSize() / 2;
            default -> getSize();
        };
    }

    private int getMaxXFromAngle(float angle) {
        int x = (int) StrictMath.round(StrictMath.cos(((angle + 180) / 180) % 2 * StrictMath.PI) * getSize() + getSize() / 2f);
        return StrictMath.max(StrictMath.min(x, getSize()), 0);
    }

    private int getMinYFromXOnArc(int x, float angle) {
        float dx = x - getSize() / 2f;
        int y;
        if (x > getMaxXFromAngle(angle)) {
            y = (int) (getSize() / 2 + StrictMath.tan(((angle + 180) / 180) % 2 * StrictMath.PI) * dx);
        } else {
            y = (int) StrictMath.round(getSize() / 2f - StrictMath.sqrt(getSize() * getSize() - dx * dx));
        }
        return StrictMath.max(StrictMath.min(y, getSize()), 0);
    }

    private int getMaxYFromXOnArc(int x, float angle) {
        float dx = x - getSize() / 2f;
        int y;
        if (x > getSize() / 2) {
            y = (int) (getSize() / 2 + StrictMath.tan(((angle + 180) / 180) % 2 * StrictMath.PI) * dx);
        } else {
            y = getSize() / 2;
        }
        return StrictMath.max(StrictMath.min(y, getSize()), 0);
    }

    public boolean inTeam(Vector3f pos, boolean reverse) {
        return inTeam(new Vector2f(pos), reverse);
    }

    public boolean inTeam(Vector2f pos, boolean reverse) {
        return inTeam((int) pos.x, (int) pos.y, reverse);
    }

    public boolean inTeam(int x, int y, boolean reverse) {
        return (x >= getMinXBound(SymmetryType.TEAM) && x < getMaxXBound(SymmetryType.TEAM) && y >= getMinYBound(x, SymmetryType.TEAM) && y < getMaxYBound(x, SymmetryType.TEAM)) ^ reverse && inBounds(x, y);
    }

    public boolean inHalf(Vector3f pos, float angle) {
        return inHalf(new Vector2f(pos), angle);
    }

    public boolean inHalf(int x, int y, float angle) {
        return inHalf(new Vector2f(x, y), angle);
    }

    public boolean inHalf(Vector2f pos, float angle) {
        float vectorAngle = (float) ((new Vector2f(getSize() / 2f, getSize() / 2f).getAngle(pos) * 180f / StrictMath.PI) + 90f + 360f) % 360f;
        if (angle >= 180) {
            return (vectorAngle >= angle || vectorAngle < (angle + 180f) % 360f) && inBounds(pos);
        } else {
            return (vectorAngle >= angle && vectorAngle < (angle + 180f) % 360f) && inBounds(pos);
        }
    }

    public void applySymmetry(SymmetryType symmetryType) {
        applySymmetry(symmetryType, false);
    }

    public void applySymmetry(SymmetryType symmetryType, boolean reverse) {
        for (int x = getMinXBound(symmetryType); x < getMaxXBound(symmetryType); x++) {
            for (int y = getMinYBound(x, symmetryType); y < getMaxYBound(x, symmetryType); y++) {
                Vector2f location = new Vector2f(x, y);
                ArrayList<SymmetryPoint> symPoints = getSymmetryPoints(location, symmetryType);
                symPoints.forEach(symmetryPoint -> {
                    if (reverse) {
                        setValueAt(location, getValueAt(symmetryPoint.getLocation()));
                    } else {
                        setValueAt(symmetryPoint.getLocation(), getValueAt(location));
                    }
                });
            }
        }
        if (!symmetrySettings.getSymmetry(symmetryType).isPerfectSymmetry()) {
            interpolate();
        }
    }

    public void applySymmetry(float angle) {
        if (symmetrySettings.getSymmetry(SymmetryType.SPAWN) != Symmetry.POINT2) {
            System.out.println("Spawn Symmetry must equal POINT2");
        }
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                if (inHalf(x, y, angle)) {
                    Vector2f location = new Vector2f(x, y);
                    ArrayList<SymmetryPoint> symPoints = getSymmetryPoints(location, SymmetryType.SPAWN);
                    symPoints.forEach(symmetryPoint -> setValueAt(symmetryPoint.getLocation(), getValueAt(location)));
                }
            }
        }
    }

    public Mask<T> enlarge(int size) {
        return enlarge(size, SymmetryType.SPAWN);
    }

    public Mask<T> enlarge(int size, SymmetryType symmetryType) {
        T[][] largeMask = getEmptyMask(size);
        int smallX;
        int smallY;
        for (int x = 0; x < size; x++) {
            smallX = StrictMath.min(x / (size / getSize()), getSize() - 1);
            for (int y = 0; y < size; y++) {
                smallY = StrictMath.min(y / (size / getSize()), getSize() - 1);
                largeMask[x][y] = getValueAt(smallX, smallY);
            }
        }
        mask = largeMask;
        applySymmetry(symmetryType);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public Mask<T> shrink(int size) {
        return shrink(size, SymmetryType.SPAWN);
    }

    public Mask<T> shrink(int size, SymmetryType symmetryType) {
        T[][] smallMask = getEmptyMask(size);
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
                smallMask[x][y] = getValueAt(largeX, largeY);
            }
        }
        mask = smallMask;
        applySymmetry(symmetryType);
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public Mask<T> flip(SymmetryType symmetryType) {
        Symmetry symmetry = symmetrySettings.getSymmetry(symmetryType);
        if (symmetry.getNumSymPoints() != 2) {
            throw new IllegalArgumentException("Cannot flip non single axis symmetry");
        }
        T[][] newMask = getEmptyMask(getSize());
        for (int x = 0; x < getSize(); x++) {
            for (int y = 0; y < getSize(); y++) {
                ArrayList<SymmetryPoint> symmetryPoints = getSymmetryPoints(x, y, symmetryType);
                newMask[x][y] = getValueAt(symmetryPoints.get(0).getLocation());
            }
        }
        this.mask = newMask;
        VisualDebugger.visualizeMask(this);
        return this;
    }

    public Mask<T> startVisualDebugger(String maskName) {
        return startVisualDebugger(maskName, Util.getStackTraceParentClass());
    }

    public Mask<T> startVisualDebugger(String maskName, String parentClass) {
        VisualDebugger.whitelistMask(this, maskName, parentClass);
        show();
        return this;
    }

    public void show() {
        VisualDebugger.visualizeMask(this);
    }
}
