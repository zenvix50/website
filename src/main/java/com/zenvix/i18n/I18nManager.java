package com.zenvix.i18n;

import javafx.geometry.NodeOrientation;
import javafx.scene.Node;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Ensures strict global translation bindings actively managing JavaFX Nodes naturally
 * evaluating explicit RTL alignments smoothly across dynamic runtime switches securely.
 */
public class I18nManager {

    private static I18nManager instance;
    private Locale currentLocale;
    private ResourceBundle bundle;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
    private final String[] RTL_LANGUAGES = {"ar", "he", "fa", "ur"};

    private I18nManager(Locale defaultLocale) {
        setLocale(defaultLocale);
    }

    public static synchronized I18nManager getInstance() {
        if (instance == null) {
            instance = new I18nManager(new Locale("en", "US"));
        }
        return instance;
    }
    
    protected static synchronized void resetInstance(Locale locale) {
        instance = new I18nManager(locale);
    }

    public void setLocale(Locale locale) {
        Locale oldLocale = this.currentLocale;
        this.currentLocale = locale;
        Locale.setDefault(locale);
        this.bundle = ResourceBundle.getBundle("i18n/messages", locale);
        pcs.firePropertyChange("locale", oldLocale, locale);
    }

    public Locale getCurrentLocale() {
        return currentLocale;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }

    public String format(String key, Object... args) {
        String pattern = get(key);
        MessageFormat formatter = new MessageFormat(pattern, currentLocale);
        return formatter.format(args);
    }

    public String formatNumber(double number) {
        return NumberFormat.getInstance(currentLocale).format(number);
    }

    public DateTimeFormatter getDateFormatter() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", currentLocale);
    }

    public boolean isRTL() {
        String lang = currentLocale.getLanguage();
        return Arrays.asList(RTL_LANGUAGES).contains(lang);
    }

    public void applyRTL(Node rootNode) {
        if (rootNode != null) {
            rootNode.setNodeOrientation(isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        }
    }
}
