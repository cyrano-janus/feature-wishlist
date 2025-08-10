// src/main/java/com/example/featurewishlist/ui/ThemeUtil.java
package com.example.featurewishlist.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import jakarta.servlet.http.Cookie;

public class ThemeUtil {
    private static final String COOKIE_NAME = "ui-theme"; // "dark" | "light"

    public static void applySavedThemeOrSystemDefault() {
        String v = readCookie(COOKIE_NAME);
        if (v == null || v.isBlank()) {
            // Kein Cookie -> System-Preference übernehmen
            UI.getCurrent().getPage().executeJs(
                "const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;" +
                "document.documentElement.toggleAttribute('theme', prefersDark);" +
                "if(prefersDark){document.documentElement.setAttribute('theme','dark');}"
            );
        } else {
            setDark("dark".equalsIgnoreCase(v));
        }
    }

    public static void toggle() {
        boolean dark = !isDark();
        setDark(dark);
        writeCookie(COOKIE_NAME, dark ? "dark" : "light", 365);
    }

    public static boolean isDark() {
        // fragt DOM ab; Rückgabe asynchron -> wir halten eine JS-Seite:
        // in Vaadin-Server-Seite einfacher: lese Cookie
        String v = readCookie(COOKIE_NAME);
        return v != null && v.equalsIgnoreCase("dark");
    }

    public static void setDark(boolean dark) {
        // Lumo: 'theme="dark"' am <html> Element setzen/löschen
        UI.getCurrent().getPage().executeJs(
            "if($0){document.documentElement.setAttribute('theme','dark');}else{document.documentElement.removeAttribute('theme');}",
            dark
        );
    }

    private static void writeCookie(String name, String value, int days) {
        Cookie c = new Cookie(name, value);
        c.setPath("/");
        c.setMaxAge(days * 24 * 60 * 60);
        VaadinService.getCurrentResponse().addCookie(c);
    }

    private static String readCookie(String name) {
        var req = VaadinService.getCurrentRequest();
        if (req == null || req.getCookies() == null) return null;
        for (var c : req.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
