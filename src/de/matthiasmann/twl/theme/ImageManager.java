/*
 * Copyright (c) 2008-2009, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.twl.theme;

import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.renderer.MouseCursor;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.renderer.Renderer;
import de.matthiasmann.twl.renderer.Texture;
import de.matthiasmann.twl.utils.StateExpression;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 *
 * @author Matthias Mann
 */
class ImageManager {

    private static final Logger logger = Logger.getLogger(ImageManager.class.getName());

    private final Renderer renderer;
    private final TreeMap<String, Image> images;
    private final TreeMap<String, MouseCursor> cursors;
    private final ArrayList<Texture> textureResources;

    static final EmptyImage NONE = new EmptyImage(0, 0);
    
    ImageManager(Renderer renderer) {
        this.renderer = renderer;
        this.images = new TreeMap<String, Image>();
        this.cursors = new TreeMap<String, MouseCursor>();
        this.textureResources = new ArrayList<Texture>();

        images.put("none", NONE);
    }

    void destroy() {
        for(Texture texture : textureResources) {
            texture.destroy();
        }
    }

    Image getImage(String name) {
        return images.get(name);
    }

    Image getReferencedImage(XmlPullParser xpp) throws XmlPullParserException {
        String ref = ParserUtil.getAttributeNotNull(xpp, "ref");
        if(ref.endsWith(".*")) {
            throw new XmlPullParserException("wildcard mapping not allowed", xpp, null);
        }
        return getReferencedImage(xpp, ref);
    }

    Image getReferencedImage(XmlPullParser xpp, String ref) throws XmlPullParserException {
        Image img = images.get(ref);
        if(img == null) {
            throw new XmlPullParserException("referenced image \"" + ref + "\" not found", xpp, null);
        }
        return img;
    }

    MouseCursor getReferencedCursor(XmlPullParser xpp, String ref) throws XmlPullParserException {
        MouseCursor cursor = getCursor(ref);
        if(cursor == null) {
            throw new XmlPullParserException("referenced cursor \"" + ref + "\" not found", xpp, null);
        }
        return cursor;
    }

    Map<String, Image> getImages(String ref, String name) {
        return ParserUtil.resolve(images, ref, name);
    }

    public MouseCursor getCursor(String name) {
        return cursors.get(name);
    }

    Map<String, MouseCursor> getCursors(String ref, String name) {
        return ParserUtil.resolve(cursors, ref, name);
    }

    void parseTextures(XmlPullParser xpp, URL baseUrl) throws XmlPullParserException, IOException {
        xpp.require(XmlPullParser.START_TAG, null, "textures");
        String fmt = xpp.getAttributeValue(null, "format");
        String filter = xpp.getAttributeValue(null, "filter");
        String fileName = xpp.getAttributeValue(null, "file");

        try {
            Texture texture = renderer.loadTexture(new URL(baseUrl, fileName), fmt, filter);
            if(texture == null) {
                throw new NullPointerException("loadTexture returned null");
            }
            textureResources.add(texture);

            try {
                xpp.nextTag();
                while(xpp.getEventType() != XmlPullParser.END_TAG) {
                    String name = ParserUtil.getAttributeNotNull(xpp, "name");
                    ParserUtil.checkNameNotEmpty(name, xpp);
                    if(images.containsKey(name)) {
                        throw new XmlPullParserException("image \"" + name + "\" already defined", xpp, null);
                    }
                    String tagName = xpp.getName();
                    if("cursor".equals(xpp.getName())) {
                        parseCursor(xpp, name, texture);
                    } else {
                        Image image = parseImage(xpp, tagName);
                        images.put(name, image);
                    }
                    xpp.require(XmlPullParser.END_TAG, null, tagName);
                    xpp.nextTag();
                }
            } finally {
                texture.themeLoadingDone();
            }
        } catch (Exception ex) {
            throw (XmlPullParserException)(new XmlPullParserException(
                    "Unable to load texture: " + fileName, xpp, ex).initCause(ex));
        }
    }

    private Border getBorder(Image image, Border border) {
        if(border == null && (image instanceof HasBorder)) {
            border = ((HasBorder)image).getBorder();
        }
        return border;
    }

    private void parseCursor(XmlPullParser xpp, String name, Texture texture) throws IOException, XmlPullParserException {
        String ref = xpp.getAttributeValue(null, "ref");
        MouseCursor cursor;
        if(ref != null) {
            cursor = cursors.get(ref);
        } else {
            ImageParams imageParams = new ImageParams();
            parseRectFromAttribute(xpp, imageParams);
            int hotSpotX = ParserUtil.parseIntFromAttribute(xpp, "hotSpotX");
            int hotSpotY = ParserUtil.parseIntFromAttribute(xpp, "hotSpotY");
            cursor = texture.createCursor(imageParams.x, imageParams.y, imageParams.w, imageParams.h, hotSpotX, hotSpotY);
        }
        if(cursor != null) {
            cursors.put(name, cursor);
        }
        xpp.nextTag();
    }

    private Image parseImage(XmlPullParser xpp, String tagName) throws XmlPullParserException, IOException {
        StateExpression condition = ParserUtil.parseCondition(xpp);
        Image image = parseImageNoCond(xpp, tagName);
        if(condition != null) {
            image = new ConditionImage(image, getBorder(image, null), condition);
        }
        return image;
    }

    private Image parseImageNoCond(XmlPullParser xpp, String tagName) throws XmlPullParserException, IOException {
        ImageParams params = new ImageParams();
        params.tintColor = ParserUtil.parseColorFromAttribute(xpp, "tint", null);
        params.border = ParserUtil.parseBorderFromAttribute(xpp, "border");
        params.inset = ParserUtil.parseBorderFromAttribute(xpp, "inset");
        params.repeatX = ParserUtil.parseBoolFromAttribute(xpp, "repeatX", false);
        params.repeatY = ParserUtil.parseBoolFromAttribute(xpp, "repeatY", false);
        params.sizeOverwriteH = ParserUtil.parseIntFromAttribute(xpp, "sizeOverwriteH", -1);
        params.sizeOverwriteV = ParserUtil.parseIntFromAttribute(xpp, "sizeOverwriteV", -1);
        params.center = ParserUtil.parseBoolFromAttribute(xpp, "center", false);
        
        Image image = parseImageDelegate(xpp, tagName, params);
        image = adjustImage(image, params);
        return image;
    }

    private Image adjustImage(Image image, ImageParams params) {
        Border border = getBorder(image, params.border);
        if(params.tintColor != null && !Color.WHITE.equals(params.tintColor)) {
            image = image.createTintedVersion(params.tintColor);
        }
        if(params.repeatX || params.repeatY) {
            image = new RepeatImage(image, border, params.repeatX, params.repeatY);
        }
        Border imgBorder = getBorder(image, null);
        if((border != null && border != imgBorder) || params.inset != null ||
                params.center || params.sizeOverwriteH >= 0 || params.sizeOverwriteV >= 0) {
            image = new ImageAdjustments(image, border, params.inset,
                    params.sizeOverwriteH, params.sizeOverwriteV, params.center);
        }
        return image;
    }

    private Image parseImageDelegate(XmlPullParser xpp, String tagName, ImageParams params) throws XmlPullParserException, IOException {
        if("texture".equals(tagName)) {
            return parseTexture(xpp, params);
        } else if("hvsplit".equals(tagName)) {
            return parseHVSplit(xpp, params);
        } else if("hsplit".equals(tagName)) {
            return parseHSplit(xpp, params);
        } else if("vsplit".equals(tagName)) {
            return parseVSplit(xpp, params);
        } else if("alias".equals(tagName)) {
            return parseAlias(xpp);
        } else if("composed".equals(tagName)) {
            return parseComposed(xpp, params);
        } else if("select".equals(tagName)) {
            return parseStateSelect(xpp, params);
        } else if("grid".equals(tagName)) {
            return parseGrid(xpp, params);
        } else if("animation".equals(tagName)) {
            return parseAnimation(xpp, params);
        } else {
            throw new XmlPullParserException("Unexpected '"+tagName+"'", xpp, null);
        }
    }

    private Image parseComposed(XmlPullParser xpp, ImageParams params) throws IOException, XmlPullParserException {
        ArrayList<Image> layers = new ArrayList<Image>();
        xpp.nextTag();
        while(xpp.getEventType() != XmlPullParser.END_TAG) {
            xpp.require(XmlPullParser.START_TAG, null, null);
            String tagName = xpp.getName();
            Image image = parseImage(xpp, tagName);
            layers.add(image);
            params.border = getBorder(image, params.border);
            xpp.require(XmlPullParser.END_TAG, null, tagName);
            xpp.nextTag();
        }
        if(layers.size() < 2) {
            throw new XmlPullParserException("composed image needs atleast 2 layers", xpp, null);
        }
        Image image = new ComposedImage(
                layers.toArray(new Image[layers.size()]),
                params.border);
        return image;
    }

    private Image parseStateSelect(XmlPullParser xpp, ImageParams params) throws IOException, XmlPullParserException {
        ArrayList<Image> stateImages = new ArrayList<Image>();
        ArrayList<StateExpression> conditions = new ArrayList<StateExpression>();
        xpp.nextTag();
        while(xpp.getEventType() != XmlPullParser.END_TAG) {
            xpp.require(XmlPullParser.START_TAG, null, null);
            StateExpression cond = ParserUtil.parseCondition(xpp);
            String tagName = xpp.getName();
            Image image = parseImageNoCond(xpp, tagName);
            stateImages.add(image);
            params.border = getBorder(image, params.border);
            xpp.require(XmlPullParser.END_TAG, null, tagName);
            xpp.nextTag();
            if(cond != null) {
                conditions.add(cond);
            } else {
                break;
            }
        }
        if(conditions.size() < 1) {
            throw new XmlPullParserException("state select image needs atleast 1 condition", xpp, null);
        }
        Image image = new StateSelectImage(
                stateImages.toArray(new Image[stateImages.size()]),
                conditions.toArray(new StateExpression[conditions.size()]),
                params.border);
        return image;
    }

    private Image parseTexture(XmlPullParser xpp, ImageParams params) throws IOException, XmlPullParserException {
        parseRectFromAttribute(xpp, params);
        boolean tiled = ParserUtil.parseBoolFromAttribute(xpp, "tiled", false);
        Image image = createImage(xpp, params.x, params.y, params.w, params.h, params.tintColor, tiled);
        params.tintColor = null;
        if(tiled) {
            params.repeatX = false;
            params.repeatY = false;
        }
        xpp.nextTag();
        return image;
    }

    private Image parseAlias(XmlPullParser xpp) throws XmlPullParserException, XmlPullParserException, IOException {
        Image image = getReferencedImage(xpp);
        xpp.nextTag();
        return image;
    }

    private static int[] parseSplit(XmlPullParser xpp, String attribName, int width) throws XmlPullParserException {
        try {
            int[] off = new int[4];
            ParserUtil.parseIntArray(ParserUtil.getAttributeNotNull(xpp, attribName), off, 1, 2);
            off[3] = width;
            return off;
        } catch(NumberFormatException ex) {
            throw (XmlPullParserException)(new XmlPullParserException(
                    "Unable to parse", xpp, ex).initCause(ex));
        }
    }

    private static int[] parseSplit(XmlPullParser xpp, String attribName, int left, int right, int width) {
        int[] off = new int[4];
        String splitStr = xpp.getAttributeValue(null, attribName);
        if(splitStr != null) {
            ParserUtil.parseIntArray(splitStr, off, 1, 2);
        } else {
            off[1] = left;
            off[2] = width - right;
        }
        off[3] = width;
        return off;
    }

    private void parseSubImages(XmlPullParser xpp, Image[] textures) throws XmlPullParserException, IOException {
        for(int i=0 ; i<textures.length ; i++) {
            xpp.require(XmlPullParser.START_TAG, null, null);
            String tagName = xpp.getName();
            textures[i] = parseImage(xpp, tagName);
            xpp.require(XmlPullParser.END_TAG, null, tagName);
            xpp.nextTag();
        }
    }

    private Image parseGrid(XmlPullParser xpp, ImageParams params) throws IOException, XmlPullParserException {
        try {
            int[] weightsX = ParserUtil.parseIntArrayFromAttribute(xpp, "weightsX");
            int[] weightsY = ParserUtil.parseIntArrayFromAttribute(xpp, "weightsY");
            Image[] textures = new Image[weightsX.length * weightsY.length];
            xpp.nextTag();
            parseSubImages(xpp, textures);
            Image image = new GridImage(textures, weightsX, weightsY, params.border);
            return image;
        } catch(IllegalArgumentException ex) {
            ex.printStackTrace();
            throw (XmlPullParserException)(new XmlPullParserException(
                    "Invalid value", xpp, ex).initCause(ex));
        }
    }

    private static final int[] SPLIT_WEIGHTS_3 = {0,1,0};
    private static final int[] SPLIT_WEIGHTS_1 = {1};
    
    private Image parseHSplit(XmlPullParser xpp, ImageParams params) throws IOException, XmlPullParserException {
        Image[] textures = new Image[3];
        if(xpp.getAttributeValue(null, "x") == null) {
            xpp.nextTag();
            parseSubImages(xpp, textures);
        } else {
            parseRectFromAttribute(xpp, params);
            int[] xoff;
            if(params.border != null) {
                xoff = parseSplit(xpp, "splitx", params.border.getBorderLeft(),
                        params.border.getBorderRight(), Math.abs(params.w));
            } else {
                xoff = parseSplit(xpp, "splitx", Math.abs(params.w));
            }
            for(int h=0 ; h<3 ; h++) {
                int imgW = (xoff[h+1] - xoff[h]) * Integer.signum(params.w);
                textures[h] = createImage(xpp,
                        params.x+xoff[h], params.y, imgW, params.h,
                        params.tintColor, false);
            }
            params.tintColor = null;
            xpp.nextTag();
        }
        Image image = new GridImage(textures, SPLIT_WEIGHTS_3, SPLIT_WEIGHTS_1, params.border);
        return image;
    }

    private Image parseVSplit(XmlPullParser xpp, ImageParams params) throws IOException, XmlPullParserException {
        Image[] textures = new Image[3];
        if(xpp.getAttributeValue(null, "x") == null) {
            xpp.nextTag();
            parseSubImages(xpp, textures);
        } else {
            parseRectFromAttribute(xpp, params);
            int[] yoff;
            if(params.border != null) {
                yoff = parseSplit(xpp, "splity", params.border.getBorderTop(),
                        params.border.getBorderBottom(), Math.abs(params.h));
            } else {
                yoff = parseSplit(xpp, "splity", Math.abs(params.h));
            }
            for(int v=0 ; v<3 ; v++) {
                int imgH = (yoff[v+1] - yoff[v]) * Integer.signum(params.h);
                textures[v] = createImage(xpp,
                        params.x, params.y+yoff[v], params.w, imgH,
                        params.tintColor, false);
            }
            params.tintColor = null;
            xpp.nextTag();
        }
        Image image = new GridImage(textures, SPLIT_WEIGHTS_1, SPLIT_WEIGHTS_3, params.border);
        return image;
    }


    private Image parseHVSplit(XmlPullParser xpp, ImageParams params) throws IOException, XmlPullParserException {
        Image[] textures = new Image[9];
        if(xpp.getAttributeValue(null, "x") == null) {
            xpp.nextTag();
            parseSubImages(xpp, textures);
        } else {
            parseRectFromAttribute(xpp, params);
            boolean noCenter = ParserUtil.parseBoolFromAttribute(xpp, "nocenter", false);
            int[] xoff, yoff;
            if(params.border != null) {
                xoff = parseSplit(xpp, "splitx", params.border.getBorderLeft(), params.border.getBorderRight(), Math.abs(params.w));
                yoff = parseSplit(xpp, "splity", params.border.getBorderTop(), params.border.getBorderBottom(), Math.abs(params.h));
            } else {
                xoff = parseSplit(xpp, "splitx", Math.abs(params.w));
                yoff = parseSplit(xpp, "splity", Math.abs(params.h));
            }
            for(int v=0 ; v<3 ; v++) {
                for(int h=0 ; h<3 ; h++) {
                    int imgW = (xoff[h+1] - xoff[h]) * Integer.signum(params.w);
                    int imgH = (yoff[v+1] - yoff[v]) * Integer.signum(params.h);
                    if(noCenter && h == 1 && v == 1) {
                        textures[v*3+h] = new EmptyImage(imgW, imgH);
                    } else {
                        textures[v*3+h] = createImage(xpp,
                                params.x+xoff[h], params.y+yoff[v], imgW, imgH,
                                params.tintColor, false);
                    }
                }
            }
            params.tintColor = null;
            xpp.nextTag();
        }
        Image image = new GridImage(textures, SPLIT_WEIGHTS_3, SPLIT_WEIGHTS_3, params.border);
        return image;
    }

    private AnimatedImage.Element parseAnimElement(XmlPullParser xpp, String tagName) throws XmlPullParserException, IOException {
        if("repeat".equals(tagName)) {
            return parseAnimRepeat(xpp);
        }
        if("frame".equals(tagName)) {
            return parseAnimFrame(xpp);
        }
        throw new XmlPullParserException("Unexpected " + tagName, xpp, null);
    }

    private AnimatedImage.Img parseAnimFrame(XmlPullParser xpp) throws XmlPullParserException, IOException {
        int duration = ParserUtil.parseIntFromAttribute(xpp, "duration");
        if(duration <= 0) {
            throw new IllegalArgumentException("duration must be >= 1 ms");
        }
        Color tint = ParserUtil.parseColorFromAttribute(xpp, "tint", Color.WHITE);
        Image image = getReferencedImage(xpp);
        AnimatedImage.Img img = new AnimatedImage.Img(duration, image, tint);
        xpp.nextTag();
        return img;
    }

    private AnimatedImage.Repeat parseAnimRepeat(XmlPullParser xpp) throws XmlPullParserException, IOException {
        String strRepeatCount = xpp.getAttributeValue(null, "count");
        int repeatCount = 0;
        if(strRepeatCount != null) {
            repeatCount = Integer.parseInt(strRepeatCount);
            if(repeatCount <= 0) {
                throw new IllegalArgumentException("Invalid repeat count");
            }
        }
        boolean lastRepeatsEndless = false;
        boolean hasWarned = false;
        ArrayList<AnimatedImage.Element> children = new ArrayList<AnimatedImage.Element>();
        xpp.nextTag();
        while(xpp.getEventType() == XmlPullParser.START_TAG) {
            if(lastRepeatsEndless && !hasWarned) {
                hasWarned = true;
                logger.warning("Animation frames after an endless repeat won't be displayed: " + xpp.getPositionDescription());
            }
            String tagName = xpp.getName();
            AnimatedImage.Element e = parseAnimElement(xpp, tagName);
            children.add(e);
            lastRepeatsEndless =
                    (e instanceof AnimatedImage.Repeat) &&
                    ((AnimatedImage.Repeat)e).repeatCount == 0;
            xpp.require(XmlPullParser.END_TAG, null, tagName);
            xpp.nextTag();
        }
        return new AnimatedImage.Repeat(children.toArray(new AnimatedImage.Element[children.size()]), repeatCount);
    }

    private Border getBorder(AnimatedImage.Element e) {
        if(e instanceof AnimatedImage.Repeat) {
            AnimatedImage.Repeat r = (AnimatedImage.Repeat)e;
            for(AnimatedImage.Element c : r.children) {
                Border border = getBorder(c);
                if(border != null) {
                    return border;
                }
            }
        } else if(e instanceof AnimatedImage.Img) {
            AnimatedImage.Img i = (AnimatedImage.Img)e;
            if(i.image instanceof HasBorder) {
                return ((HasBorder)i.image).getBorder();
            }
        }
        return null;
    }

    private Image parseAnimation(XmlPullParser xpp, ImageParams params) throws XmlPullParserException, IOException {
        try {
            String timeSource = ParserUtil.getAttributeNotNull(xpp, "timeSource");
            AnimatedImage.Repeat root = parseAnimRepeat(xpp);
            if(params.border == null) {
                params.border = getBorder(root);
            }
            Image image = new AnimatedImage(renderer, root, timeSource, params.border,
                    (params.tintColor == null) ? Color.WHITE : params.tintColor);
            params.tintColor = null;
            return image;
        } catch(IllegalArgumentException ex) {
            throw (XmlPullParserException)(new XmlPullParserException(
                    "Unable to parse", xpp, ex).initCause(ex));
        }
    }

    private Image createImage(XmlPullParser xpp, int x, int y, int w, int h, Color tintColor, boolean tiled) {
        if(w == 0 || h == 0) {
            return new EmptyImage(Math.abs(w), Math.abs(h));
        }
        // adjust position for flip
        if(w < 0) {
            x -= w;
        }
        if(h < 0) {
            y -= h;
        }

        Texture texture = textureResources.get(textureResources.size()-1);
        if(x >= texture.getWidth() || x+Math.abs(w) <= 0 ||
                y >= texture.getHeight() || y+Math.abs(h) <= 0) {
            logger.warning("texture partly outside of file: " + xpp.getPositionDescription());
        }
        return texture.getImage(x, y, w, h, tintColor, tiled);
    }
    
    private void parseRectFromAttribute(XmlPullParser xpp, ImageParams params) throws XmlPullParserException {
        params.x = ParserUtil.parseIntFromAttribute(xpp, "x");
        params.y = ParserUtil.parseIntFromAttribute(xpp, "y");
        params.w = ParserUtil.parseIntFromAttribute(xpp, "width");
        params.h = ParserUtil.parseIntFromAttribute(xpp, "height");
    }

    static class ImageParams {
        int x, y, w, h;
        Color tintColor;
        Border border;
        Border inset;
        boolean repeatX;
        boolean repeatY;
        int sizeOverwriteH = -1;
        int sizeOverwriteV = -1;
        boolean center;
    }
}
