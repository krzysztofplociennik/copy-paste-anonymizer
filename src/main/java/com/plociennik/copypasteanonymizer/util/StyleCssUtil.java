package com.plociennik.copypasteanonymizer.util;

import com.plociennik.copypasteanonymizer.CopyPasteAnonymizerApplication;
import com.plociennik.copypasteanonymizer.common.CopyPasteAnonymizerException;

import java.net.URL;

public class StyleCssUtil {

    private final static String STYLE_NAME_FILE = "style.css";

    public static String getResource() {
        URL resource = CopyPasteAnonymizerApplication.class.getResource(STYLE_NAME_FILE);
        if (resource == null) {
            throw new CopyPasteAnonymizerException("1410_09072026", "The file [%s] is not present in the resource folder.".formatted(STYLE_NAME_FILE));
        }
        return resource.toExternalForm();
    }
}
