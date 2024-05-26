package su.nightexpress.excellentcrates.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;

public class TextDisplayBuilder {

    private final Location location;
    private String text;
    private boolean seeThrough = false;
    private int lineWidth = 200;
    private Color bgColor = null;
    private Display.Billboard billboard = Display.Billboard.FIXED;
    private TextDisplay.TextAlignment alignment = TextDisplay.TextAlignment.CENTER;
    private double scale = 1;
    private boolean rotateY = false;
    private Vector translation = null;

    public TextDisplayBuilder(Location location) {
        this.location = location;
    }

    public TextDisplayBuilder setText(String text) {
        this.text = text;
        return this;
    }

    public TextDisplayBuilder setSeeThrough(boolean seeThrough) {
        this.seeThrough = seeThrough;
        return this;
    }

    public TextDisplayBuilder setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
        return this;
    }

    public TextDisplayBuilder setBackgroundColor(Color bgColor) {
        this.bgColor = bgColor;
        return this;
    }

    public TextDisplayBuilder setBillboard(Display.Billboard billboard) {
        this.billboard = billboard;
        return this;
    }

    public TextDisplayBuilder setAlignment(TextDisplay.TextAlignment alignment) {
        this.alignment = alignment;
        return this;
    }

    public TextDisplayBuilder setScale(double scale) {
        this.scale = scale;
        return this;
    }

    public TextDisplayBuilder setRotateY(boolean rotateY) {
        this.rotateY = rotateY;
        return this;
    }

    public TextDisplayBuilder setTranslation(Vector translation) {
        this.translation = translation;
        return this;
    }

    public TextDisplay build() {
        Location spawnLoc = location.clone();
        spawnLoc.getChunk().load();
        float yaw = spawnLoc.getYaw();
        if (rotateY) {
            spawnLoc.setYaw(90);
        }

        return spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, e -> {
            e.setText(text);
            e.setSeeThrough(seeThrough);
            e.setDefaultBackground(false);
            e.setLineWidth(lineWidth);
            e.setBillboard(billboard);
            e.setAlignment(alignment);
            if (bgColor != null)
                e.setBackgroundColor(bgColor);
            if (scale != 1 || rotateY || translation != null) {
                Transformation transformation = e.getTransformation();
                if (scale != 1) {
                    transformation.getScale().set(scale);
                }
                if (rotateY) {
                    AxisAngle4f angle = new AxisAngle4f((float) Math.toRadians(-yaw + 270), 0, 1, 0);
                    transformation.getRightRotation().set(angle);
                }
                if (translation != null) {
                    transformation.getTranslation().set(translation.getX(), translation.getY(), translation.getZ());
                }

                e.setInterpolationDelay(0);
                e.setTransformation(transformation);
            }
        });
    }
}
